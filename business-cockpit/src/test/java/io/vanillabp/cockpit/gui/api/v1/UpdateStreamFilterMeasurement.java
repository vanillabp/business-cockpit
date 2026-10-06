package io.vanillabp.cockpit.gui.api.v1;

import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.userTasks;
import static org.mockito.Mockito.mock;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.Person;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.UserTaskStreamAudience;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Measures what variant a) of the update stream filter costs: one query per stream and tick, when
 * something was collected. It is no test and runs only on request:
 *
 * <pre>
 * mvn -pl business-cockpit -am test -Dsurefire.failIfNoSpecifiedTests=false \
 *     -Dtest=UpdateStreamFilterMeasurement -Dmeasure.update-streams=true
 * </pre>
 *
 * <p>The streams, the memory and the queries are the cockpit's own. What is simulated is the rest:
 * the changes are collected as the change stream would hand them over, at a random rate around the
 * one configured, and nothing is written to a browser. The results go to
 * {@code target/update-stream-measurement.md}, one line per run, so a long measurement can be
 * watched while it runs.
 *
 * <p>The numbers can be tuned by system properties: {@code measure.datasets} (number of tasks,
 * comma separated), {@code measure.open-tasks}, {@code measure.rates} (changes per second),
 * {@code measure.intervals} (milliseconds) and {@code measure.seconds-per-run}.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
@EnabledIfSystemProperty(named = "measure.update-streams", matches = "true")
class UpdateStreamFilterMeasurement {

    private static final int USERS = 300;

    private static final int STREAMS = 450;

    private static final int GROUPS = 30;

    /** A task in the cockpit carries a title, details and fulltext; this stands in for them. */
    private static final String DETAILS = "x".repeat(1_500);

    private static final Path RESULTS = Path.of("target", "update-stream-measurement.md");

    private final AtomicInteger findsOfUserTasks = new AtomicInteger();

    private final Random random = new Random(4711);

    @Test
    void measure() throws Exception {

        write("# Measurement of variant a), " + LocalDateTime.now());
        write("");
        measureTheMemory();

        final var datasets = numbers("measure.datasets", "20000,500000");
        final var openTasks = Integer.getInteger("measure.open-tasks", 4_000);
        final var rates = doubles("measure.rates", "0.3,3,5,20,50");
        final var intervals = numbers("measure.intervals", "250,1000");

        final var mongodb = new MongoDBContainer(ContainerImages.MONGODB)
                .withCreateContainerCmdModifier(command -> command
                        .getHostConfig()
                        .withMemory(4L * 1024 * 1024 * 1024));
        mongodb.start();
        try (MongoClient mongoClient = countingClient(mongodb)) {
            final var mongoTemplate = UpdateStreamFixtures.cockpitLikeTemplate(mongoClient);
            final var users = users();
            write("| tasks | open | rate/s | tick ms | run s | ticks | ticks with changes | changes "
                    + "| queries/s | tick p50 ms | tick p95 ms | tick max ms | server CPU % | MongoDB CPU % "
                    + "| b) probe p50 ms | b) probe max ms |");
            write("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|");
            for (final var tasks : datasets) {
                final var openIds = load(mongoTemplate, users, tasks, openTasks);
                for (final var interval : intervals) {
                    for (final var rate : rates) {
                        run(mongodb, mongoClient, mongoTemplate, users, openIds, tasks, openTasks,
                                rate, interval);
                    }
                }
            }
        } finally {
            mongodb.stop();
        }
        write("");
        write("=== MEASUREMENT DONE ===");

    }

