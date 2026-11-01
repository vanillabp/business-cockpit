package io.vanillabp.derived.cockpit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A database of release 0.3.4 or 0.4.3 runs none of its changesets again after the upgrade. Only
 * the changesets which are new since that release run.
 * <p>
 * The records are written the way those releases wrote them, by a raw insert and not by the
 * library of today. Three things in them differ from what the library writes now. The
 * {@code _class} names {@code io.vanillabp.cockpit.commons.mongo.changesets.ChangesetInformation},
 * a class which no longer exists. The {@code timestamp} is a BSON date, which the releases wrote
 * from an {@code OffsetDateTime}. And the {@code version} is 1, because those releases saved each
 * record twice. That Spring Data reads such a record anyway was concluded from its code before.
 * This test measures it.
 * <p>
 * Which changeset counts as done is decided by the id and by the order together. The library looks
 * a record up in a map sorted by order. So a record whose order differs from today's changeset is
 * not found, and that changeset runs a second time. The orders below are those of the releases,
 * read from the tags.
 * <p>
 * Only the records are written. The collections and indexes those changesets built are not, so
 * the new changesets run against an empty database. Whether they meet an index of an old release
 * which is in their way is not what this test asks.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AnUpgradeRepeatsNoChangesetTest {

  private static final String COLLECTION = "ChangesetInformation";

  private static final String CLASS_OF_THE_OLD_RELEASES = "io.vanillabp.cockpit.commons.mongo.changesets.ChangesetInformation";

  private static final String TASKS = "io.vanillabp.cockpit.tasklist.model.changesets.V000001#";

  private static final String WORKFLOWS = "io.vanillabp.cockpit.workflowlist.model.changesets.V000001#";

  private static final String MODULES = "io.vanillabp.cockpit.workflowmodules.model.changesets.V000001#";

  /**
   * The record of the module collection. Both releases stored a rollback which drops the
   * workflows, and a changeset of today repairs it, see decision 67 in the repository's
   * DECISIONS.md. So this record is the one an upgrade is expected to change.
   */
  private static final String RECORD_OF_THE_MODULE_COLLECTION = MODULES
      + "createWorkflowModuleCollection";

  private static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:7.0").withReplicaSet();

  private static MongoClient client;

  /**
   * A changeset as one of the old releases recorded it.
   */
  private record OldRecord(
                           String id,
                           int order,
                           String author,
                           List<String> rollbackScripts) {

    private static OldRecord of(
        final String id,
        final int order,
        final String author,
        final String... rollbackScripts) {

      return new OldRecord(id, order, author, List.of(rollbackScripts));

    }

  }

  /**
   * The 23 records of 0.3.4, read from the changeset classes at the tag {@code 0.3.4}. A
   * changeset which returned nothing has an empty list of rollback scripts.
   */
  private static final List<OldRecord> RECORDS_OF_0_3_4 = List.of(
      OldRecord.of(TASKS
          + "createUsertaskCollection", 1, "stephanpelikan",
          "{ dropIndexes: 'usertask', index: '_defaultSort' }",
          "{ dropIndexes: 'usertask', index: '_workflowModuleUri' }",
          "{ drop: 'usertask' }"),
      OldRecord.of(TASKS
          + "createUsertaskEndedAtIndex", 2, "stephanpelikan",
          "{ dropIndexes: 'usertask', index: '_endedAt' }"),
      OldRecord.of(TASKS
          + "changeDefaultUserTaskIndex", 3, "stephanpelikan"),
      OldRecord.of(TASKS
          + "fixDefaultUserTaskIndex", 4, "stephanpelikan"),
      OldRecord.of(TASKS
          + "clearAndIndexReadBy", 5, "stephanpelikan",
          "{ dropIndexes: 'usertask', index: '_readBy' }"),
      OldRecord.of(TASKS
          + "setDanglingFieldAccordingToAssigneeAndCandidates", 6, "stephanpelikan"),
      OldRecord.of(TASKS
          + "renameWorkflowModuleIntoWorkflowModuleId", 7, "stephanpelikan"),
      OldRecord.of(TASKS
          + "moveWorkflowModuleUriIntoSeparateCollection", 8, "stephanpelikan"),
      OldRecord.of(TASKS
          + "dropDefaultSortIndex", 9, "stephanpelikan"),
      OldRecord.of(TASKS
          + "introducePersonAndGroupForUserTasks", 10, "stephanpelikan"),
      OldRecord.of(TASKS
          + "dropSortIndexesDueToNewNaming", 11, "stephanpelikan"),
      OldRecord.of(WORKFLOWS
          + "createWorkflowCollection", 1000, "gwieshammer",
          "{ dropIndexes: 'workflow', index: 'workflow_defaultSort' }",
          "{ dropIndexes: 'workflow', index: 'workflow_workflowModuleUri' }",
          "{ drop: 'workflow' }"),
      OldRecord.of(WORKFLOWS
          + "createWorkflowEndedAtIndex", 1002, "gwieshammer",
          "{ dropIndexes: 'workflow', index: 'workflow_endedAt' }"),
      OldRecord.of(WORKFLOWS
          + "fixDefaultUserTaskIndex", 1003, "stephanpelikan"),
      OldRecord.of(WORKFLOWS
          + "createDetailsFulltextSearchIndex", 1004, "stephanpelikan"),
      OldRecord.of(WORKFLOWS
          + "renameWorkflowModuleIntoWorkflowModuleId", 1005, "stephanpelikan"),
      OldRecord.of(WORKFLOWS
          + "moveWorkflowModuleUriIntoSeparateCollection", 1006, "gwieshammer"),
      OldRecord.of(WORKFLOWS
          + "dropDefaultSortIndex", 1007, "gwieshammer"),
      OldRecord.of(WORKFLOWS
          + "introducePersonAndGroupForWorkflows", 1008, "gwieshammer"),
      OldRecord.of(WORKFLOWS
          + "dropSortIndexesDueToNewNaming", 1009, "stephanpelikan"),
      OldRecord.of(WORKFLOWS
          + "dropRedundantColumnWorkflowId", 1010, "stephanpelikan"),
      OldRecord.of(WORKFLOWS
          + "addBusinessIdIndex", 1011, "martingreilinger",
          "{ dropIndexes: 'workflow', index: '_sort_businessId' }"),
      OldRecord.of(RECORD_OF_THE_MODULE_COLLECTION, 2000, "stephanpelikan",
          "{ drop: 'workflow' }"));

  /**
   * The 27 records of 0.4.3: those of 0.3.4 and four more, read from the tag {@code 0.4.3}.
   */
  private static final List<OldRecord> RECORDS_OF_0_4_3 = Stream
      .concat(RECORDS_OF_0_3_4.stream(), Stream.of(
          OldRecord.of(TASKS
              + "introduceReportedAtAndCandidateUsersSince", 12, "stephanpelikan"),
          OldRecord.of(WORKFLOWS
              + "introduceReportedAt", 1012, "gwieshammer"),
          OldRecord.of("io.vanillabp.cockpit.notification.model.changesets.V000001#"
              + "createNotificationOutboxCollection", 3000, "stephanpelikan",
              "{ drop: 'notification_outbox' }"),
          OldRecord.of("io.vanillabp.cockpit.users.model.changesets.V000001#"
              + "createUsersCollection", 4000, "stephanpelikan",
              "{ drop: 'users' }")))
      .toList();

  /**
   * The changesets of today which neither release had. These are the only ones expected to run.
   */
  private static final List<String> NEW_SINCE_0_4_3 = List.of(
      TASKS
          + "introduceLatestEventAt",
      TASKS
          + "removeAssigneesHavingNoUserId",
      TASKS
          + "startWhatIsKnownFromItsEndAlone",
      "io.vanillabp.cockpit.tasklist.model.changesets.V000002#indexTheVisibilityOfAUserTask",
      WORKFLOWS
          + "introduceLatestEventAt",
      WORKFLOWS
          + "startWhatIsKnownFromItsEndAlone",
      "io.vanillabp.cockpit.workflowlist.model.changesets.V000002#indexTheVisibilityOfAWorkflow",
      "io.vanillabp.cockpit.workflowmodules.model.changesets.V000002#correctTheRollbackOfTheWorkflowModuleCollection");

  enum Release {

    V0_3_4(RECORDS_OF_0_3_4),
    V0_4_3(RECORDS_OF_0_4_3);

    private final List<OldRecord> records;

    Release(
        final List<OldRecord> records) {
      this.records = records;
    }

    String database() {
      return "upgrade-from-"
          + name().toLowerCase();
    }

  }

  @BeforeAll
  static void startTheDatabase() {

    MONGODB.start();
    client = MongoClients.create(MONGODB.getReplicaSetUrl());

  }

  @AfterAll
  static void stopTheDatabase() {

    if (client != null) {
      client.close();
    }
    MONGODB.stop();

  }

  @ParameterizedTest
  @EnumSource(Release.class)
  void anUpgradeRunsOnlyTheChangesetsWhichAreNew(
      final Release release) {

    assertThat(RECORDS_OF_0_3_4).hasSize(23);
    assertThat(RECORDS_OF_0_4_3).hasSize(27);

    final var written = writeTheRecordsOf(release);

    startTheCockpitOn(release.database());

    final var afterTheUpgrade = recordsIn(release.database());

    // every old record is still the document the release wrote. A changeset which ran again
    // would have been saved again, with a timestamp, a version and a class of today.
    for (final var record : release.records) {
      final var expected = new Document(written.get(record.id()));
      if (record.id().equals(RECORD_OF_THE_MODULE_COLLECTION)) {
        expected.put("rollbackScripts", List.of("{ drop: 'modules' }"));
      }
      assertThat(afterTheUpgrade.get(record.id()))
          .as("the record of %s after the upgrade from %s", record.id(), release)
          .isEqualTo(expected);
    }

    final var newRecords = new ArrayList<>(afterTheUpgrade.keySet());
    newRecords.removeAll(written.keySet());
    final var expectedToRun = new ArrayList<>(NEW_SINCE_0_4_3);
    if (release == Release.V0_3_4) {
      RECORDS_OF_0_4_3
          .stream()
          .filter(record -> !RECORDS_OF_0_3_4.contains(record))
          .map(OldRecord::id)
          .forEach(expectedToRun::add);
    }
    assertThat(newRecords)
        .as("the changesets which ran during the upgrade from %s", release)
        .containsExactlyInAnyOrderElementsOf(expectedToRun);
    assertThat(newRecords)
        .allSatisfy(id -> assertThat(afterTheUpgrade.get(id).getString("_class"))
            .isEqualTo(com.phactum.mongodb.changesets.ChangesetInformation.class.getName()));

  }

  /**
   * Writes the records with the driver alone, so that nothing of today's mapping takes part.
   */
  private Map<String, Document> writeTheRecordsOf(
      final Release release) {

    final var collection = client
        .getDatabase(release.database())
        .getCollection(COLLECTION);
    final var written = new HashMap<String, Document>();
    // a second apart, in the order the release applied them
    var appliedAt = 1_700_000_000_000L;
    for (final var record : release.records) {
      final var document = new Document()
          .append("_id", record.id())
          .append("version", 1L)
          .append("author", record.author())
          .append("timestamp", new Date(appliedAt))
          .append("order", record.order())
          .append("rollbackScripts", record.rollbackScripts())
          .append("_class", CLASS_OF_THE_OLD_RELEASES);
      collection.insertOne(document);
      written.put(record.id(), document);
      appliedAt += 1_000L;
    }
    return written;

  }

  private Map<String, Document> recordsIn(
      final String database) {

    final var records = new HashMap<String, Document>();
    client
        .getDatabase(database)
        .getCollection(COLLECTION)
        .find()
        .forEach(document -> records.put(document.getString("_id"), document));
    return records;

  }

  /**
   * Starts the cockpit against the database and stops it again. The changesets run while the
   * cockpit starts, so a start is all the upgrade needs.
   */
  private void startTheCockpitOn(
      final String database) {

    try (var cockpit = new SpringApplicationBuilder(DerivedCockpitApplication.class).run(
        "--server.port=0",
        "--spring.main.banner-mode=off",
        "--spring.mongodb.uri="
            + MONGODB.getReplicaSetUrl(database),
        "--business-cockpit.title-short=Upgraded",
        "--business-cockpit.title-long=An upgraded cockpit",
        "--business-cockpit.application-version=1.2.3",
        "--business-cockpit.application-uri=http://localhost",
        "--business-cockpit.jwt.hmacSHA256-base64="
            + Base64.getEncoder().encodeToString(new byte[32]),
        "--bpms-api.realm-name=BPMS-API",
        "--bpms-api.username=reporting",
        "--bpms-api.password={noop}secret")) {
      assertThat(cockpit.isRunning()).isTrue();
    }

  }

}
