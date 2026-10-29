package io.vanillabp.cockpit.gui.api.v1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskService.RetrieveItemsMode;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.changesets.V000002;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Measures which plan MongoDB picks for the counts of the lists, and how long they take. It is
 * no test and runs only on request:
 *
 * <pre>
 * mvn -pl business-cockpit -am test -Dsurefire.failIfNoSpecifiedTests=false \
 *     -Dtest=CountingMeasurement -Dmeasure.counts=true
 * </pre>
 *
 * <p>The queries are the cockpit's own, built by {@link UserTaskService} and
 * {@link WorkflowlistService} and mapped the way the cockpit maps them. The data is generated:
 * {@code measure.tasks} user tasks (30,000 by default), of which {@code measure.open-tasks} are
 * open (4,000), and as many workflows. Each count runs twice: once with the indexes the changesets
 * of the cockpit created before the count endpoint, and once with the indexes of the two
 * changesets {@code V000002} of the user tasks and the workflows on top. The results go to {@code target/counting-measurement.md}.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
@EnabledIfSystemProperty(named = "measure.counts", matches = "true")
class CountingMeasurement {

  private static final int USERS = 300;

  private static final int GROUPS = 30;

  private static final int RUNS = 7;

  private static final Path RESULTS = Path.of("target", "counting-measurement.md");

  private final Random random = new Random(4711);

  @Test
  void measure() throws Exception {

    final var tasks = Integer.getInteger("measure.tasks", 30_000);
    final var openTasks = Integer.getInteger("measure.open-tasks", 4_000);

    write("# Measurement of the counts, "
        + LocalDateTime.now());
    write("");
    write("%d user tasks, %d of them open, %d workflows, %d users, %d groups, median of %d runs."
        .formatted(tasks, openTasks, tasks, USERS, GROUPS, RUNS));

    try (var mongodb = new MongoDBContainer(ContainerImages.MONGODB)) {
      mongodb.start();
      try (var mongoClient = MongoClients.create(mongodb.getConnectionString())) {
        final var mongoTemplate = UpdateStreamFixtures.cockpitLikeTemplate(mongoClient);
        loadUserTasks(mongoTemplate, tasks, openTasks);
        loadWorkflows(mongoTemplate, tasks, openTasks);

        write("");
        write("## Indexes before the count endpoint");
        measureAll(mongoTemplate);

        new V000002().indexTheVisibilityOfAUserTask(mongoTemplate);
        new io.vanillabp.cockpit.workflowlist.model.changesets.V000002()
            .indexTheVisibilityOfAWorkflow(mongoTemplate);
        write("");
        write("## With the indexes of the visibility");
        measureAll(mongoTemplate);
      }
    }
    write("");
    write("=== MEASUREMENT DONE ===");

  }

  private void measureAll(
      final MongoTemplate mongoTemplate) throws IOException {

    final var userTasks = new UserTaskService();
    final var now = OffsetDateTime.now();
    final var user = "user-7";
    final var groups = groupsOf(7);
    final var visibilities = new LinkedHashMap<String, UserTaskVisibility>();
    visibilities.put("everything the user may work on",
        UserTaskVisibility.everythingTheUserMayWorkOn(user, groups));
    visibilities.put("only what is the user's own", new UserTaskVisibility(
        false, false, List.of(user), List.of(user), null, List.of(user), null));
    visibilities.put("what the user's groups may take", new UserTaskVisibility(
        true, true, List.of(user), null, groups, List.of(user), null));

    write("");
    write("| list | mode | count | plan | keys examined | docs examined | median ms |");
    write("|---|---|---|---|---|---|---|");
    // a count without 'initialTimestamp' asks for the tasks which have not ended. With one, a
    // task which ended after it counts as open as well, and the query gets an "or" more
    final var timestamps = new LinkedHashMap<String, OffsetDateTime>();
    timestamps.put("", null);
    timestamps.put(", initialTimestamp", now);
    for (final var timestamp : timestamps.entrySet()) {
      for (final var visibility : visibilities.entrySet()) {
        for (final var mode : RetrieveItemsMode.values()) {
          final var query = new Query(userTasks.buildUserTasksCriteria(
              visibility.getValue(), timestamp.getValue(), mode, null));
          measure(mongoTemplate, UserTask.class, UserTask.COLLECTION_NAME, query,
              visibility.getKey() + timestamp.getKey(), mode.name());
        }
      }
    }

    final var workflows = new WorkflowlistService();
    for (final var mode : WorkflowlistService.RetrieveItemsMode.values()) {
      final var query = new Query(workflows.buildWorkflowlistCriteria(
          new WorkflowVisibility(true, List.of(user), groups), now, mode, null, null));
      measure(mongoTemplate, Workflow.class, Workflow.COLLECTION_NAME, query,
          "workflows the user may see", mode.name());
    }

  }

  private void measure(
      final MongoTemplate mongoTemplate,
      final Class<?> entity,
      final String collection,
      final Query query,
      final String list,
      final String mode) throws IOException {

    final var mapper = new QueryMapper(mongoTemplate.getConverter());
    final var filter = mapper.getMappedObject(
        query.getQueryObject(),
        mongoTemplate.getConverter().getMappingContext().getPersistentEntity(entity));
    final var explained = mongoTemplate.getDb()
        .runCommand(new Document("explain", new Document("count", collection).append("query", filter))
            .append("verbosity", "executionStats"));
    final var stats = (Document) explained.get("executionStats");

    final var millis = new long[RUNS];
    long count = 0;
    for (var run = 0; run < RUNS; ++run) {
      final var started = System.nanoTime();
      count = mongoTemplate.count(query, entity);
      millis[run] = (System.nanoTime() - started) / 1_000_000;
    }
    Arrays.sort(millis);

    write("| %s | %s | %d | %s | %s | %s | %d |".formatted(
        list,
        mode,
        count,
        String.join(" < ", stagesOf((Document) ((Document) explained.get("queryPlanner"))
            .get("winningPlan"), new LinkedHashSet<>())),
        stats.get("totalKeysExamined"),
        stats.get("totalDocsExamined"),
        millis[RUNS / 2]));

  }