    /**
     * The heap 450 streams take for their memory, with 500 and with 2,000 ids each. Ids are UUIDs,
     * 36 characters, which is longer than the ids of most BPMS, so this is the upper end.
     */
    private void measureTheMemory() throws Exception {

        write("| ids per stream | streams | heap MB | bytes per id |");
        write("|---|---|---|---|");
        for (final var idsPerStream : List.of(500, 2_000)) {
            final var streams = UpdateStreamFixtures.updateStreams(
                    mock(MongoTemplate.class), mock(TaskScheduler.class), 1_000_000);
            final var before = usedHeapAfterGc();
            final var users = users();
            for (var number = 0; number < STREAMS; ++number) {
                final var user = users.get(number % USERS);
                // a stream of its own person, so every stream holds its own ids
                final var person = Person.of(user.id() + "-tab-" + number, user.groups().toArray(String[]::new));
                UpdateStreamFixtures.subscribe(streams, person);
                final var ids = new ArrayList<String>(idsPerStream);
                for (var id = 0; id < idsPerStream; ++id) {
                    ids.add(UUID.randomUUID().toString());
                }
                streams.listAnswered(
                        UserTaskStreamAudience.KIND_OF_ENTITY, person,
                        UserTaskVisibility.everythingTheUserMayWorkOn(person), ids);
            }
            final var after = usedHeapAfterGc();
            final var bytes = after - before;
            write("| %d | %d | %.1f | %.0f |".formatted(
                    idsPerStream, STREAMS, bytes / 1024.0 / 1024.0,
                    bytes / (double) (STREAMS * idsPerStream)));
            if (streams.openStreams().size() != STREAMS) {
                throw new IllegalStateException("streams lost");
            }
        }
        write("");

    }

    private static long usedHeapAfterGc() throws InterruptedException {

        final var memory = ManagementFactory.getMemoryMXBean();
        for (var round = 0; round < 5; ++round) {
            System.gc();
            Thread.sleep(200);
        }
        return memory.getHeapMemoryUsage().getUsed();

    }

