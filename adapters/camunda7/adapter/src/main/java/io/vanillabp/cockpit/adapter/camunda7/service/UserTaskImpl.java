package io.vanillabp.cockpit.adapter.camunda7.service;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.spi.cockpit.usertask.UserTask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class UserTaskImpl implements UserTask {

    private final UserTaskCreatedEvent event;

    public UserTaskImpl(
            final UserTaskCreatedEvent event) {

        this.event = event;

    }

    @Override
    public String getId() {
        return event.getUserTaskId();
    }

    @Override
    public String getInitiator() {
        return event.getInitiator();
    }

    @Override
    public String getComment() {
        return event.getComment();
    }

    @Override
    public String getBpmnProcessId() {
        return event.getBpmnProcessId();
    }

    @Override
    public String getBpmnProcessVersion() {
        return event.getBpmnProcessVersion();
    }

    @Override
    public Map<String, String> getWorkflowTitle() {
        return event.getWorkflowTitle();
    }

    @Override
    public Map<String, String> getTitle() {
        return event.getTitle();
    }

    @Override
    public String getBpmnTaskId() {
        return event.getBpmnTaskId();
    }

    @Override
    public String getTaskDefinition() {
        return event.getTaskDefinition();
    }

    @Override
    public Map<String, String> getTaskDefinitionTitle() {
        return event.getTaskDefinitionTitle();
    }

    @Override
    public String getAssignee() {
        return event.getAssignee();
    }

    @Override
    public List<String> getCandidateUsers() {
        return event.getCandidateUsers();
    }

    @Override
    public List<String> getCandidateGroups() {
        return event.getCandidateGroups();
    }

    @Override
    public OffsetDateTime getDueDate() {
        return event.getDueDate();
    }

    @Override
    public OffsetDateTime getFollowUpDate() {
        return event.getFollowUpDate();
    }

    @Override
    public Map<String, Object> getDetails() {
        return event.getDetails();
    }

    @Override
    public List<String> getI18nLanguages() {
        return event.getI18nLanguages();
    }

}
