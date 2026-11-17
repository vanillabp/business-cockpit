package io.vanillabp.cockpit.workflowlist.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.InitiatorOfAChange;
import io.vanillabp.cockpit.gui.api.v1.WorkflowEvent;
import io.vanillabp.cockpit.workflowlist.WorkflowChangedNotification;

/**
 * Turns a changed workflow into an event for the update streams of the open browsers.
 */
@Service("workflowlistGuiNotificationService")
public class GuiNotificationService {

  @Autowired
  private ApplicationEventPublisher applicationEventPublisher;

  /**
   * Called by Spring.
   */
  public GuiNotificationService() {

  }

  /**
   * Publishes a GUI event for the changed workflow. The update streams decide which browsers get
   * it. The event names who made this change where the change event said it. Where it did not,
   * the update streams add who made the latest change, which they read from the workflow.
   *
   * @param notification The change of the workflow
   */
  @EventListener(classes = WorkflowChangedNotification.class)
  public void updateClients(
      final WorkflowChangedNotification notification) {

    final var event = new WorkflowEvent()
        .name("Workflow")
        .id(notification.getWorkflowId())
        .type(WorkflowEvent.TypeEnum.fromValue(notification.getType().name()));
    notification
        .getInitiatorOfThisChange()
        .map(InitiatorOfAChange::fromStored)
        .ifPresent(initiator -> event
            .initiator(initiator.id())
            .initiatorKind(initiator.kind()));
    applicationEventPublisher.publishEvent(
        new GuiEvent(
            notification.getSource().toString(), notification.getWorkflowId(), event));

  }

}
