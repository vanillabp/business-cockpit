package io.vanillabp.cockpit.bpms.api.v1_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the cockpit stores when a workflow module reports that a process instance was suspended or
 * activated again.
 * <p>
 * A suspended user task or case stays in every list and carries the moment of the suspension and
 * the person who suspended it. Only the reports of a suspension and an activation change that: a
 * creation, a change or an end leaves it as it is. The reports overtake each other like all
 * reports, so the cockpit orders them by their timestamps.
 * Every test drives the REST way in of the current API version, and one drives version 1, which
 * knows the two reports about a user task as well.
 */
@ExtendWith(SuppressOutputExtension.class)
class SuspendedAndActivatedTest {

  private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-11-01T08:00:00Z");
  private static final OffsetDateTime SUSPENDED_AT = OffsetDateTime.parse("2026-11-01T09:00:00Z");
  private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-11-01T09:30:00Z");
  private static final OffsetDateTime ACTIVATED_AT = OffsetDateTime.parse("2026-11-01T10:00:00Z");
  private static final OffsetDateTime SUSPENDED_AGAIN_AT = OffsetDateTime.parse("2026-11-01T11:00:00Z");
  private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-11-01T12:00:00Z");
  private static final String OPERATOR = "olivia";
  private static final String SUPERVISOR = "sam";

  /**
   * One cockpit server, with its two lists held in memory instead of in MongoDB. The mappers are the
   * generated ones and the services are the real ones.
   */
  private static final class Cockpit {

    private final Map<String, UserTask> userTasks = new HashMap<>();

    private final Map<String, Workflow> workflows = new HashMap<>();

    private final Logger logger = mock(Logger.class);

    private final UserTaskRepository userTaskRepository = userTaskRepository();

    private final WorkflowRepository workflowRepository = workflowRepository();

    private final BpmsApiController bpmsApi;

    private final io.vanillabp.cockpit.bpms.api.v1.BpmsApiController bpmsApiV1;

    private Cockpit() {
      try {
        final var personAndGroupMapper = mock(PersonAndGroupMapper.class);
        when(personAndGroupMapper.toModelPerson(anyString())).thenAnswer(invocation -> {
          final var person = new Person();
          person.setId(invocation.getArgument(0));
          return person;
        });
        when(personAndGroupMapper.toModelGroup(anyString())).thenAnswer(invocation -> {
          final var group = new Group();
          group.setId(invocation.getArgument(0));
          return group;
        });

        final var userTaskMapper = new UserTaskMapperV1_1Impl();
        set(UserTaskMapper.class, userTaskMapper, "personAndGroupMapper", personAndGroupMapper);
        final var workflowMapper = new WorkflowMapperV1_1Impl();
        set(WorkflowMapper.class, workflowMapper, "personAndGroupMapper", personAndGroupMapper);

        final var userTaskService = new UserTaskService();
        set(UserTaskService.class, userTaskService, "userTasks", userTaskRepository);
        set(UserTaskService.class, userTaskService, "logger", logger);
        set(UserTaskService.class, userTaskService, "mongoTemplate", mock(MongoTemplate.class));

        final var workflowlistService = new WorkflowlistService();
        set(WorkflowlistService.class, workflowlistService, "workflowRepository", workflowRepository);
        set(WorkflowlistService.class, workflowlistService, "logger", logger);
        set(WorkflowlistService.class, workflowlistService, "mongoTemplate", mock(MongoTemplate.class));

        bpmsApi = new BpmsApiController();
        set(BpmsApiController.class, bpmsApi, "userTaskMapper", userTaskMapper);
        set(BpmsApiController.class, bpmsApi, "workflowMapper", workflowMapper);
        set(BpmsApiController.class, bpmsApi, "userTaskService", userTaskService);
        set(BpmsApiController.class, bpmsApi, "workflowlistService", workflowlistService);
        set(BpmsApiController.class, bpmsApi, "workflowModuleService", mock(WorkflowModuleService.class));

        bpmsApiV1 = new io.vanillabp.cockpit.bpms.api.v1.BpmsApiController();
        set(io.vanillabp.cockpit.bpms.api.v1.BpmsApiController.class, bpmsApiV1, "userTaskService",
            userTaskService);
      } catch (Exception e) {
        throw new IllegalStateException("Could not build the cockpit under test", e);
      }
    }

    private UserTaskRepository userTaskRepository() {
      final var repository = mock(UserTaskRepository.class);
      when(repository.findById(anyString()))
          .thenAnswer(invocation -> Optional.ofNullable(userTasks.get(invocation.getArgument(0))));
      when(repository.save(any(UserTask.class))).thenAnswer(invocation -> {
        final UserTask task = invocation.getArgument(0);
        userTasks.put(task.getId(), task);
        return task;
      });
      return repository;
    }

