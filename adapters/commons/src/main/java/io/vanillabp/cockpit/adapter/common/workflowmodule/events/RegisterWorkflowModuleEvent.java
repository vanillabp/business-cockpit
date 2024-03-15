package io.vanillabp.cockpit.adapter.common.workflowmodule.events;

import java.time.OffsetDateTime;

public class RegisterWorkflowModuleEvent implements WorkflowModuleEvent {

    private String eventId;

    private OffsetDateTime timestamp;

    private String source;

    private String id;

    private String uri;

    private String taskProviderApiUriPath;

    private String workflowProviderApiUriPath;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTaskProviderApiUriPath() {
        return taskProviderApiUriPath;
    }

    public void setTaskProviderApiUriPath(String taskProviderApiUriPath) {
        this.taskProviderApiUriPath = taskProviderApiUriPath;
    }

    public String getWorkflowProviderApiUriPath() {
        return workflowProviderApiUriPath;
    }

    public void setWorkflowProviderApiUriPath(String workflowProviderApiUriPath) {
        this.workflowProviderApiUriPath = workflowProviderApiUriPath;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setSource(String source) {
        this.source = source;
    }

}
