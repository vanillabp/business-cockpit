package io.vanillabp.cockpit.workflowmodules.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = WorkflowModule.COLLECTION_NAME)
public class WorkflowModule {

    public static final String COLLECTION_NAME = "modules";

    @Id
    private String id;

    @Version
    private long version;

    private String uri;

    private String taskProviderApiUriPath;

    private String workflowProviderApiUriPath;

    public static WorkflowModule withId(
            final String id) {

        final var result = new WorkflowModule();
        result.setId(id);
        return result;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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

}
