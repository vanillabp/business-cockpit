package io.vanillabp.cockpit.adapter.common.properties;

import io.vanillabp.springboot.adapter.VanillaBpProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = VanillaBpProperties.PREFIX, ignoreUnknownFields = true)
public class VanillaBpCockpitProperties {

    private CockpitProperties cockpit;

    public CockpitProperties getCockpit() {
        return cockpit;
    }

    public void setCockpit(CockpitProperties cockpit) {
        this.cockpit = cockpit;
    }

    private Map<String, WorkflowModuleAdapterProperties> workflowModules = Map.of();

    public Map<String, WorkflowModuleAdapterProperties> getWorkflowModules() {
        return workflowModules;
    }

    public void setWorkflowModules(Map<String, WorkflowModuleAdapterProperties> workflowModules) {

        this.workflowModules = workflowModules;
        workflowModules.forEach((workflowModuleId, properties) -> {
            properties.workflowModuleId = workflowModuleId;
            properties.defaultProperties = this;
        });

    }

    private WorkflowModuleAdapterProperties getWorkflowModule(
            final String workflowModuleId) {

        final var workflowModule = getWorkflowModules().get(workflowModuleId);
        if (workflowModule == null) {
            throw new RuntimeException(
                    "No property '"
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + workflowModuleId
                            + "' found!");
        }
        return workflowModule;

    }

    public String getUiUriType(
            final String workflowModuleId) {

        final var workflowModule = getWorkflowModule(workflowModuleId);
        final var uiUriType = workflowModule.getCockpit().getUiUriType();
        if (!StringUtils.hasText(uiUriType)) {
            throw new RuntimeException(
                    "Property for UI-URI-type not found:\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + workflowModuleId
                            + ".ui-uri-type");
        }
        return uiUriType;

    }

    public String getWorkflowModuleUri(
            final String workflowModuleId) {

        final var workflowModule = getWorkflowModule(workflowModuleId);
        final var workflowModuleUri = workflowModule.getCockpit().getWorkflowModuleUri();
        if (!StringUtils.hasText(workflowModuleUri)) {
            throw new RuntimeException(
                    "Property for workflow-module-uri not found:\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + workflowModuleId
                            + ".workflow-module-uri\n"
                            + "You may wish to use a property since the endpoint might defined later: ${containerUri}/my-workflow-module");
        }
        return workflowModuleUri;

    }

    public String getUiUriPath(
            final String workflowModuleId) {

        final var workflowModule = getWorkflowModule(workflowModuleId);
        final var uiUriPath = workflowModule.getCockpit().getUiUriPath();
        if (!StringUtils.hasText(uiUriPath)) {
            throw new RuntimeException(
                    "Property for UI-URI-path not found:\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + workflowModuleId
                            + ".ui-uri-path");
        }
        return uiUriPath;

    }

    public List<String> getI18nLanguages(
            final String workflowModuleId,
            final String bpmnProcessId) {

        final var workflowModule = getWorkflowModule(workflowModuleId);
        var i18nLanguages = workflowModule.getCockpit().getI18nLanguages();
        final var workflow = workflowModule.getWorkflows().get(bpmnProcessId);
        if ((workflow != null)
                && (workflow.getCockpit().getI18nLanguages() != null)) {
            i18nLanguages = workflow.getCockpit().getI18nLanguages();
        }
        if (i18nLanguages == null) {
            throw new RuntimeException(
                    "No property for i18n languages found. Use one of these properties:\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + workflowModuleId
                            + ".cockpit.i18n-languages\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + workflowModuleId
                            + ".workflows."
                            + bpmnProcessId
                            + ".cockpit.i18n-languages");
        }
        return i18nLanguages;

    }

    public String getBpmnDescriptionLanguage(
            final String workflowModuleId,
            final String bpmnProcessId) {

        final var workflowModule = getWorkflowModule(workflowModuleId);
        var language = workflowModule.getCockpit().getBpmnDescriptionLanguage();
        final var workflow = workflowModule.getWorkflows().get(bpmnProcessId);
        if ((workflow != null)
                && StringUtils.hasText(workflow.getCockpit().getBpmnDescriptionLanguage())) {
            language = workflow.getCockpit().getBpmnDescriptionLanguage();
        }
        if (!StringUtils.hasText(language)) {
            throw new RuntimeException(
                    "No property for BPMN language found. Use one of these properties:\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + workflowModuleId
                            + ".cockpit.bpmn-description-language\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + workflowModuleId
                            + ".workflows."
                            + bpmnProcessId
                            + ".cockpit.bpmn-description-language");
        }
        return language;

    }

    public String getTemplatePath(
            final String workflowModuleId) {

        return getTemplatePath(workflowModuleId, null, null);

    }

    public String getTemplatePath(
            final String workflowModuleId,
            final String bpmnProcessId) {

        return getTemplatePath(workflowModuleId, bpmnProcessId, null);

    }

