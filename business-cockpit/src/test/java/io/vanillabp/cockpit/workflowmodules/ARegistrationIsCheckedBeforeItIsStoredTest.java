package io.vanillabp.cockpit.workflowmodules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.util.ServletRequestPathUtils;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.kafka.KafkaWorkflowModuleController;
import io.vanillabp.cockpit.exceptions.RestfulExceptionHandler;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowmodules.model.GroupHierarchy;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModuleRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the cockpit does with a registration of a workflow module which it cannot route, or whose
 * group hierarchy it cannot use.
 * <p>
 * Such a registration is refused before anything is stored: over REST with {@code 400 Bad Request}
 * and a body which says what is wrong, over Kafka by dropping it with a log line. The routes of the
 * other modules keep working. A module which is stored already and cannot be routed is passed over
 * at startup, so the cockpit still starts.
 * <p>
 * The tests send JSON through Spring MVC to both versions of the BPMS API, so the controllers, the
 * exception handler, the service and the registry of routes are the real ones. Only MongoDB is a
 * map.
 */
@ExtendWith(SuppressOutputExtension.class)
class ARegistrationIsCheckedBeforeItIsStoredTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final String V1 = io.vanillabp.cockpit.bpms.api.v1.BpmsApiController.BPMS_API_URL_PREFIX;

  private static final String V1_1 = io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiController.BPMS_API_URL_PREFIX;

  private final Map<String, WorkflowModule> stored = new HashMap<>();

  private WorkflowModuleRepository workflowModules;

  private MicroserviceProxyRegistry routes;

  private WorkflowModuleService service;

  private MockMvc client;

  @BeforeEach
  void setUp() throws Exception {

    workflowModules = mock(WorkflowModuleRepository.class);
    when(workflowModules.findById(anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
    when(workflowModules.save(any(WorkflowModule.class))).thenAnswer(invocation -> {
      final WorkflowModule module = invocation.getArgument(0);
      stored.put(module.getId(), module);
      return module;
    });
    when(workflowModules.findAll()).thenAnswer(invocation -> new ArrayList<>(stored.values()));

    routes = new MicroserviceProxyRegistry();
    service = new WorkflowModuleService();
    set(WorkflowModuleService.class, service, "workflowModules", workflowModules);
    set(WorkflowModuleService.class, service, "microserviceProxyRegistry", routes);

    final var v1 = new io.vanillabp.cockpit.bpms.api.v1.BpmsApiController();
    set(io.vanillabp.cockpit.bpms.api.v1.BpmsApiController.class, v1, "workflowModuleService", service);
    final var v1_1 = new io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiController();
    set(io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiController.class, v1_1, "workflowModuleService", service);

    client = MockMvcBuilders
        .standaloneSetup(v1, v1_1)
        .setControllerAdvice(new RestfulExceptionHandler())
        .build();

  }

  @AfterEach
  void forgetTheHierarchies() {

    // the cache of hierarchies is shared by every test of this JVM
    stored.keySet().forEach(id -> GroupHierarchyService.putGroupHierarchy(id, Map.of()));
    List.of("m", "g", "twice", "nameless").forEach(id -> GroupHierarchyService.putGroupHierarchy(id, Map.of()));

  }

  private static void set(
      final Class<?> declaringClass,
      final Object target,
      final String field,
      final Object value) throws Exception {

    final var declaredField = declaringClass.getDeclaredField(field);
    declaredField.setAccessible(true);
    declaredField.set(target, value);

  }

  private static Map<String, Object> aRegistration(
      final String id,
      final String uri) {

    final var registration = new LinkedHashMap<String, Object>();
    registration.put("id", id);
    if (uri != null) {
      registration.put("uri", uri);
    }
    registration.put("taskProviderApiUriPath", "/api/v1/task-provider");
    return registration;

  }

  private MvcResult register(
      final String apiPrefix,
      final String id,
      final Map<String, Object> registration) throws Exception {

    return client
        .perform(post(apiPrefix
            + "/workflow-module/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(registration)))
        .andReturn();

  }

  /** Whether the cockpit passes a request to this path on to a workflow module. */
  private boolean isRouted(
      final String path) {

    final var request = new MockHttpServletRequest("GET", path);
    ServletRequestPathUtils.parseAndCache(request);
    return routes
        .route(ServerRequest.create(request, List.of()))
        .isPresent();

  }

  private void assertThatAnotherModuleIsStillRouted() throws Exception {

    final var answer = register(V1_1, "other", aRegistration("other", "http://other:8080/other"));

    assertEquals(200, answer.getResponse().getStatus(), answer.getResponse().getContentAsString());
    assertTrue(isRouted("/wm/other/index.html"), "the routes still change after a refused registration");

  }

  @Test
  void aUriWhichCannotBeParsedIsRefusedAndNothingIsStored() throws Exception {

    for (final var api : List.of(V1, V1_1)) {
      final var answer = register(api, "m", aRegistration("m", "http://my host:8080"));

      assertEquals(400, answer.getResponse().getStatus(), api);
      assertTrue(answer.getResponse().getContentAsString().contains("'uri'"),
          answer.getResponse().getContentAsString());
    }
    assertEquals(Map.of(), stored, "a refused registration stores nothing");
    assertFalse(isRouted("/wm/m/index.html"));
    assertThatAnotherModuleIsStillRouted();

  }

  @Test
  void aRegistrationWithoutUriIsRefused() throws Exception {

    for (final var api : List.of(V1, V1_1)) {
      final var answer = register(api, "m", aRegistration("m", null));

      assertEquals(400, answer.getResponse().getStatus(), api);
      assertTrue(
          answer.getResponse().getContentAsString().contains("'uri' is missing"),
          answer.getResponse().getContentAsString());
    }
    assertEquals(Map.of(), stored);
    assertThatAnotherModuleIsStillRouted();

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "relative/path", "ftp://host/module", "http:///no-host"
  })
  void aUriWhichIsNoAbsoluteHttpAddressIsRefused(
      final String uri) throws Exception {

    final var answer = register(V1_1, "m", aRegistration("m", uri));

    assertEquals(400, answer.getResponse().getStatus(), uri);
    assertEquals(Map.of(), stored);

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{x", "{x}", "*", "with space", "a:b"
  })
  void anIdWhichIsNoPlainPathSegmentIsRefused(
      final String id) throws Exception {

    for (final var api : List.of(V1, V1_1)) {
      final var answer = register(api, id, aRegistration(id, "http://module:8080"));

      assertEquals(400, answer.getResponse().getStatus(), api);
      assertTrue(answer.getResponse().getContentAsString().contains("'id'"), answer.getResponse().getContentAsString());
    }
    assertEquals(Map.of(), stored);
    assertThatAnotherModuleIsStillRouted();

  }

  @Test
  void aUsualRegistrationIsStoredAndRouted() throws Exception {

    final var answer = register(V1_1, "loan-approval_2.x~1",
        aRegistration("loan-approval_2.x~1", "https://loans:8443/base"));

    assertEquals(200, answer.getResponse().getStatus(), answer.getResponse().getContentAsString());
    assertTrue(stored.containsKey("loan-approval_2.x~1"));
    assertTrue(isRouted("/wm/loan-approval_2.x~1/remoteEntry.js"));

  }

  @Test
  void aTargetOfTheGroupHierarchyWhichIsNullIsDropped() throws Exception {

    final var registration = aRegistration("g", "http://module:8080");
    registration.put("groupHierarchy", List.of(Map.of(
        "group", "LEADS",
        "targets", Arrays.asList(null, "MEMBERS"))));

    final var answer = register(V1_1, "g", registration);

    assertEquals(200, answer.getResponse().getStatus(), answer.getResponse().getContentAsString());
    assertEquals(
        List.of(new GroupHierarchy("LEADS", List.of("MEMBERS"))),
        stored.get("g").getGroupHierarchy());
    final var groups = assertDoesNotThrow(() -> new GroupHierarchyService().resolveGroups(List.of("LEADS")));
    assertTrue(groups.contains("MEMBERS"), groups.toString());

  }

  @Test
  void aGroupHierarchyEntryWithoutTargetsCountsAsNoTargets() throws Exception {

    final var registration = aRegistration("g", "http://module:8080");
    final var entry = new HashMap<String, Object>();
    entry.put("group", "LEADS");
    registration.put("groupHierarchy", List.of(entry));

    final var answer = register(V1_1, "g", registration);

    assertEquals(200, answer.getResponse().getStatus(), answer.getResponse().getContentAsString());
    assertDoesNotThrow(() -> new GroupHierarchyService().resolveGroups(List.of("LEADS")));

  }

  @Test
  void aGroupWhichIsNamedTwiceInTheHierarchyIsRefused() throws Exception {

    final var registration = aRegistration("twice", "http://module:8080");
    registration.put("groupHierarchy", List.of(
        Map.of("group", "LEADS", "targets", List.of("MEMBERS")),
        Map.of("group", "LEADS", "targets", List.of("GUESTS"))));

    final var answer = register(V1_1, "twice", registration);

    assertEquals(400, answer.getResponse().getStatus());
    assertTrue(
        answer.getResponse().getContentAsString().contains("'groupHierarchy'"),
        answer.getResponse().getContentAsString());
    assertEquals(Map.of(), stored);

  }

  @Test
  void aHierarchyEntryWithoutGroupIsRefused() throws Exception {

    final var registration = aRegistration("nameless", "http://module:8080");
    registration.put("groupHierarchy", List.of(Map.of("targets", List.of("MEMBERS"))));

    final var answer = register(V1_1, "nameless", registration);

    assertEquals(400, answer.getResponse().getStatus());
    assertEquals(Map.of(), stored);

  }

  @Test
  void aKafkaRegistrationWhichCannotBeRoutedIsDropped() throws Exception {

    final var kafka = new KafkaWorkflowModuleController(service);
    for (final var registration : List.of(
        RegisterWorkflowModuleEvent.newBuilder().setWorkflowModuleId("{x").setUri("http://module:8080"),
        RegisterWorkflowModuleEvent.newBuilder().setWorkflowModuleId("m").setUri("http://my host:8080"),
        RegisterWorkflowModuleEvent.newBuilder().setWorkflowModuleId("m"))) {
      final var event = BcEvent.newBuilder().setRegisterWorkflowModule(registration).build();

      assertDoesNotThrow(() -> kafka.consumeWorkflowModuleEvent(
          new ConsumerRecord<>("workflow-module", 0, 0, "key", event.toByteArray())));
    }

    assertEquals(Map.of(), stored);
    assertThatAnotherModuleIsStillRouted();

  }

  @Test
  void aStoredModuleWhichCannotBeRoutedIsPassedOverAtStartup() {

    final var spaceInTheHost = WorkflowModule.withId("broken");
    spaceInTheHost.setUri("http://my host:8080");
    stored.put(spaceInTheHost.getId(), spaceInTheHost);
    final var noUri = WorkflowModule.withId("without-uri");
    stored.put(noUri.getId(), noUri);
    final var patternInTheId = WorkflowModule.withId("{x");
    patternInTheId.setUri("http://module:8080");
    stored.put(patternInTheId.getId(), patternInTheId);
    final var nullTarget = WorkflowModule.withId("null-target");
    nullTarget.setUri("http://module:8080");
    nullTarget.setGroupHierarchy(List.of(new GroupHierarchy("LEADS", Arrays.asList(null, "MEMBERS"))));
    stored.put(nullTarget.getId(), nullTarget);
    final var fine = WorkflowModule.withId("fine");
    fine.setUri("http://fine:8080");
    stored.put(fine.getId(), fine);

    assertDoesNotThrow(() -> service.registerProxiesForWorkflowModules());

    assertTrue(isRouted("/wm/fine/index.html"));
    assertTrue(isRouted("/wm/null-target/index.html"));
    assertFalse(isRouted("/wm/broken/index.html"));
    final var groups = assertDoesNotThrow(() -> new GroupHierarchyService().resolveGroups(List.of("LEADS")));
    assertTrue(groups.contains("MEMBERS"), groups.toString());

  }

}
