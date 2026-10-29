package io.vanillabp.cockpit.workflowlist.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
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
   * it.
   *
   * @param notification The change of the workflow
   */
  @EventListener(classes = WorkflowChangedNotification.class)
  public void updateClients(
      final WorkflowChangedNotification notification) {

    applicationEventPublisher.publishEvent(
        new GuiEvent(
            notification.getSource().toString(), notification.getWorkflowId(), new WorkflowEvent()
                .name("Workflow")
                .id(notification.getWorkflowId())
                .type(notification.getType().toString())));

  }

}