  /** The names of the stages of a plan, from the top down, each one once. */
  private static Set<String> stagesOf(
      final Document plan,
      final Set<String> stages) {

    if (plan == null) {
      return stages;
    }
    final var stage = plan.getString("stage");
    final var index = plan.getString("indexName");
    stages.add(index == null ? stage : stage
        + "("
        + index
        + ")");
    if (plan.get("inputStage") instanceof Document input) {
      stagesOf(input, stages);
    }
    if (plan.get("inputStages") instanceof List<?> inputs) {
      inputs.forEach(input -> stagesOf((Document) input, stages));
    }
    // the slot based engine nests the classic plan one level deeper
    if (plan.get("queryPlan") instanceof Document inner) {
      stagesOf(inner, stages);
    }
    return stages;

  }

  private List<String> groupsOf(
      final int user) {

    return List.of("group-"
        + (user % GROUPS),
        "group-"
            + ((user * 7 + 3) % GROUPS));

  }

  private static List<Document> ids(
      final List<String> ids) {

    return ids.stream().map(id -> new Document("_id", id)).toList();

  }

  private void loadUserTasks(
      final MongoTemplate mongoTemplate,
      final int tasks,
      final int openTasks) {

    final var collection = mongoTemplate.getCollection(UserTask.COLLECTION_NAME);
    final var ended = new Date(System.currentTimeMillis() - 86_400_000L);
    final var inAWeek = new Date(System.currentTimeMillis() + 7 * 86_400_000L);
    final var batch = new ArrayList<Document>(5_000);
    for (var number = 0; number < tasks; ++number) {
      final var groups = new ArrayList<String>();
      if (random.nextInt(10) > 0) {
        groups.add("group-"
            + random.nextInt(GROUPS));
      }
      final var assignee = random.nextInt(3) == 0 ? "user-"
          + random.nextInt(USERS) : null;
      final var candidates = random.nextInt(4) == 0
          ? List.of("user-"
              + random.nextInt(USERS))
          : List.<String>of();
      final var document = new Document("_id", UUID.randomUUID().toString())
          .append("candidateGroups", ids(groups))
          .append("candidateUsers", ids(candidates))
          .append("dangling", (assignee == null) && groups.isEmpty() && candidates.isEmpty())
          .append("createdAt", new Date(ended.getTime() - number * 1_000L))
          .append("detailsFulltextSearch", "task "
              + number);
      if (assignee != null) {
        document.append("assignee", new Document("_id", assignee));
      }
      if (random.nextInt(20) == 0) {
        document.append("admittedUsers", ids(List.of("user-"
            + random.nextInt(USERS))));
      }
      if (random.nextInt(50) == 0) {
        document.append("excludedCandidateUsers", ids(List.of("user-"
            + random.nextInt(USERS))));
      }
      if (number >= openTasks) {
        document.append("endedAt", ended);
      } else if (random.nextInt(5) == 0) {
        document.append("followUpDate", inAWeek);
      }
      batch.add(document);
      if (batch.size() == 5_000) {
        collection.insertMany(batch);
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      collection.insertMany(batch);
    }
    // the indexes the changesets of the cockpit created before the count endpoint
    collection.createIndex(new Document("endedAt", 1));
    collection.createIndex(new Document("readBy.userId", 1));

  }

  private void loadWorkflows(
      final MongoTemplate mongoTemplate,
      final int workflows,
      final int active) {

    final var collection = mongoTemplate.getCollection(Workflow.COLLECTION_NAME);
    final var ended = new Date(System.currentTimeMillis() - 86_400_000L);
    final var batch = new ArrayList<Document>(5_000);
    for (var number = 0; number < workflows; ++number) {
      final var groups = random.nextInt(10) > 0
          ? List.of("group-"
              + random.nextInt(GROUPS))
          : List.<String>of();
      final var users = random.nextInt(4) == 0
          ? List.of("user-"
              + random.nextInt(USERS))
          : List.<String>of();
      final var document = new Document("_id", UUID.randomUUID().toString())
          .append("accessibleToGroups", ids(groups))
          .append("accessibleToUsers", ids(users))
          .append("dangling", groups.isEmpty() && users.isEmpty())
          .append("createdAt", new Date(ended.getTime() - number * 1_000L))
          .append("businessId", "case-"
              + number);
      if (number >= active) {
        document.append("endedAt", ended);
      }
      batch.add(document);
      if (batch.size() == 5_000) {
        collection.insertMany(batch);
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      collection.insertMany(batch);
    }
    collection.createIndex(new Document("endedAt", 1));
    collection.createIndex(new Document("businessId", 1).append("createdAt", 1).append("_id", 1));

  }

  private static void write(
      final String line) throws IOException {

    Files.createDirectories(RESULTS.getParent());
    Files.writeString(RESULTS, line + System.lineSeparator(), StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

  }

}
