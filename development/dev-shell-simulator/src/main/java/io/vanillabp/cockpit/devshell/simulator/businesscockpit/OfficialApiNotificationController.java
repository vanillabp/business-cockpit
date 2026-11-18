package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.devshell.simulator.usermanagement.SignedInUser;
import io.vanillabp.cockpit.gui.api.v1.NotificationConfiguration;
import io.vanillabp.cockpit.gui.api.v1.NotificationMedium;
import io.vanillabp.cockpit.gui.api.v1.NotificationWorkflow;
import io.vanillabp.cockpit.gui.api.v1.OfficialNotificationConfigApi;
import io.vanillabp.cockpit.gui.api.v1.RecipientMediumConfiguration;

/**
 * Answers the page which configures the notifications, {@code /official-api/gui/api/v1/notifications/**},
 * so a user interface which shows that page works on the simulator too.
 *
 * <p>
 * The simulator sends no notifications, so it offers no medium: the lists of media and of
 * recipient settings are empty, and a recipient setting for any medium gets 404, as the cockpit
 * answers for a medium it does not know. The configuration of a user is kept in memory and is
 * gone when the simulator stops. Every path needs a user signed in, as in the cockpit. Without
 * one the answer is 401.
 * </p>
 */
@RestController
@RequestMapping("/official-api/gui/api/v1")
public class OfficialApiNotificationController implements OfficialNotificationConfigApi {

  private final SignedInUser signedInUser;

  private final TaskService taskService;

  private final WorkflowService workflowService;

  private final Map<String, NotificationConfiguration> configurations = new ConcurrentHashMap<>();

  /**
   * Creates the controller. Spring creates it, nobody else does.
   *
   * @param signedInUser Reads the user from the login cookie
   * @param taskService Keeps the user tasks, which name the workflows
   * @param workflowService Keeps the workflows, to find the case a user task belongs to
   */
  public OfficialApiNotificationController(
      final SignedInUser signedInUser,
      final TaskService taskService,
      final WorkflowService workflowService) {

    this.signedInUser = signedInUser;
    this.taskService = taskService;
    this.workflowService = workflowService;

  }

  @Override
  public ResponseEntity<List<NotificationMedium>> getNotificationMedia() {

    return ifSignedIn(userId -> ResponseEntity.ok(List.of()));

  }

  @Override
  public ResponseEntity<NotificationConfiguration> getNotificationConfiguration() {

    return ifSignedIn(userId -> ResponseEntity.ok(configurations
        .getOrDefault(userId, new NotificationConfiguration())));

  }

  @Override
  public ResponseEntity<Void> saveNotificationConfiguration(
      final NotificationConfiguration notificationConfiguration) {

    return ifSignedIn(userId -> {
      configurations.put(userId, Objects.requireNonNullElseGet(
          notificationConfiguration, NotificationConfiguration::new));
      return ResponseEntity.ok().build();
    });

  }

  @Override
  public ResponseEntity<List<RecipientMediumConfiguration>> getRecipientConfiguration() {

    return ifSignedIn(userId -> ResponseEntity.ok(List.of()));

  }

  @Override
  public ResponseEntity<Void> saveRecipientConfiguration(
      final String mediumType,
      final Map<String, String> requestBody) {

    return ifSignedIn(userId -> ResponseEntity.notFound().build());

  }

  /**
   * The workflows the user has user tasks of, one entry per workflow module and BPMN process of
   * the case, as in the cockpit. A user task of a called process counts for the process of its
   * case. The title is the one of a user task which sits in the process of the case, if there is
   * one. The simulator lists the user tasks of everybody to everybody, so this list holds every
   * workflow it knows a user task of.
   */
  @Override
  public ResponseEntity<List<NotificationWorkflow>> getNotificationWorkflows() {

    return ifSignedIn(userId -> {
      final var workflows = new LinkedHashMap<String, NotificationWorkflow>();
      taskService
          .getAllUserTasks()
          .stream()
          .map(this::withItsCase)
          // a user task of the case's own process first, so that its title names the workflow
          .sorted(Comparator.comparing(TaskOfCase::sitsInTheCaseProcess).reversed())
          .forEach(task -> workflows.putIfAbsent(
              task.workflowModuleId()
                  + "#"
                  + task.bpmnProcessId(),
              new NotificationWorkflow()
                  .workflowModuleId(task.workflowModuleId())
                  .bpmnProcessId(task.bpmnProcessId())
                  .workflowTitle(task.userTask().getWorkflowTitle())));
      return ResponseEntity.ok(List.copyOf(workflows.values()));
    });

  }

  private TaskOfCase withItsCase(
      final UserTask userTask) {

    final var theCase = userTask.getWorkflowId() == null
        ? null
        : workflowService
            .getWorkflow(userTask.getWorkflowId())
            .orElse(null);
    if (theCase == null) {
      return new TaskOfCase(userTask, userTask.getWorkflowModuleId(), userTask.getBpmnProcessId());
    }
    return new TaskOfCase(userTask, theCase.getWorkflowModuleId(), theCase.getBpmnProcessId());

  }

  private <T> ResponseEntity<T> ifSignedIn(
      final Function<String, ResponseEntity<T>> answer) {

    return signedInUser
        .id()
        .map(answer)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

  }

  /**
   * A user task together with the workflow module and the BPMN process of its case.
   *
   * @param userTask The user task
   * @param workflowModuleId The workflow module of the case
   * @param bpmnProcessId The BPMN process of the case
   */
  private record TaskOfCase(
                            UserTask userTask,
                            String workflowModuleId,
                            String bpmnProcessId) {

    boolean sitsInTheCaseProcess() {

      return Objects.equals(bpmnProcessId, userTask.getBpmnProcessId());

    }

  }

}
