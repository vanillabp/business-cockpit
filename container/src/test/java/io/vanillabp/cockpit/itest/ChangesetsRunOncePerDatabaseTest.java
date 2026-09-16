package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import com.phactum.mongodb.changesets.ChangesetApplier;
import com.phactum.mongodb.changesets.ChangesetInformation;
import com.phactum.mongodb.changesets.ChangesetProperties;
import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;

/**
 * A database which already carries the records of the migration repeats no step.
 *
 * <p>This is what an installation does on every start after the first one, and it is what an
 * installation does after the upgrade which moved the mechanism out of this repository into the
 * published library. The identity of a step is the class name of its changeset bean plus the name
 * of its method. Those beans are the five {@code V000001} classes of the cockpit, and they stayed
 * where they were, so the records an older version wrote still name the steps of this one.
 *
 * <p>The application under test has migrated the database while it started, so the collection is
 * filled. The test then lets a second applier run against it, the way a second start would, and
 * compares the collection before and after.
 *
 * <p>The second applier gets a {@code MongoTemplate} of its own. An applier sets the write concern
 * of the template it is handed, and the template of the application is shared with every other
 * test in this suite.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ChangesetsRunOncePerDatabaseTest extends ItestBase {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private MongoDatabaseFactory mongoDatabaseFactory;

    @Autowired
    private MongoConverter mongoConverter;

    @Autowired
    private ChangesetProperties changesetProperties;

    /**
     * What the database says has been applied, as {@code id, timestamp, order} and in a stable
     * order. A step which ran a second time would be missing from this list, carry another
     * timestamp, or turn up twice.
     */
    private List<String> whatTheDatabaseSaysHasBeenApplied() {

        return mongo
                .find(new Query(), ChangesetInformation.class)
                .stream()
                .map(changeset -> changeset.getId()
                        + " @ " + changeset.getTimestamp()
                        + " # " + changeset.getOrder())
                .sorted()
                .toList();

    }

    /**
     * The identity every changeset bean of the application gives its steps right now, read the way
     * the library reads it.
     */
    private Set<String> whatTheChangesetBeansOfTheApplicationAreCalled() {

        return context
                .getBeansWithAnnotation(DbChangesetConfiguration.class)
                .values()
                .stream()
                .flatMap(bean -> Arrays
                        .stream(bean.getClass().getMethods())
                        .filter(method -> method.getAnnotation(DbChangeset.class) != null)
                        .map(method -> bean.getClass().getName() + "#" + method.getName()))
                .collect(Collectors.toSet());

    }

    @Test
    void aSecondApplierFindsEverythingDoneAndChangesNothing() {

        final var before = whatTheDatabaseSaysHasBeenApplied();
        assertThat(before)
                .describedAs("""
                        What the application wrote into the changeset collection while it started. \
                        An empty list means the migration did not run at all, and then this test \
                        would prove nothing.""")
                .isNotEmpty();

        final var ownTemplate = new MongoTemplate(mongoDatabaseFactory, mongoConverter);
        ownTemplate.setWriteResultChecking(WriteResultChecking.EXCEPTION);
        new ChangesetApplier(context, ownTemplate, changesetProperties).init();

        assertThat(whatTheDatabaseSaysHasBeenApplied())
                .describedAs("""
                        The changeset collection after a second applier ran against it. A step \
                        which ran again would be saved again, with a new timestamp.""")
                .isEqualTo(before);

    }

    @Test
    void theRecordsNameTheChangesetBeansOfTheCockpit() {

        final var applied = mongo
                .find(new Query(), ChangesetInformation.class)
                .stream()
                .map(ChangesetInformation::getId)
                .collect(Collectors.toSet());

        assertThat(applied)
                .describedAs("""
                        The identity of an applied step, as it stands in the database. It is the \
                        class name of the changeset bean plus the name of its method. Moving the \
                        mechanism into a library of its own does not touch it, because the beans \
                        belong to the cockpit. If one of these ids ever changes, every database \
                        which already has the collection runs that step a second time.""")
                .isEqualTo(whatTheChangesetBeansOfTheApplicationAreCalled())
                .allSatisfy(id -> assertThat(id)
                        .startsWith("io.vanillabp.cockpit.")
                        .contains(".model.changesets.V000001#"));

    }

}