    public String getTemplatePath(
            final String workflowModuleId,
            final String bpmnProcessId,
            final String taskDefinition) {

        final var templatePath = new LinkedList<String>();

        final var workflowModule = getWorkflowModules().get(workflowModuleId);
        if (workflowModule == null) {
            templatePath.add(workflowModuleId);
            if (bpmnProcessId != null) {
                templatePath.add(bpmnProcessId);
            }
            if (taskDefinition != null) {
                templatePath.add(taskDefinition);
            }
        } else {
            if (StringUtils.hasText(workflowModule.getCockpit().getTemplatePath())) {
                templatePath.add(workflowModule.getCockpit().getTemplatePath());
            } else {
                templatePath.add(workflowModuleId);
            }
            if (bpmnProcessId != null) {
                final var workflow = workflowModule.getWorkflows().get(bpmnProcessId);
                if (workflow == null) {
                    templatePath.add(bpmnProcessId);
                    if (taskDefinition != null) {
                        templatePath.add(taskDefinition);
                    }
                } else {
                    if (StringUtils.hasText(workflow.getCockpit().getTemplatePath())) {
                        templatePath.add(workflow.getCockpit().getTemplatePath());
                    } else {
                        templatePath.add(bpmnProcessId);
                    }
                    if (taskDefinition != null) {
                        final var userTask = workflow.getUserTasks().get(taskDefinition);
                        if (userTask == null) {
                            templatePath.add(taskDefinition);
                        } else {
                            if (StringUtils.hasText(userTask.getCockpit().getTemplatePath())) {
                                templatePath.add(userTask.getCockpit().getTemplatePath());
                            } else {
                                templatePath.add(taskDefinition);
                            }
                        }
                    }
                }
            }
        }

        return String.join(File.separator, templatePath);

    }

    public static class WorkflowModuleAdapterProperties {

        String workflowModuleId;

        VanillaBpCockpitProperties defaultProperties;

        private WorkflowModuleCockpitAdapterProperties cockpit = new WorkflowModuleCockpitAdapterProperties();

        private Map<String, WorkflowAdapterProperties> workflows = Map.of();

        public Map<String, WorkflowAdapterProperties> getWorkflows() { return workflows; }

        public void setWorkflows(Map<String, WorkflowAdapterProperties> workflows) {

            this.workflows = workflows;
            workflows.forEach((bpmnProcessId, properties) -> {
                properties.bpmnProcessId = bpmnProcessId;
                properties.workflowModule = this;
            });

        }

        public WorkflowModuleCockpitAdapterProperties getCockpit() {
            return cockpit;
        }

        public void setCockpit(WorkflowModuleCockpitAdapterProperties cockpit) {
            this.cockpit = cockpit;
        }

    }

    public static class WorkflowModuleCockpitAdapterProperties extends CockpitAdapterProperties {

        private String workflowModuleUri;

        private String uiUriType;

        private String uiUriPath;

        private String bpmnDescriptionLanguage;

        private List<String> i18nLanguages;

        public String getWorkflowModuleUri() {
            return workflowModuleUri;
        }

        public void setWorkflowModuleUri(String workflowModuleUri) {
            this.workflowModuleUri = workflowModuleUri;
        }

        public String getUiUriType() {
            return uiUriType;
        }

        public void setUiUriType(String uiUriType) {
            this.uiUriType = uiUriType;
        }

        public String getUiUriPath() {
            return uiUriPath;
        }

        public void setUiUriPath(String uiUriPath) {
            this.uiUriPath = uiUriPath;
        }

        public List<String> getI18nLanguages() {
            return i18nLanguages;
        }

        public void setI18nLanguages(List<String> i18nLanguages) {
            this.i18nLanguages = i18nLanguages;
        }

        public String getBpmnDescriptionLanguage() {
            return bpmnDescriptionLanguage;
        }

        public void setBpmnDescriptionLanguage(String bpmnDescriptionLanguage) {
            this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;
        }

    }

    public static class WorkflowAdapterProperties {

        String bpmnProcessId;

        WorkflowModuleAdapterProperties workflowModule;

        private Map<String, UserTaskProperties> userTasks = Map.of();

        private WorkflowCockpitAdapterProperties cockpit = new WorkflowCockpitAdapterProperties();

        public Map<String, UserTaskProperties> getUserTasks() {
            return userTasks;
        }

        public void setUserTasks(Map<String, UserTaskProperties> userTasks) {
            this.userTasks = userTasks;
        }

        public WorkflowModuleAdapterProperties getWorkflowModule() {
            return workflowModule;
        }

        public String getBpmnProcessId() {
            return bpmnProcessId;
        }

        public WorkflowCockpitAdapterProperties getCockpit() {
            return cockpit;
        }

        public void setCockpit(WorkflowCockpitAdapterProperties cockpit) {
            this.cockpit = cockpit;
        }

    }

    public static class WorkflowCockpitAdapterProperties extends CockpitAdapterProperties {

        private String bpmnDescriptionLanguage;

        private List<String> i18nLanguages;

        public List<String> getI18nLanguages() {
            return i18nLanguages;
        }

        public void setI18nLanguages(List<String> i18nLanguages) {
            this.i18nLanguages = i18nLanguages;
        }

        public String getBpmnDescriptionLanguage() {
            return bpmnDescriptionLanguage;
        }

        public void setBpmnDescriptionLanguage(String bpmnDescriptionLanguage) {
            this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;
        }

    }

    public static class UserTaskProperties {

        private CockpitAdapterProperties cockpit;

        public CockpitAdapterProperties getCockpit() {
            return cockpit;
        }

        public void setCockpit(CockpitAdapterProperties cockpit) {
            this.cockpit = cockpit;
        }

    }

    public static class CockpitAdapterProperties {

        private String templatePath;

        public String getTemplatePath() {
            return templatePath;
        }

        public void setTemplatePath(String templatePath) {
            this.templatePath = templatePath;
        }

    }

}
