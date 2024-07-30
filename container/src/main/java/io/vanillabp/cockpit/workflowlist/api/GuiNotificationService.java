package io.vanillabp.cockpit.workflowlist.api;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.WorkflowEvent;
import io.vanillabp.cockpit.workflowlist.WorkflowChangedNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service("workflowlistGuiNotificationService")
public class GuiNotificationService {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @EventListener(classes = WorkflowChangedNotification.class)
    public void updateClients(
            final WorkflowChangedNotification notification) {

        applicationEventPublisher.publishEvent(
                new GuiEvent(
                        notification.getSource(),
                        notification.getTargetGroups(),
                        new WorkflowEvent()
                                .name("Workflow")
                                .id(notification.getWorkflowId())
                                .type(notification.getType().toString())));

    }

}
