package io.vanillabp.cockpit.adapter.common.usertask;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import io.vanillabp.springboot.utils.WorkflowAndModule;

public class UserTasksProperties extends WorkflowAndModule implements Comparable<UserTasksProperties> {
    
    private Map<String, UserTaskProperties> userTasks = new HashMap<>();

    private String templatesPath;
    
    private String uiUriType;

    private String workflowModuleUri;
    
    private String taskProviderApiPath;
    
    private String uiUriPath;
    
    private String bpmnDescriptionLanguage;
    
    private Map<String, DetailsProperties> detailsProperties;
    
    @Override
    public boolean equals(
            final Object obj) {
        if (!(obj instanceof UserTasksProperties)) {
            return false;
        }
        return compareTo((UserTasksProperties) obj) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = getWorkflowModuleId() == null
                ? 0
                : getWorkflowModuleId().hashCode();
        result = 31 * result + getBpmnProcessId() == null
                ? 0
                : getBpmnProcessId().hashCode();
        return result;
    }
    
    @Override
    public int compareTo(
            final UserTasksProperties other) {

        if (!StringUtils.hasText(getWorkflowModuleId())) {
            if (StringUtils.hasText(other.getWorkflowModuleId())) {
                return -1;
            }
        } else if (!StringUtils.hasText(other.getWorkflowModuleId())) {
            return 1;
        } else {
            final var result = getWorkflowModuleId()
                    .compareTo(other.getWorkflowModuleId());
            if (result != 0) {
                return result;
            }
        }
        // sort inverse for bpmnProcessId because items having
        // a bpmnProcessId set should be listed first
        if (!StringUtils.hasText(getBpmnProcessId())) {
            if (StringUtils.hasText(other.getBpmnProcessId())) {
                return 1;
            }
            return 0;
        } else if (!StringUtils.hasText(other.getBpmnProcessId())) {
            return -1;
        }
        return getBpmnProcessId()
                .compareTo(other.getBpmnProcessId());

    }

    public Map<String, UserTaskProperties> getUserTasks() {
        return userTasks;
    }

    public void setUserTasks(Map<String, UserTaskProperties> userTasks) {
        this.userTasks = userTasks;
    }
    
    public String getTemplatesPath() {
        return templatesPath;
    }
    
    public void setTemplatesPath(String templatesPath) {
        this.templatesPath = templatesPath;
    }

    public String getUiUriType() {
        return uiUriType;
    }

    public void setUiUriType(String uiUriType) {
        this.uiUriType = uiUriType;
    }

    public String getWorkflowModuleUri() {
        return workflowModuleUri;
    }

    public void setWorkflowModuleUri(String workflowModuleUri) {
        this.workflowModuleUri = workflowModuleUri;
    }

    public String getTaskProviderApiPath() {
        return taskProviderApiPath;
    }

    public void setTaskProviderApiPath(String taskProviderApiPath) {
        this.taskProviderApiPath = taskProviderApiPath;
    }

    public String getUiUriPath() {
        return uiUriPath;
    }

    public void setUiUriPath(String uiUriPath) {
        this.uiUriPath = uiUriPath;
    }
    
    public Map<String, DetailsProperties> getDetailsProperties() {
        return detailsProperties;
    }
    
    public void setDetailsProperties(Map<String, DetailsProperties> detailsProperties) {
        this.detailsProperties = detailsProperties;
    }
    
    public String getBpmnDescriptionLanguage() {
        return bpmnDescriptionLanguage;
    }
    
    public void setBpmnDescriptionLanguage(String bpmnDescriptionLanguage) {
        this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;
    }
    
}
