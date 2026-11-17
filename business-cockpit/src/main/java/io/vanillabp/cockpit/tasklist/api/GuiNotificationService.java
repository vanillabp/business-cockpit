package io.vanillabp.cockpit.tasklist.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.InitiatorKind;
import io.vanillabp.cockpit.gui.api.v1.InitiatorOfAChange;
import io.vanillabp.cockpit.gui.api.v1.UserTaskEvent;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import io.vanillabp.cockpit.util.events.NotificationEvent;

/**
 * Turns a changed user task into an event for the update streams of the open browsers.
 */
@Service("tasklistGuiNotificationService")
public class GuiNotificationService {

  @Autowired
  private ApplicationEventPublisher applicationEventPublisher;

  /**
   * Called by Spring.
   */
  public GuiNotificationService() {

  }

  /**
   * Publishes a GUI event for the changed user task. The update streams decide which browsers get
   * it. The event names who made this change where the change event said it. Where it did not,
   * the update streams add who made the latest change, which they read from the task.
   *
   * @param notification The change of the user task
   */
  @EventListener(classes = UserTaskChangedNotification.class)
  public void updateClients(
      final UserTaskChangedNotification notification) {

    final var event = new UserTaskEvent()
        .name("UserTask")
        .id(notification.getUserTaskId())
        .type(UserTaskEvent.TypeEnum.fromValue(notification.getType().name()));
    // nobody changed the task when its follow-up date is reached. The cockpit noticed it, and
    // the initiator stored in the task belongs to an older change
    if (notification.getType() == NotificationEvent.Type.FOLLOWUP) {
      event.initiatorKind(InitiatorKind.COCKPIT);
    } else {
      notification
          .getInitiatorOfThisChange()
          .map(InitiatorOfAChange::fromStored)
          .ifPresent(initiator -> event
              .initiator(initiator.id())
              .initiatorKind(initiator.kind()));
    }
    applicationEventPublisher.publishEvent(
        new GuiEvent(
            notification.getSource().toString(), notification.getUserTaskId(), event));

  }

}