    private WorkflowRepository workflowRepository() {
      final var repository = mock(WorkflowRepository.class);
      when(repository.findById(anyString()))
          .thenAnswer(invocation -> Optional.ofNullable(workflows.get(invocation.getArgument(0))));
      when(repository.save(any(Workflow.class))).thenAnswer(invocation -> {
        final Workflow workflow = invocation.getArgument(0);
        workflows.put(workflow.getId(), workflow);
        return workflow;
      });
      return repository;
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

    private UserTask storedTask() {
      return userTasks.get("task-1");
    }

    private Workflow storedWorkflow() {
      return workflows.get("workflow-1");
    }

  }

  private static Cockpit cockpitWhichWasSent(
      final Consumer<BpmsApiController> reports) {

    final var cockpit = new Cockpit();
    reports.accept(cockpit.bpmsApi);
    return cockpit;

  }

  // --- what a report looks like -------------------------------------------------------------

  private static UserTaskCreatedEvent taskCreated() {

    return new UserTaskCreatedEvent()
        .id("event-created")
        .userTaskId("task-1")
        .timestamp(CREATED_AT)
        .workflowModuleId("taxi-ride")
        .bpmnProcessId("ride")
        .workflowId("workflow-1")
        .taskDefinition("assign-driver")
        .title(Map.of("en", "Assign a driver"))
        .uiUriPath("/ui")
        .uiUriType("WEBPACK_MF_REACT")
        .details(new LinkedHashMap<>(Map.of("customer", "Anna")))
        .detailsFulltextSearch("Anna");

  }

  private static UserTaskUpdatedEvent taskChanged(
      final OffsetDateTime timestamp) {

    return new UserTaskUpdatedEvent()
        .id("event-changed-"
            + timestamp)
        .userTaskId("task-1")
        .timestamp(timestamp)
        .workflowModuleId("taxi-ride")
        .bpmnProcessId("ride")
        .workflowId("workflow-1")
        .taskDefinition("assign-driver")
        .title(Map.of("en", "Assign a driver"))
        .uiUriPath("/ui")
        .uiUriType("WEBPACK_MF_REACT");

  }

  private static UserTaskCompletedEvent taskCompleted() {

    return new UserTaskCompletedEvent()
        .id("event-completed")
        .userTaskId("task-1")
        .timestamp(ENDED_AT)
        .workflowModuleId("taxi-ride")
        .bpmnProcessId("ride")
        .workflowId("workflow-1")
        .taskDefinition("assign-driver")
        .title(Map.of("en", "Assign a driver"))
        .uiUriPath("/ui")
        .uiUriType("WEBPACK_MF_REACT");

  }

  private static UserTaskSuspendedEvent taskSuspended(
      final OffsetDateTime timestamp) {

    return new UserTaskSuspendedEvent()
        .id("event-suspended-"
            + timestamp)
        .userTaskId("task-1")
        .timestamp(timestamp)
        .suspendedBy(OPERATOR);

  }

  private static UserTaskActivatedEvent taskActivated(
      final OffsetDateTime timestamp) {

    return new UserTaskActivatedEvent()
        .id("event-activated-"
            + timestamp)
        .userTaskId("task-1")
        .timestamp(timestamp)
        .suspendedBy(SUPERVISOR);

  }

  private static UserTaskCancelledEvent taskCancelled() {

    return new UserTaskCancelledEvent()
        .id("event-cancelled")
        .userTaskId("task-1")
        .timestamp(ENDED_AT)
        .workflowModuleId("taxi-ride")
        .bpmnProcessId("ride")
        .workflowId("workflow-1")
        .taskDefinition("assign-driver")
        .title(Map.of("en", "Assign a driver"))
        .uiUriPath("/ui")
        .uiUriType("WEBPACK_MF_REACT");

  }

  private static WorkflowCreatedEvent workflowCreated() {

    return new WorkflowCreatedEvent()
        .id("event-created")
        .workflowId("workflow-1")
        .timestamp(CREATED_AT)
        .workflowModuleId("taxi-ride")
        .bpmnProcessId("ride")
        .title(Map.of("en", "A ride"))
        .uiUriPath("/ui")
        .uiUriType("WEBPACK_MF_REACT");

  }

