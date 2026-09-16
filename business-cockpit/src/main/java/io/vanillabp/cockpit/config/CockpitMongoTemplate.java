package io.vanillabp.cockpit.config;

import com.mongodb.WriteConcern;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoAction;
import org.springframework.data.mongodb.core.MongoActionOperation;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

/**
 * The cockpit's {@link MongoTemplate}. The one thing it adds is an answer to the question which
 * write concern a write really runs with.
 *
 * <p>A write concern can be written in three places: in the connection string, on the
 * {@code MongoClient} and on the template. Which of them a write ends up with is decided by
 * {@code MongoTemplate.prepareWriteConcern}, and the answer is not the one most people expect. The
 * template asks its {@code WriteConcernResolver}, which by default answers with the concern set on
 * the template. Then it raises anything below one acknowledgement to
 * {@link WriteConcern#ACKNOWLEDGED}, as long as write results are checked. Only where all of that
 * ends in nothing does a write fall back to the concern of the connection.
 *
 * <p>So a cockpit which checks its write results, and this one does, never reaches the concern of
 * its connection string. Asking the template is the only way to learn what it promises, and
 * {@code prepareWriteConcern} is protected. That is what this class is for.
 */
public class CockpitMongoTemplate extends MongoTemplate {

    /**
     * Any collection name does, because the resolver of the template decides per write and the
     * default one does not look at the collection. It is only here because {@link MongoAction}
     * insists on a name.
     */
    private static final String ANY_COLLECTION = "write-concern-probe";

    private WriteConcern writeConcernOfThisTemplate;

    public CockpitMongoTemplate(
            final MongoDatabaseFactory mongoDbFactory,
            final MongoConverter converter) {

        super(mongoDbFactory, converter);

    }

    /**
     * Remembers what was set, because {@link MongoTemplate} keeps it to itself and offers no
     * getter. Remembering is not the same as reading it once while the template is built: the
     * MongoDB changeset library sets a stricter concern for the time of a migration and puts the
     * old one back afterwards, so the value changes while the application runs.
     */
    @Override
    public void setWriteConcern(
            final WriteConcern writeConcern) {

        super.setWriteConcern(writeConcern);
        this.writeConcernOfThisTemplate = writeConcern;

    }

    /**
     * What a write of this template promises right now. Where the template leaves the answer to
     * the connection, that is the concern the connection carries.
     */
    public WriteConcern writeConcernApplied() {

        final var applied = prepareWriteConcern(new MongoAction(
                writeConcernOfThisTemplate,
                MongoActionOperation.SAVE,
                ANY_COLLECTION,
                null,
                null,
                null));
        return applied != null ? applied : writeConcernOfTheConnection();

    }

    /**
     * What the connection string or the {@code MongoClient} asks for. A write only gets this where
     * {@link #writeConcernApplied()} has nothing of its own to say.
     */
    public WriteConcern writeConcernOfTheConnection() {

        return getDb().getWriteConcern();

    }

}