    private void run(
            final MongoDBContainer mongodb,
            final MongoClient mongoClient,
            final MongoTemplate mongoTemplate,
            final List<Person> users,
            final List<String> openIds,
            final int tasks,
            final int openTasks,
            final double rate,
            final int interval) throws Exception {

        final var streams = UpdateStreamFixtures.updateStreams(
                mongoTemplate, mock(TaskScheduler.class), 2_000);
        final var streamList = new ArrayList<UpdateEmitter>();
        for (var number = 0; number < STREAMS; ++number) {
            final var user = users.get(number % USERS);
            streamList.add(UpdateStreamFixtures.subscribe(streams, user));
        }
        // each person's list shows the first 90 of the open tasks they may see, the way a tab
        // which nobody scrolled holds them
        for (var number = 0; number < USERS; ++number) {
            final var user = users.get(number);
            final var visibility = UserTaskVisibility.everythingTheUserMayWorkOn(user);
            final var shown = new ArrayList<String>();
            for (var index = 0; (index < openIds.size()) && (shown.size() < 90); ++index) {
                if (random.nextInt(GROUPS) < 3) {
                    shown.add(openIds.get(index));
                }
            }
            streams.listAnswered(UserTaskStreamAudience.KIND_OF_ENTITY, user, visibility, shown);
        }
        streams.filterCollectedChanges();
        streamList.forEach(UpdateEmitter::consumeEvents);

        final var seconds = rate < 1 ? 120 : Integer.getInteger("measure.seconds-per-run", 30);
        final var changes = new AtomicLong();
        final var changer = Executors.newSingleThreadScheduledExecutor();
        final var changeRandom = new Random(42);
        final Runnable[] nextChange = new Runnable[1];
        nextChange[0] = () -> {
            final var id = openIds.get(changeRandom.nextInt(openIds.size()));
            streams.collectChange(new GuiEvent(
                    UserTaskStreamAudience.KIND_OF_ENTITY, id, Map.of("id", id)));
            changes.incrementAndGet();
            // a Poisson process: the time to the next change is exponentially distributed
            final var waitMicros = (long) (-Math.log(1 - changeRandom.nextDouble()) / rate * 1_000_000);
            changer.schedule(nextChange[0], waitMicros, TimeUnit.MICROSECONDS);
        };

        final var tickMillis = Collections.synchronizedList(new ArrayList<Double>());
        final var probeMillis = Collections.synchronizedList(new ArrayList<Double>());
        final var ticks = new AtomicInteger();
        final var ticksWithChanges = new AtomicInteger();
        final var ticker = Executors.newSingleThreadScheduledExecutor();
        final var probeCollection = mongoClient
                .getDatabase(UpdateStreamFixtures.DATABASE)
                .getCollection(userTasks());

        final var findsBefore = findsOfUserTasks.get();
        final var mongoCpuBefore = cpuMicrosOf(mongodb);
        final var serverCpuBefore = processCpuNanos();
        final var start = System.nanoTime();
        changer.schedule(nextChange[0], 0, TimeUnit.MILLISECONDS);
        ticker.scheduleWithFixedDelay(() -> {
            final var findsAtStart = findsOfUserTasks.get();
            final var tickStart = System.nanoTime();
            streams.filterCollectedChanges();
            final var tookMillis = (System.nanoTime() - tickStart) / 1_000_000.0;
            ticks.incrementAndGet();
            if (findsOfUserTasks.get() > findsAtStart) {
                ticksWithChanges.incrementAndGet();
                tickMillis.add(tookMillis);
            }
            streamList.forEach(UpdateEmitter::consumeEvents);
        }, interval, interval, TimeUnit.MILLISECONDS);

        Thread.sleep(Duration.ofSeconds(seconds));
        changer.shutdownNow();
        ticker.shutdown();
        ticker.awaitTermination(1, TimeUnit.MINUTES);
        final var wallSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
        final var serverCpu = (processCpuNanos() - serverCpuBefore) / 1_000_000_000.0;
        final var mongoCpu = (cpuMicrosOf(mongodb) - mongoCpuBefore) / 1_000_000.0;
        final var queries = findsOfUserTasks.get() - findsBefore;

        // variant b) is not built. As a probe for its cost, one query per tick reads the changed
        // tasks with the fields the visibility depends on. It runs after the measurement, so it
        // does not disturb it, for 20 ticks' worth of changes
        final var perTick = Math.max(1, (int) Math.round(rate * interval / 1000.0));
        final var visibilityFields = Projections.include(
                "assignee", "candidateUsers", "candidateGroups", "admittedUsers",
                "excludedCandidateUsers", "dangling", "endedAt");
        for (var probe = 0; probe < 20; ++probe) {
            final var ids = new ArrayList<String>();
            for (var index = 0; index < perTick; ++index) {
                ids.add(openIds.get(random.nextInt(openIds.size())));
            }
            final var probeStart = System.nanoTime();
            final var found = probeCollection
                    .find(Filters.in("_id", ids))
                    .projection(visibilityFields)
                    .into(new ArrayList<>());
            probeMillis.add((System.nanoTime() - probeStart) / 1_000_000.0);
            if (found.isEmpty()) {
                throw new IllegalStateException("probe found nothing");
            }
        }

        write("| %d | %d | %s | %d | %.0f | %d | %d (%.0f %%) | %d | %.0f | %.1f | %.1f | %.1f | %.0f | %.0f | %.1f | %.1f |"
                .formatted(
                        tasks, openTasks, rate, interval, wallSeconds, ticks.get(),
                        ticksWithChanges.get(), 100.0 * ticksWithChanges.get() / Math.max(1, ticks.get()),
                        changes.get(), queries / wallSeconds,
                        percentile(tickMillis, 50), percentile(tickMillis, 95), percentile(tickMillis, 100),
                        100.0 * serverCpu / wallSeconds, 100.0 * mongoCpu / wallSeconds,
                        percentile(probeMillis, 50), percentile(probeMillis, 100)));
        streams.closeUpdateStreams();

    }