  private static WorkflowUpdatedEvent workflowChanged(
      final OffsetDateTime timestamp) {

    return new WorkflowUpdatedEvent()
        .id("event-changed-"
            + timestamp)
        .workflowId("workflow-1")
        .timestamp(timestamp)
        .workflowModuleId("taxi-ride")
        .bpmnProcessId("ride")
        .title(Map.of("en", "A ride"))
        .uiUriPath("/ui")
        .uiUriType("WEBPACK_MF_REACT");

  }

  private static WorkflowCompletedEvent workflowCompleted() {

    return new WorkflowCompletedEvent()
        .id("event-completed")
        .workflowId("workflow-1")
        .timestamp(ENDED_AT)
        .workflowModuleId("taxi-ride")
        .bpmnProcessId("ride")
        .title(Map.of("en", "A ride"))
        .uiUriPath("/ui")
        .uiUriType("WEBPACK_MF_REACT");

  }

  private static WorkflowSuspendedEvent workflowSuspended(
      final OffsetDateTime timestamp) {

    return new WorkflowSuspendedEvent()
        .id("event-suspended-"
            + timestamp)
        .workflowId("workflow-1")
        .timestamp(timestamp)
        .suspendedBy(OPERATOR);

  }

  private static WorkflowActivatedEvent workflowActivated(
      final OffsetDateTime timestamp) {

    return new WorkflowActivatedEvent()
        .id("event-activated-"
            + timestamp)
        .workflowId("workflow-1")
        .timestamp(timestamp)
        .suspendedBy(SUPERVISOR);

  }

  // --- user tasks ---------------------------------------------------------------------------

  @Test
  void aSuspendedTaskStaysOpenAndCarriesTheMomentAndThePersonOfItsSuspension() {

    final var cockpit = new Cockpit();
    cockpit.bpmsApi.userTaskCreatedEvent(taskCreated());

    final var answer = cockpit.bpmsApi.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));

