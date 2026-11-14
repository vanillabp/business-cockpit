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
import com.mongodb.client.model.IndexOptions;

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
 * The collections and indexes of the release are written as well, also by the driver alone. They
 * are what stood after the last changeset of the release, read from the tags, together with the
 * sort index each list built on demand when it was shown for the first time. So a new changeset
 * meets every index of the old release. One which has the same name but other keys or options
 * stops the start, and the test with it. At the end the test compares the indexes of each
 * collection with the old ones plus those of the new changesets.
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
      "io.vanillabp.cockpit.workflowmodules.model.changesets.V000002#correctTheRollbackOfTheWorkflowModuleCollection",
      "io.vanillabp.cockpit.tasklist.model.changesets.V000003#giveEveryEndedTaskItsReason",
      "io.vanillabp.cockpit.workflowlist.model.changesets.V000003#giveEveryEndedCaseItsReason",
      "io.vanillabp.cockpit.workflowlist.model.changesets.V000003#indexTheNote",
      "io.vanillabp.cockpit.tasklist.model.changesets.V000004#rollBackTheUsertaskCollectionByDroppingItOnly",
      "io.vanillabp.cockpit.workflowlist.model.changesets.V000004#rollBackTheWorkflowCollectionByDroppingItOnly",
      "io.vanillabp.cockpit.tasklist.model.changesets.V000005#indexTheWorkflowModuleOfAUserTask",
      "io.vanillabp.cockpit.workflowlist.model.changesets.V000005#indexTheWorkflowModuleOfAWorkflow");

  /**
   * An index as MongoDB lists it: its name, its keys in their order, and whether it is unique.
   */
  private record OldIndex(
                          String collection,
                          String name,
                          Document keys,
                          boolean unique) {

    private static OldIndex of(
        final String collection,
        final String name,
        final String... fields) {

      return new OldIndex(collection, name, ascending(fields), false);

    }

    private static OldIndex unique(
        final String collection,
        final String name,
        final String... fields) {

      return new OldIndex(collection, name, ascending(fields), true);

    }

    private static Document ascending(
        final String... fields) {

      final var keys = new Document();
      for (final var field : fields) {
        keys.append(field, 1);
      }
      return keys;

    }

  }

  /**
   * The collections and indexes after the last changeset of 0.3.4, read from the changeset
   * classes at the tag. Indexes which a later changeset of the release dropped are left out.
   * {@code _id_} is not listed, MongoDB creates it with each collection.
   */
  private static final List<String> COLLECTIONS_OF_0_3_4 = List.of(
      "usertask", "workflow", "modules");

  private static final List<OldIndex> INDEXES_OF_0_3_4 = List.of(
      OldIndex.of("usertask", "_endedAt", "endedAt"),
      OldIndex.of("usertask", "_readBy", "readBy.userId"),
      OldIndex.of("workflow", "workflow_endedAt", "endedAt"),
      OldIndex.of("workflow", "workflow_fulltext", "detailsFulltextSearch"),
      OldIndex.of("workflow", "_sort_businessId", "businessId", "createdAt", "_id"),
      // no changeset built these two. The release built them when a list was shown with its
      // default sort for the first time, so every installation which was used has them
      OldIndex.of("usertask", "_sort_dueDate", "dueDate", "createdAt", "_id"),
      OldIndex.of("workflow", "_sort_createdAt", "createdAt", "_id"));

  /**
   * 0.4.3 has the collections and indexes of 0.3.4, and two collections more.
   */
  private static final List<String> COLLECTIONS_OF_0_4_3 = Stream
      .concat(COLLECTIONS_OF_0_3_4.stream(), Stream.of("notification_outbox", "users"))
      .toList();

  private static final List<OldIndex> INDEXES_OF_0_4_3 = Stream
      .concat(INDEXES_OF_0_3_4.stream(), Stream.of(
          OldIndex.unique("notification_outbox", "_outboxUnique",
              "userTaskId", "notificationType", "recipientUserId", "medium"),
          OldIndex.of("notification_outbox", "_outboxPending", "sentAt", "createdAt")))
      .toList();

  /**
   * The indexes the new changesets build. Written out here and not read from the changesets, so
   * a changeset which builds something else is noticed too.
   */
  private static final List<OldIndex> INDEXES_OF_THE_NEW_CHANGESETS = List.of(
      OldIndex.of("usertask", "_visibility_assignee", "assignee._id", "endedAt"),
      OldIndex.of("usertask", "_visibility_candidateUsers", "candidateUsers._id", "endedAt"),
      OldIndex.of("usertask", "_visibility_candidateGroups", "candidateGroups._id", "endedAt"),
      OldIndex.of("usertask", "_visibility_admittedUsers", "admittedUsers._id", "endedAt"),
      OldIndex.of("usertask", "_visibility_dangling", "dangling", "endedAt"),
      OldIndex.of("workflow", "_visibility_accessibleToUsers", "accessibleToUsers._id", "endedAt"),
      OldIndex.of("workflow", "_visibility_accessibleToGroups", "accessibleToGroups._id", "endedAt"),
      OldIndex.of("workflow", "_visibility_dangling", "dangling", "endedAt"),
      OldIndex.of("workflow", "workflow_note", "note"),
      OldIndex.of("usertask", "_workflowModuleIdAndEndedAt", "workflowModuleId", "endedAt"),
      OldIndex.of("workflow", "_workflowModuleIdAndEndedAt", "workflowModuleId", "endedAt"));

  enum Release {

    V0_3_4(RECORDS_OF_0_3_4, COLLECTIONS_OF_0_3_4, INDEXES_OF_0_3_4),
    V0_4_3(RECORDS_OF_0_4_3, COLLECTIONS_OF_0_4_3, INDEXES_OF_0_4_3);

    private final List<OldRecord> records;

    private final List<String> collections;

    private final List<OldIndex> indexes;

    Release(
        final List<OldRecord> records,
        final List<String> collections,
        final List<OldIndex> indexes) {
      this.records = records;
      this.collections = collections;
      this.indexes = indexes;
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
    buildTheCollectionsAndIndexesOf(release);

    startTheCockpitOn(release.database());

    theIndexesAreTheOldOnesAndTheNewOnes(release);

    final var afterTheUpgrade = recordsIn(release.database());

    // every old record is still the document the release wrote. A changeset which ran again
    // would have been saved again, with a timestamp, a version and a class of today.
    for (final var record : release.records) {
      final var expected = new Document(written.get(record.id()));
      if (record.id().equals(RECORD_OF_THE_MODULE_COLLECTION)) {
        expected.put("rollbackScripts", List.of("{ drop: 'modules' }"));
      }
      // Two changesets of today shorten the stored rollback of the task and the case collection
      // to a plain drop, because the indexes it named are gone by then (decision 67 again). They
      // only touch a record which still holds the three commands the first release stored.
      final var droppedOnly = Map.of(
          TASKS
              + "createUsertaskCollection",
          "usertask",
          WORKFLOWS
              + "createWorkflowCollection",
          "workflow");
      if (droppedOnly.containsKey(record.id()) && ((List<?>) expected.get("rollbackScripts")).size() == 3) {
        expected.put("rollbackScripts", List.of("{ drop: '"
            + droppedOnly.get(record.id())
            + "' }"));
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

  /**
   * Builds the collections and indexes with the driver alone, as the release left them.
   */
  private void buildTheCollectionsAndIndexesOf(
      final Release release) {

    final var database = client.getDatabase(release.database());
    release.collections.forEach(database::createCollection);
    for (final var index : release.indexes) {
      database
          .getCollection(index.collection())
          .createIndex(index.keys(), new IndexOptions()
              .name(index.name())
              .unique(index.unique()));
    }

  }

  /**
   * Every collection of the new changesets has the indexes of the old release and those the new
   * changesets build, and no other. The upgrade from 0.3.4 also builds the two collections of
   * 0.4.3, so both releases end with the same indexes.
   */
  private void theIndexesAreTheOldOnesAndTheNewOnes(
      final Release release) {

    final var expected = new ArrayList<OldIndex>();
    expected.addAll(INDEXES_OF_0_4_3);
    expected.addAll(INDEXES_OF_THE_NEW_CHANGESETS);
    final var collections = COLLECTIONS_OF_0_4_3;

    final var database = client.getDatabase(release.database());
    assertThat(database.listCollectionNames().into(new ArrayList<>()))
        .as("the collections after the upgrade from %s", release)
        .containsAll(collections);

    for (final var collection : collections) {
      final var actual = new ArrayList<OldIndex>();
      for (final var index : database.getCollection(collection).listIndexes()) {
        if ("_id_".equals(index.getString("name"))) {
          continue;
        }
        actual.add(new OldIndex(
            collection, index.getString("name"),
            // MongoDB may list a key as 1 or as 1.0, so each key is read as a number
            ascending(index.get("key", Document.class)), index.getBoolean("unique", false)));
      }
      assertThat(actual)
          .as("the indexes of '%s' after the upgrade from %s", collection, release)
          .containsExactlyInAnyOrderElementsOf(expected
              .stream()
              .filter(index -> index.collection().equals(collection))
              .toList());
    }

  }

  private static Document ascending(
      final Document keys) {

    final var normalized = new Document();
    keys.forEach((
        field,
        direction) -> normalized.append(field, ((Number) direction).intValue()));
    return normalized;

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
