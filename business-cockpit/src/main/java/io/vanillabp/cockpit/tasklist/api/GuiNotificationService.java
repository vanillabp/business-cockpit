package io.vanillabp.cockpit.tasklist.api;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.UserTaskEvent;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

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
     * it.
     *
     * @param notification The change of the user task
     */
    @EventListener(classes = UserTaskChangedNotification.class)
    public void updateClients(
            final UserTaskChangedNotification notification) {

        applicationEventPublisher.publishEvent(
                new GuiEvent(
                        notification.getSource().toString(),
                        notification.getUserTaskId(),
                        new UserTaskEvent()
                                .name("UserTask")
                                .id(notification.getUserTaskId())
                                .type(notification.getType().toString())));

    }

}
