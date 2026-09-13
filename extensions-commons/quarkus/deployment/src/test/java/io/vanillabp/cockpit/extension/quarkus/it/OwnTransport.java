package io.vanillabp.cockpit.extension.quarkus.it;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.transport.BusinessCockpitTransport;
import jakarta.inject.Singleton;

/**
 * The way to the cockpit a workflow module brings itself, which version 1 of the Business Cockpit
 * allowed and version 2 allows again. It keeps what it was asked to send so that a test can say
 * the report took this way and no other.
 * <p>
 * A bean of the type is all it takes. ArC lets it win over the transport the extension ships,
 * which is declared as its default bean, and the Spring Boot half of this test does the same with
 * a bean of its own.
 */
@Singleton
public class OwnTransport implements BusinessCockpitTransport {

  private static final Duration WAIT_FOR_THE_OUTBOX = Duration.ofSeconds(30);

  private final Queue<UserTaskEvent> userTasks = new ConcurrentLinkedQueue<>();

  private final Queue<WorkflowEvent> workflows = new ConcurrentLinkedQueue<>();

  private final Queue<RegisterWorkflowModuleEvent> workflowModules = new ConcurrentLinkedQueue<>();

  @Override
  public String describe() {

    return "the transport of the application itself";

  }

  @Override
  public void publishUserTaskEvent(
      final UserTaskEvent event) {

    userTasks.add(event);

  }

  @Override
  public void publishWorkflowEvent(
      final WorkflowEvent event) {

    workflows.add(event);

  }

  @Override
  public void registerWorkflowModule(
      final RegisterWorkflowModuleEvent event) {

    workflowModules.add(event);

  }

  /**
   * @param taskDefinition The user task to wait for
   * @return The report about it, once the outbox dispatched the entry
   */
  public UserTaskEvent awaitUserTask(
      final String taskDefinition) {

    return await(
        () -> userTasks
            .stream()
            .filter(event -> taskDefinition.equals(event.getTaskDefinition()))
            .findFirst()
            .orElse(null),
        "a report about user task '%s'".formatted(taskDefinition));

  }

  /**
   * @param workflowModuleId The module to wait for
   * @return Its registration, once the outbox dispatched the entry
   */
  public RegisterWorkflowModuleEvent awaitWorkflowModule(
      final String workflowModuleId) {

    return await(
        () -> workflowModules
            .stream()
            .filter(event -> workflowModuleId.equals(event.workflowModuleId()))
            .findFirst()
            .orElse(null),
        "the registration of workflow module '%s'".formatted(workflowModuleId));

  }

  private static <T> T await(
      final Supplier<T> whatArrived,
      final String whatWasExpected) {

    final var deadline = System.currentTimeMillis() + WAIT_FOR_THE_OUTBOX.toMillis();
    while (System.currentTimeMillis() < deadline) {
      final var arrived = whatArrived.get();
      if (arrived != null) {
        return arrived;
      }
      try {
        Thread.sleep(50);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
    throw new AssertionError(
        "%s never reached the transport of the application".formatted(whatWasExpected));

  }

}