    assertEquals(HttpStatus.OK, answer.getStatusCode());
    final var stored = cockpit.storedTask();
    assertEquals(SUSPENDED_AT, stored.getSuspendedAt());
    assertEquals(OPERATOR, stored.getSuspendedBy());
    assertNull(stored.getEndedAt(), "a suspended task is still open");
    // a suspension says nothing about the data of the task, so it never makes a change look old
    assertEquals(CREATED_AT, stored.getLatestEventAt());

  }

  @Test
  void anActivationRemovesTheMarkAndNamesWhoActivated() {

    final var cockpit = new Cockpit();
    cockpit.bpmsApi.userTaskCreatedEvent(taskCreated());
    cockpit.bpmsApi.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));

    final var answer = cockpit.bpmsApi.userTaskActivatedEvent("task-1", taskActivated(ACTIVATED_AT));

    assertEquals(HttpStatus.OK, answer.getStatusCode());
    assertNull(cockpit.storedTask().getSuspendedAt());
    assertEquals(SUPERVISOR, cockpit.storedTask().getSuspendedBy());

  }

  @Test
  void aRepeatedSuspensionKeepsTheMomentAndThePersonOfTheFirstOne() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
      // a second node of the application reports the same suspension
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AGAIN_AT).suspendedBy("otto"));
    });

    assertEquals(SUSPENDED_AT, cockpit.storedTask().getSuspendedAt());
    assertEquals(OPERATOR, cockpit.storedTask().getSuspendedBy());
    // the creation, the first suspension and the later one were stored. The repeat was not
    verify(cockpit.userTaskRepository, times(3)).save(any(UserTask.class));

  }

  @Test
  void anOlderSuspensionWhichArrivesAfterANewerActivationChangesNothing() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskActivatedEvent("task-1", taskActivated(ACTIVATED_AT));
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
    });

    assertNull(cockpit.storedTask().getSuspendedAt());
    assertEquals(SUPERVISOR, cockpit.storedTask().getSuspendedBy());
    verify(cockpit.logger).info(
        contains("Keeping user task"),
        eq("task-1"),
        eq("suspension"),
        eq(SUSPENDED_AT),
        eq("a younger suspension or activation"));

  }

  @Test
  void aSuspensionWhichArrivesAfterTheNextSuspensionKeepsTheTaskSuspended() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AGAIN_AT));
      api.userTaskActivatedEvent("task-1", taskActivated(ACTIVATED_AT));
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
    });

    assertEquals(SUSPENDED_AGAIN_AT, cockpit.storedTask().getSuspendedAt());

  }

  @Test
  void aChangeDuringTheSuspensionKeepsTheMark() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
      api.userTaskUpdatedEvent("task-1", taskChanged(CHANGED_AT));
    });

    assertEquals(SUSPENDED_AT, cockpit.storedTask().getSuspendedAt());
    assertEquals(OPERATOR, cockpit.storedTask().getSuspendedBy());
    assertEquals(CHANGED_AT, cockpit.storedTask().getLatestEventAt(), "the change itself is stored");

  }

  @Test
  void aChangeOfTheAggregateDuringTheSuspensionKeepsTheMark() {

    // BusinessCockpitService.aggregateChanged reports the case and its open tasks as changed. The
    // application may call it while the instance is suspended
    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.workflowCreatedEvent(workflowCreated());
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
      api.workflowSuspendedEvent("workflow-1", workflowSuspended(SUSPENDED_AT));
      api.workflowUpdatedEvent("workflow-1", workflowChanged(CHANGED_AT).initiator("anna"));
      api.userTaskUpdatedEvent("task-1", taskChanged(CHANGED_AT).initiator("anna"));
    });

    assertEquals(SUSPENDED_AT, cockpit.storedTask().getSuspendedAt());
    assertEquals(SUSPENDED_AT, cockpit.storedWorkflow().getSuspendedAt());
    assertEquals(OPERATOR, cockpit.storedWorkflow().getSuspendedBy());

    // and the suspension which arrives after such a change still counts
    final var later = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskUpdatedEvent("task-1", taskChanged(CHANGED_AT).initiator("anna"));
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
    });
    assertEquals(SUSPENDED_AT, later.storedTask().getSuspendedAt());

  }

  @Test
  void aCreationWhichArrivesAfterTheSuspensionLeavesTheMarkAlone() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
      // the creation is repeated, for example by a second node of the application
      api.userTaskCreatedEvent(taskCreated());
    });

    assertEquals(SUSPENDED_AT, cockpit.storedTask().getSuspendedAt());

  }

  @Test
  void anEndLeavesTheMarkAlone() {

    // a cancelled process instance may have been suspended. Only the end says that the task is over
    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
      api.userTaskCancelledEvent("task-1", taskCancelled());
    });

    assertNotNull(cockpit.storedTask().getEndedAt());
    assertEquals(SUSPENDED_AT, cockpit.storedTask().getSuspendedAt());

  }

  @Test
  void aSuspensionWhichArrivesAfterTheEndIsStoredAsWell() {

    // the reports overtake each other, so the same story must give the same record
    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskCancelledEvent("task-1", taskCancelled());
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
    });

    assertNotNull(cockpit.storedTask().getEndedAt());
    assertEquals(SUSPENDED_AT, cockpit.storedTask().getSuspendedAt());

  }

  @Test
  void aCompletionKeepsWhoActivated() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.userTaskCreatedEvent(taskCreated());
      api.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));
      api.userTaskActivatedEvent("task-1", taskActivated(ACTIVATED_AT));
      api.userTaskCompletedEvent("task-1", taskCompleted());
    });

    assertNull(cockpit.storedTask().getSuspendedAt());
    assertEquals(SUPERVISOR, cockpit.storedTask().getSuspendedBy());

  }

  @Test
  void aSuspensionOfATaskTheCockpitDoesNotKnowIsAnsweredAndLeftOut() {

    final var cockpit = new Cockpit();

    final var answer = cockpit.bpmsApi.userTaskSuspendedEvent("task-1", taskSuspended(SUSPENDED_AT));

    assertEquals(HttpStatus.OK, answer.getStatusCode(), "repeating the report would not help");
    assertNull(cockpit.storedTask(), "a suspension says too little to create a task");
    verify(cockpit.logger).info(
        contains("the cockpit does not know the task"),
        eq("suspension"),
        eq("task-1"),
        eq(SUSPENDED_AT));

  }

  @Test
  void aSuspensionWhosePathNamesAnotherTaskIsRefused() {

    final var cockpit = new Cockpit();
    cockpit.bpmsApi.userTaskCreatedEvent(taskCreated());

    final var answer = cockpit.bpmsApi.userTaskSuspendedEvent("task-2", taskSuspended(SUSPENDED_AT));

    assertEquals(HttpStatus.BAD_REQUEST, answer.getStatusCode());
    assertNull(cockpit.storedTask().getSuspendedAt());

  }

  @Test
  void versionOneOfTheApiReportsBothAsWell() {

    final var cockpit = new Cockpit();
    cockpit.bpmsApi.userTaskCreatedEvent(taskCreated());

    final var suspended = cockpit.bpmsApiV1.userTaskSuspendedEvent(
        "task-1",
        new io.vanillabp.cockpit.bpms.api.v1.UserTaskSuspendedEvent()
            .id("event-suspended")
            .userTaskId("task-1")
            .timestamp(SUSPENDED_AT));
    assertEquals(HttpStatus.OK, suspended.getStatusCode());
    assertEquals(SUSPENDED_AT, cockpit.storedTask().getSuspendedAt());
    assertNull(cockpit.storedTask().getSuspendedBy(), "version 1 cannot name the person");

    final var activated = cockpit.bpmsApiV1.userTaskActivatedEvent(
        "task-1",
        new io.vanillabp.cockpit.bpms.api.v1.UserTaskActivatedEvent()
            .id("event-activated")
            .userTaskId("task-1")
            .timestamp(ACTIVATED_AT));
    assertEquals(HttpStatus.OK, activated.getStatusCode());
    assertNull(cockpit.storedTask().getSuspendedAt());

  }

  // --- workflows ----------------------------------------------------------------------------

  @Test
  void aSuspendedCaseStaysOpenAndCarriesTheMomentOfItsSuspension() {

    final var cockpit = new Cockpit();
    cockpit.bpmsApi.workflowCreatedEvent(workflowCreated());

    final var answer = cockpit.bpmsApi
        .workflowSuspendedEvent("workflow-1", workflowSuspended(SUSPENDED_AT));

    assertEquals(HttpStatus.OK, answer.getStatusCode());
    final var stored = cockpit.storedWorkflow();
    assertEquals(SUSPENDED_AT, stored.getSuspendedAt());
    assertEquals(OPERATOR, stored.getSuspendedBy());
    assertNull(stored.getEndedAt());
    assertEquals(CREATED_AT, stored.getLatestEventAt());

    cockpit.bpmsApi.workflowActivatedEvent("workflow-1", workflowActivated(ACTIVATED_AT));

    assertNull(cockpit.storedWorkflow().getSuspendedAt());
    assertEquals(SUPERVISOR, cockpit.storedWorkflow().getSuspendedBy());

  }

  @Test
  void anActivationWhichOvertookTheSuspensionOfACaseStillWins() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.workflowCreatedEvent(workflowCreated());
      api.workflowActivatedEvent("workflow-1", workflowActivated(ACTIVATED_AT));
      api.workflowSuspendedEvent("workflow-1", workflowSuspended(SUSPENDED_AT));
    });

    assertNull(cockpit.storedWorkflow().getSuspendedAt());

  }

  @Test
  void aChangeOfTheCaseDuringTheSuspensionKeepsTheMark() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.workflowCreatedEvent(workflowCreated());
      api.workflowSuspendedEvent("workflow-1", workflowSuspended(SUSPENDED_AT));
      api.workflowUpdatedEvent("workflow-1", workflowChanged(CHANGED_AT));
    });

    assertEquals(SUSPENDED_AT, cockpit.storedWorkflow().getSuspendedAt());

  }

  @Test
  void anEndOfTheCaseLeavesTheMarkAlone() {

    final var cockpit = cockpitWhichWasSent(api -> {
      api.workflowCreatedEvent(workflowCreated());
      api.workflowSuspendedEvent("workflow-1", workflowSuspended(SUSPENDED_AT));
      api.workflowActivatedEvent("workflow-1", workflowActivated(ACTIVATED_AT));
      api.workflowCompletedEvent("workflow-1", workflowCompleted());
    });

    assertNotNull(cockpit.storedWorkflow().getEndedAt());
    assertNull(cockpit.storedWorkflow().getSuspendedAt());
    assertEquals(SUPERVISOR, cockpit.storedWorkflow().getSuspendedBy());

  }

  @Test
  void aSuspensionOfACaseTheCockpitDoesNotKnowIsAnsweredAndLeftOut() {

    final var cockpit = new Cockpit();

    final var answer = cockpit.bpmsApi
        .workflowSuspendedEvent("workflow-1", workflowSuspended(SUSPENDED_AT));

    assertEquals(HttpStatus.OK, answer.getStatusCode());
    assertNull(cockpit.storedWorkflow());
    verify(cockpit.logger).info(
        contains("the cockpit does not know the case"),
        eq("suspension"),
        eq("workflow-1"),
        eq(SUSPENDED_AT));

  }

  @Test
  void aSuspensionWhosePathNamesAnotherCaseIsRefused() {

    final var cockpit = new Cockpit();
    cockpit.bpmsApi.workflowCreatedEvent(workflowCreated());

    final var answer = cockpit.bpmsApi
        .workflowActivatedEvent("workflow-2", workflowActivated(ACTIVATED_AT));

    assertEquals(HttpStatus.BAD_REQUEST, answer.getStatusCode());

  }

}
