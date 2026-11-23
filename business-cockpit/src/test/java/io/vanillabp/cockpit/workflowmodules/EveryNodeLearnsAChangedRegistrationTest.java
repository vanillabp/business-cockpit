package io.vanillabp.cockpit.workflowmodules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.mongo.MongoDbProperties;
import io.vanillabp.cockpit.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowmodules.model.GroupHierarchy;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModuleRepository;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A cockpit with several nodes keeps the group hierarchies and the routes to the workflow modules
 * in the memory of each node. A registration reaches one node only, the one which took the REST
 * call or the Kafka record. Every other node learns what was stored: a new module, a new URI and a
 * changed hierarchy.
 * <p>
 * Each node is an application context of its own with the beans this needs: the service, the
 * registry of routes, the repository and the change stream. All nodes share one MongoDB, like
 * the nodes of a real cockpit do. The cache of group hierarchies is static and so shared by every
 * node of this JVM. The hierarchy is therefore written straight into the collection, where no
 * service sees it: only the change stream can bring it into the cache.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class EveryNodeLearnsAChangedRegistrationTest {

  /** Generous, because a change stream is polled and the poll is answered at most once a second. */
  private static final Duration ARRIVAL = Duration.ofSeconds(30);

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB).withReplicaSet();

  private static MongoClient mongoClient;

  private final List<AnnotationConfigApplicationContext> nodes = new ArrayList<>();

  private final List<String> moduleIds = new ArrayList<>();

  private String database;

  @BeforeAll
  static void startMongoDb() {

    MONGODB.start();
    mongoClient = MongoClients.create(MONGODB.getReplicaSetUrl());

  }

  @AfterAll
  static void stopMongoDb() {

    if (mongoClient != null) {
      mongoClient.close();
    }
    MONGODB.stop();

  }

  @BeforeEach
  void anEmptyDatabase() {

    database = "cockpit-"
        + UUID.randomUUID();
    // a change stream is opened on a collection. In the running cockpit a changeset creates it
    template().createCollection(WorkflowModule.COLLECTION_NAME);

  }

  @AfterEach
  void stopTheNodes() {

    nodes.forEach(AnnotationConfigApplicationContext::close);
    moduleIds.forEach(id -> GroupHierarchyService.putGroupHierarchy(id, Map.of()));

  }

  private MongoTemplate template() {

    return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, database));

  }

  /** Starts one node of the cockpit, which is up once this returns. */
  private AnnotationConfigApplicationContext aNode() {

    final var template = template();
    final var node = new AnnotationConfigApplicationContext();
    node.registerBean(MongoTemplate.class, () -> template);
    node.registerBean(MongoDbProperties.class, MongoDbProperties::new);
    node.registerBean(MessageListenerContainer.class, () -> {
      final var executor = new SimpleAsyncTaskExecutor("change-stream-test-");
      executor.setDaemon(true);
      return new DefaultMessageListenerContainer(template, executor);
    });
    node.registerBean(ChangeStreamUtils.class, ChangeStreamUtils::new);
    node.registerBean(WorkflowModuleRepository.class, () -> new MongoRepositoryFactory(template)
        .getRepository(WorkflowModuleRepository.class));
    node.registerBean(MicroserviceProxyRegistry.class, MicroserviceProxyRegistry::new);
    node.registerBean(WorkflowModuleService.class);
    node.refresh();
    final var listenerContainer = node.getBean(MessageListenerContainer.class);
    if (!listenerContainer.isRunning()) {
      listenerContainer.start();
    }
    node.publishEvent(new ApplicationStartedEvent(new SpringApplication(), new String[0], node, Duration.ZERO));
    nodes.add(node);
    return node;

  }

  private String aModuleId() {

    final var id = "module-"
        + moduleIds.size();
    moduleIds.add(id);
    return id;

  }

  private static void register(
      final AnnotationConfigApplicationContext node,
      final String id,
      final String uri) {

    node
        .getBean(WorkflowModuleService.class)
        .registerOrUpdateWorkflowModule(id, uri, "/tasks", "/workflows", List.of(), null);

  }

  /**
   * The URI the node passes requests for the module on to. The registry offers no getter for it,
   * so the test reads the field the routes are built from.
   */
  @SuppressWarnings("unchecked")
  private static String uriOfTheRoute(
      final AnnotationConfigApplicationContext node,
      final String id) {

    final var routes = (Map<String, String>) ReflectionTestUtils.getField(
        node.getBean(MicroserviceProxyRegistry.class), "routes");
    synchronized (node.getBean(MicroserviceProxyRegistry.class)) {
      return routes.get(id);
    }

  }

  private static boolean isRouted(
      final AnnotationConfigApplicationContext node,
      final String id) {

    final var request = new MockHttpServletRequest("GET", "/wm/"
        + id
        + "/index.html");
    ServletRequestPathUtils.parseAndCache(request);
    return node
        .getBean(MicroserviceProxyRegistry.class)
        .route(ServerRequest.create(request, List.of()))
        .isPresent();

  }

  private static Map<String, Collection<String>> hierarchyOf(
      final String id) {

    return new GroupHierarchyService()
        .getGroupHierarchies()
        .get(id);

  }

  @Test
  void aModuleRegisteredOnOneNodeIsRoutedOnTheOther() {

    final var first = aNode();
    final var second = aNode();
    final var id = aModuleId();

    register(first, id, "http://module:8080/module");

    await()
        .atMost(ARRIVAL)
        .untilAsserted(() -> assertThat(isRouted(second, id)).isTrue());
    assertThat(uriOfTheRoute(second, id)).isEqualTo("http://module:8080/module");

  }

  @Test
  void aMovedModuleIsReachedAtItsNewUriOnEveryNode() {

    final var first = aNode();
    final var second = aNode();
    final var id = aModuleId();
    register(first, id, "http://old-host:8080");
    await()
        .atMost(ARRIVAL)
        .untilAsserted(() -> assertThat(uriOfTheRoute(second, id)).isEqualTo("http://old-host:8080"));

    register(first, id, "http://new-host:8080");

    await()
        .atMost(ARRIVAL)
        .untilAsserted(() -> assertThat(uriOfTheRoute(second, id)).isEqualTo("http://new-host:8080"));

  }

  @Test
  void aModuleRegisteredBeforeANodeStartsIsKnownToThatNode() {

    final var first = aNode();
    final var id = aModuleId();
    register(first, id, "http://module:8080");

    final var second = aNode();

    assertThat(isRouted(second, id)).isTrue();

  }

  @Test
  void aHierarchyWhichIsTakenBackIsTakenBackOnEveryNode() {

    aNode();
    final var id = aModuleId();
    final var module = WorkflowModule.withId(id);
    module.setUri("http://module:8080");
    module.setGroupHierarchy(List.of(new GroupHierarchy("LEADS", List.of("MEMBERS"))));

    template().save(module);

    await()
        .atMost(ARRIVAL)
        .untilAsserted(() -> assertThat(hierarchyOf(id)).isEqualTo(Map.of("LEADS", List.of("MEMBERS"))));

    // a partial update, whose change event brings no document unless the stream asks for it
    template().updateFirst(
        Query.query(Criteria.where("_id").is(id)),
        new Update().set("groupHierarchy", List.of()),
        WorkflowModule.class);

    await()
        .atMost(ARRIVAL)
        .untilAsserted(() -> assertThat(hierarchyOf(id)).isEqualTo(Map.of()));

  }

  @Test
  void aStoredModuleWhichCannotBeRoutedDoesNotStopTheOthers() {

    final var node = aNode();
    final var broken = WorkflowModule.withId(aModuleId());
    broken.setUri("http://my host:8080");
    template().save(broken);
    final var id = aModuleId();
    final var fine = WorkflowModule.withId(id);
    fine.setUri("http://module:8080");

    template().save(fine);

    await()
        .atMost(ARRIVAL)
        .untilAsserted(() -> assertThat(isRouted(node, id)).isTrue());
    assertThat(isRouted(node, broken.getId())).isFalse();

  }

}
