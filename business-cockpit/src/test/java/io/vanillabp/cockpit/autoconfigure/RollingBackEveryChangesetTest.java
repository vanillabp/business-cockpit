package io.vanillabp.cockpit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.phactum.mongodb.changesets.ChangesetApplier;
import com.phactum.mongodb.changesets.ChangesetAutoConfiguration;
import com.phactum.mongodb.changesets.ChangesetInformation;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A developer who starts the cockpit with {@code initializer.rollback.all} expects an empty
 * database afterwards. Every changeset of the cockpit runs against a real MongoDB first, through
 * the changeset library, the way the cockpit starts. Then everything is rolled back.
 *
 * <p>The library ends the process after {@code initializer.rollback.all}, and a test cannot let
 * that happen. So the rollback is asked for with {@code initializer.rollback.unknown} by an
 * application which knows no changeset at all. To such an application every record is unknown,
 * and the library rolls the records back with the same code and in the same order, the highest
 * order first.
 *
 * <p>The library catches a rollback command which fails, logs it and goes on with the next
 * changeset. The record of the failed changeset stays, and so does what its remaining commands
 * would have dropped. That is why the test looks at the records and the collections which are
 * left, and not only at whether the start went through.
 *
 * <p>Without the {@code V000004} changesets of the user tasks and the workflows, this left both
 * collections behind. The first changeset of each collection stored three commands, and the first
 * of them drops an index which a later changeset had dropped already. See decision 67 in the
 * repository's DECISIONS.md.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class RollingBackEveryChangesetTest {

  private static final String ROLLBACK_UNKNOWN = "initializer.rollback.unknown";

  private static final org.testcontainers.mongodb.MongoDBContainer MONGODB = new org.testcontainers.mongodb.MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private MongoTemplate mongo;

  private ListAppender<ILoggingEvent> logOfTheLibrary;

  @BeforeAll
  static void startMongoDb() {

    MONGODB.start();
    mongoClient = MongoClients.create(MONGODB.getConnectionString());

  }

  @AfterAll
  static void stopMongoDb() {

    if (mongoClient != null) {
      mongoClient.close();
    }
    MONGODB.stop();

  }

  @BeforeEach
  void aDatabaseOfItsOwn() {

    mongo = new MongoTemplate(mongoClient, "cockpit-"
        + UUID.randomUUID());

    logOfTheLibrary = new ListAppender<>();
    logOfTheLibrary.start();
    ((Logger) LoggerFactory.getLogger(ChangesetApplier.class)).addAppender(logOfTheLibrary);

  }

  @AfterEach
  void stopRecordingTheLog() {

    ((Logger) LoggerFactory.getLogger(ChangesetApplier.class)).detachAppender(logOfTheLibrary);
    System.clearProperty(ROLLBACK_UNKNOWN);

  }

  @Test
  void rollingBackEveryChangesetLeavesAnEmptyDatabase() {

    startTheCockpitWith(changesetsOfTheCockpit());
    assertThat(mongo.findAll(ChangesetInformation.class)).isNotEmpty();

    System.setProperty(ROLLBACK_UNKNOWN, Boolean.TRUE.toString());
    startTheCockpitWith();

    final var softly = new SoftAssertions();
    softly.assertThat(failedRollbacks())
        .as("rollbacks the library logged as failed")
        .isEmpty();
    softly.assertThat(mongo.findAll(ChangesetInformation.class))
        .as("records of changesets which were not rolled back")
        .extracting(ChangesetInformation::getId)
        .isEmpty();
    softly.assertThat(mongo.getCollectionNames())
        .as("collections left behind")
        .filteredOn(name -> !name.equals(ChangesetInformation.COLLECTION_NAME))
        .isEmpty();
    softly.assertAll();

  }

  /**
   * The correcting changesets name the record they rewrite as text. It has to be the id the library
   * gives the first changeset of each collection.
   */
  @Test
  void theCorrectingChangesetsNameTheRecordsTheLibraryWrites() {

    assertThat(io.vanillabp.cockpit.tasklist.model.changesets.V000004.RECORD_OF_THE_USERTASK_COLLECTION)
        .isEqualTo(io.vanillabp.cockpit.tasklist.model.changesets.V000001.class.getName()
            + "#createUsertaskCollection");
    assertThat(io.vanillabp.cockpit.workflowlist.model.changesets.V000004.RECORD_OF_THE_WORKFLOW_COLLECTION)
        .isEqualTo(io.vanillabp.cockpit.workflowlist.model.changesets.V000001.class.getName()
            + "#createWorkflowCollection");

  }

  /**
   * The correcting changesets rewrite a record only where it holds exactly what the first
   * changeset of its collection stored. Somebody who changed such a record changed it on purpose.
   */
  @Test
  void aRollbackSomebodyChangedOnPurposeIsLeftAlone() {

    final var changedOnPurpose = List.of("{ drop: 'somethingElse' }");
    storeTheRecord(io.vanillabp.cockpit.tasklist.model.changesets.V000004.RECORD_OF_THE_USERTASK_COLLECTION,
        changedOnPurpose);
    storeTheRecord(io.vanillabp.cockpit.workflowlist.model.changesets.V000004.RECORD_OF_THE_WORKFLOW_COLLECTION,
        changedOnPurpose);

    new io.vanillabp.cockpit.tasklist.model.changesets.V000004().rollBackTheUsertaskCollectionByDroppingItOnly(mongo);
    new io.vanillabp.cockpit.workflowlist.model.changesets.V000004()
        .rollBackTheWorkflowCollectionByDroppingItOnly(mongo);

    assertThat(mongo.findAll(ChangesetInformation.class))
        .extracting(ChangesetInformation::getRollbackScripts)
        .containsExactly(changedOnPurpose, changedOnPurpose);

  }

  /** Stores the record of a changeset the way the library stores it after the changeset ran. */
  private void storeTheRecord(
      final String record,
      final List<String> rollbackScripts) {

    final var changeset = new ChangesetInformation();
    changeset.setId(record);
    changeset.setRollbackScripts(rollbackScripts);
    mongo.save(changeset);

  }

  /**
   * The changeset classes the cockpit itself imports, so a changeset added there is part of this
   * test without anybody listing it here.
   */
  private static Class<?>[] changesetsOfTheCockpit() {

    return Arrays
        .stream(BusinessCockpitPersistenceAutoConfiguration.class.getAnnotation(Import.class).value())
        .filter(imported -> imported.isAnnotationPresent(DbChangesetConfiguration.class))
        .toArray(Class<?>[]::new);

  }

  private void startTheCockpitWith(
      final Class<?>... changesets) {

    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ChangesetAutoConfiguration.class))
        .withBean(MongoTemplate.class, () -> mongo)
        .withBean(PersonAndGroupMapper.class, () -> Mockito.mock(PersonAndGroupMapper.class))
        .withUserConfiguration(changesets)
        .run(context -> assertThat(context).hasNotFailed());

  }

  /** What the library logs when a rollback command throws, with the message of the exception. */
  private List<String> failedRollbacks() {

    return logOfTheLibrary.list
        .stream()
        .filter(event -> event.getFormattedMessage().endsWith("failed!"))
        .map(event -> event.getFormattedMessage()
            + " "
            + (event.getThrowableProxy() == null
                ? ""
                : event.getThrowableProxy().getMessage()))
        .toList();

  }

}
