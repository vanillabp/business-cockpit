package io.vanillabp.cockpit.tasklist.api;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.UserTaskEvent;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service("tasklistGuiNotificationService")
public class GuiNotificationService {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @EventListener(classes = UserTaskChangedNotification.class)
    public void updateClients(
            final UserTaskChangedNotification notification) {

        applicationEventPublisher.publishEvent(
                new GuiEvent(
                        notification.getSource(),
                        notification.getTargetGroups(),
                        new UserTaskEvent()
                                .name("UserTask")
                                .id(notification.getUserTaskId())
                                .type(notification.getType().toString())));

    }

}