    /**
     * Writes the tasks: {@code openTasks} of them open, the rest ended, the way a year of tasks
     * piles up in the collection. Each is offered to one or two groups, and a third is assigned to
     * a member of one of them.
     */
    private List<String> load(
            final MongoTemplate mongoTemplate,
            final List<Person> users,
            final int tasks,
            final int openTasks) throws IOException {

        final var started = System.nanoTime();
        mongoTemplate.dropCollection(userTasks());
        final var collection = mongoTemplate.getCollection(userTasks());
        final var openIds = new ArrayList<String>(openTasks);
        final var batch = new ArrayList<Document>(5_000);
        final var ended = new Date(System.currentTimeMillis() - 86_400_000L);
        for (var number = 0; number < tasks; ++number) {
            final var id = UUID.randomUUID().toString();
            final var open = number < openTasks;
            final var groups = new ArrayList<String>();
            groups.add("group-" + random.nextInt(GROUPS));
            if (random.nextBoolean()) {
                groups.add("group-" + random.nextInt(GROUPS));
            }
            final var document = UpdateStreamFixtures.userTask(
                    id,
                    random.nextInt(3) == 0 ? memberOf(users, groups.getFirst()) : null,
                    groups);
            document.append("title", new Document("en", "Task " + number));
            document.append("createdAt", new Date(ended.getTime() - number * 1_000L));
            document.append("details", new Document("text", DETAILS));
            if (!open) {
                document.append("endedAt", ended);
            } else {
                openIds.add(id);
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
        // the indexes the changesets of the cockpit create on this collection, so the query
        // planner has the same choice it has in production
        collection.createIndex(new Document("endedAt", 1));
        collection.createIndex(new Document("dueDate", 1).append("createdAt", 1).append("_id", 1));
        collection.createIndex(new Document("readBy.userId", 1));
        write("<!-- loaded %d tasks in %.0f s -->".formatted(
                tasks, (System.nanoTime() - started) / 1_000_000_000.0));
        return openIds;

    }

    private String memberOf(
            final List<Person> users,
            final String group) {

        final var members = users.stream().filter(user -> user.groups().contains(group)).toList();
        return members.isEmpty() ? null : members.get(random.nextInt(members.size())).id();

    }

    /** 300 people, each in two to four of 30 groups. */
    private List<Person> users() {

        final var groupRandom = new Random(17);
        final var users = new ArrayList<Person>();
        for (var number = 0; number < USERS; ++number) {
            final var groups = new ArrayList<String>();
            final var count = 2 + groupRandom.nextInt(3);
            while (groups.size() < count) {
                final var group = "group-" + groupRandom.nextInt(GROUPS);
                if (!groups.contains(group)) {
                    groups.add(group);
                }
            }
            users.add(new Person("user-" + number, List.copyOf(groups)));
        }
        return users;

    }

    private MongoClient countingClient(
            final MongoDBContainer mongodb) {

        return MongoClients.create(MongoClientSettings
                .builder()
                .applyConnectionString(new ConnectionString(
                        mongodb.getConnectionString() + "/" + UpdateStreamFixtures.DATABASE))
                .applyToConnectionPoolSettings(pool -> pool.maxSize(100))
                .addCommandListener(new CommandListener() {
                    @Override
                    public void commandStarted(
                            final CommandStartedEvent event) {
                        if ("find".equals(event.getCommandName())
                                && userTasks().equals(event.getCommand().getString("find").getValue())) {
                            findsOfUserTasks.incrementAndGet();
                        }
                    }
                })
                .build());

    }

    /** The CPU time MongoDB's container used so far, read from its own cgroup. */
    private static long cpuMicrosOf(
            final MongoDBContainer mongodb) throws IOException, InterruptedException {

        final var stat = mongodb.execInContainer("cat", "/sys/fs/cgroup/cpu.stat").getStdout();
        return stat
                .lines()
                .filter(line -> line.startsWith("usage_usec "))
                .mapToLong(line -> Long.parseLong(line.substring("usage_usec ".length()).trim()))
                .findFirst()
                .orElse(0L);

    }

    private static long processCpuNanos() {

        return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                .getProcessCpuTime();

    }

    private static double percentile(
            final List<Double> values,
            final int percentile) {

        if (values.isEmpty()) {
            return 0;
        }
        final var sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        final var index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));

    }

    private static List<Integer> numbers(
            final String property,
            final String defaults) {

        return java.util.Arrays
                .stream(System.getProperty(property, defaults).split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .toList();

    }

    private static List<Double> doubles(
            final String property,
            final String defaults) {

        return java.util.Arrays
                .stream(System.getProperty(property, defaults).split(","))
                .map(String::trim)
                .map(Double::valueOf)
                .toList();

    }

    private static void write(
            final String line) throws IOException {

        Files.createDirectories(RESULTS.toAbsolutePath().getParent());
        Files.writeString(
                RESULTS, line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

    }

}
