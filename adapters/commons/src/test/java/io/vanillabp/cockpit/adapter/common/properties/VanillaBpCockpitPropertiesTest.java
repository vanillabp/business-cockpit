package io.vanillabp.cockpit.adapter.common.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VanillaBpCockpitPropertiesTest {

    private VanillaBpCockpitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new VanillaBpCockpitProperties();
    }

    @Test
    void setAndGetCockpit() {
        CockpitProperties cockpit = new CockpitProperties();
        properties.setCockpit(cockpit);
        assertThat(properties.getCockpit()).isEqualTo(cockpit);
    }

    @Test
    void defaultWorkflowModules_isEmpty() {
        assertThat(properties.getWorkflowModules()).isEmpty();
    }

    @Test
    void setWorkflowModules_setsWorkflowModuleIdAndDefaultProperties() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        modules.put("test-module", module);

        properties.setWorkflowModules(modules);

        assertThat(properties.getWorkflowModules()).hasSize(1);
    }

    @Test
    void getUiUriType_withValidModule_returnsUiUriType() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        module.getCockpit().setUiUriType("WEBPACK_MF_REACT");
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getUiUriType("test-module")).isEqualTo("WEBPACK_MF_REACT");
    }

    @Test
    void getUiUriType_withMissingModule_throwsRuntimeException() {
        assertThatThrownBy(() -> properties.getUiUriType("non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Property for UI-URI-type not found");
    }

    @Test
    void getUiUriType_withEmptyUiUriType_throwsRuntimeException() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThatThrownBy(() -> properties.getUiUriType("test-module"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Property for UI-URI-type not found");
    }

    @Test
    void getWorkflowModuleUri_withValidModule_returnsUri() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        module.getCockpit().setWorkflowModuleUri("http://localhost:8080");
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getWorkflowModuleUri("test-module")).isEqualTo("http://localhost:8080");
    }

    @Test
    void getWorkflowModuleUri_withMissingModule_throwsRuntimeException() {
        assertThatThrownBy(() -> properties.getWorkflowModuleUri("non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Property for workflow-module-uri not found");
    }

    @Test
    void getUiUriPath_withValidModule_returnsPath() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        module.getCockpit().setUiUriPath("/ui/path");
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getUiUriPath("test-module")).isEqualTo("/ui/path");
    }

    @Test
    void getUiUriPath_withMissingModule_throwsRuntimeException() {
        assertThatThrownBy(() -> properties.getUiUriPath("non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Property for UI-URI-path not found");
    }

    @Test
    void getI18nLanguages_withModuleLevelLanguages_returnsLanguages() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        module.getCockpit().setI18nLanguages(Arrays.asList("en", "de"));
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getI18nLanguages("test-module", "process-1"))
                .containsExactly("en", "de");
    }

    @Test
    void getI18nLanguages_withWorkflowLevelLanguages_overridesModuleLanguages() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        module.getCockpit().setI18nLanguages(Arrays.asList("en", "de"));

        Map<String, VanillaBpCockpitProperties.WorkflowAdapterProperties> workflows = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowAdapterProperties workflow = new VanillaBpCockpitProperties.WorkflowAdapterProperties();
        workflow.getCockpit().setI18nLanguages(Arrays.asList("fr", "es"));
        workflows.put("process-1", workflow);
        module.setWorkflows(workflows);

        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getI18nLanguages("test-module", "process-1"))
                .containsExactly("fr", "es");
    }

    @Test
    void getI18nLanguages_withMissingLanguages_throwsRuntimeException() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThatThrownBy(() -> properties.getI18nLanguages("test-module", "process-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No property for i18n languages found");
    }

    @Test
    void getBpmnDescriptionLanguage_withModuleLevelLanguage_returnsLanguage() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        module.getCockpit().setBpmnDescriptionLanguage("en");
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getBpmnDescriptionLanguage("test-module", "process-1")).isEqualTo("en");
    }

    @Test
    void getBpmnDescriptionLanguage_withWorkflowLevelLanguage_overridesModuleLanguage() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        module.getCockpit().setBpmnDescriptionLanguage("en");

        Map<String, VanillaBpCockpitProperties.WorkflowAdapterProperties> workflows = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowAdapterProperties workflow = new VanillaBpCockpitProperties.WorkflowAdapterProperties();
        workflow.getCockpit().setBpmnDescriptionLanguage("de");
        workflows.put("process-1", workflow);
        module.setWorkflows(workflows);

        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getBpmnDescriptionLanguage("test-module", "process-1")).isEqualTo("de");
    }

    @Test
    void getBpmnDescriptionLanguage_withMissingLanguage_throwsRuntimeException() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThatThrownBy(() -> properties.getBpmnDescriptionLanguage("test-module", "process-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No property for BPMN language found");
    }

    @Test
    void getTemplatePath_withWorkflowModuleIdOnly_returnsDefaultPath() {
        assertThat(properties.getTemplatePath("my-module"))
                .isEqualTo("my-module");
    }

    @Test
    void getTemplatePath_withWorkflowModuleIdAndBpmnProcessId_returnsDefaultPath() {
        assertThat(properties.getTemplatePath("my-module", "my-process"))
                .isEqualTo("my-module" + File.separator + "my-process");
    }

    @Test
    void getTemplatePath_withAllParameters_returnsDefaultPath() {
        assertThat(properties.getTemplatePath("my-module", "my-process", "my-task"))
                .isEqualTo("my-module" + File.separator + "my-process" + File.separator + "my-task");
    }

    @Test
    void getTemplatePath_withCustomModulePath_returnsCustomPath() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        module.getCockpit().setTemplatePath("custom-module-path");
        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getTemplatePath("test-module"))
                .isEqualTo("custom-module-path");
    }

    @Test
    void getTemplatePath_withCustomWorkflowPath_returnsCustomPath() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();

        Map<String, VanillaBpCockpitProperties.WorkflowAdapterProperties> workflows = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowAdapterProperties workflow = new VanillaBpCockpitProperties.WorkflowAdapterProperties();
        workflow.getCockpit().setTemplatePath("custom-workflow-path");
        workflows.put("process-1", workflow);
        module.setWorkflows(workflows);

        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getTemplatePath("test-module", "process-1"))
                .isEqualTo("test-module" + File.separator + "custom-workflow-path");
    }

    @Test
    void getTemplatePath_withCustomUserTaskPath_returnsCustomPath() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();

        Map<String, VanillaBpCockpitProperties.WorkflowAdapterProperties> workflows = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowAdapterProperties workflow = new VanillaBpCockpitProperties.WorkflowAdapterProperties();

        Map<String, VanillaBpCockpitProperties.UserTaskProperties> userTasks = new HashMap<>();
        VanillaBpCockpitProperties.UserTaskProperties userTask = new VanillaBpCockpitProperties.UserTaskProperties();
        VanillaBpCockpitProperties.CockpitAdapterProperties taskCockpit = new VanillaBpCockpitProperties.CockpitAdapterProperties();
        taskCockpit.setTemplatePath("custom-task-path");
        userTask.setCockpit(taskCockpit);
        userTasks.put("task-1", userTask);
        workflow.setUserTasks(userTasks);

        workflows.put("process-1", workflow);
        module.setWorkflows(workflows);

        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(properties.getTemplatePath("test-module", "process-1", "task-1"))
                .isEqualTo("test-module" + File.separator + "process-1" + File.separator + "custom-task-path");
    }

    @Test
    void workflowModuleAdapterProperties_getCockpit_returnsDefault() {
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();
        assertThat(module.getCockpit()).isNotNull();
    }

    @Test
    void workflowAdapterProperties_getCockpit_returnsDefault() {
        VanillaBpCockpitProperties.WorkflowAdapterProperties workflow = new VanillaBpCockpitProperties.WorkflowAdapterProperties();
        assertThat(workflow.getCockpit()).isNotNull();
    }

    @Test
    void workflowAdapterProperties_getWorkflowModule_returnsModule() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();

        Map<String, VanillaBpCockpitProperties.WorkflowAdapterProperties> workflows = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowAdapterProperties workflow = new VanillaBpCockpitProperties.WorkflowAdapterProperties();
        workflows.put("process-1", workflow);
        module.setWorkflows(workflows);

        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(workflow.getWorkflowModule()).isEqualTo(module);
    }

    @Test
    void workflowAdapterProperties_getBpmnProcessId_returnsId() {
        Map<String, VanillaBpCockpitProperties.WorkflowModuleAdapterProperties> modules = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowModuleAdapterProperties module = new VanillaBpCockpitProperties.WorkflowModuleAdapterProperties();

        Map<String, VanillaBpCockpitProperties.WorkflowAdapterProperties> workflows = new HashMap<>();
        VanillaBpCockpitProperties.WorkflowAdapterProperties workflow = new VanillaBpCockpitProperties.WorkflowAdapterProperties();
        workflows.put("process-1", workflow);
        module.setWorkflows(workflows);

        modules.put("test-module", module);
        properties.setWorkflowModules(modules);

        assertThat(workflow.getBpmnProcessId()).isEqualTo("process-1");
    }

    @Test
    void userTaskProperties_getCockpit_returnsValue() {
        VanillaBpCockpitProperties.UserTaskProperties userTask = new VanillaBpCockpitProperties.UserTaskProperties();
        VanillaBpCockpitProperties.CockpitAdapterProperties cockpit = new VanillaBpCockpitProperties.CockpitAdapterProperties();
        userTask.setCockpit(cockpit);
        assertThat(userTask.getCockpit()).isEqualTo(cockpit);
    }

    @Test
    void cockpitAdapterProperties_setAndGetTemplatePath() {
        VanillaBpCockpitProperties.CockpitAdapterProperties cockpit = new VanillaBpCockpitProperties.CockpitAdapterProperties();
        cockpit.setTemplatePath("/custom/path");
        assertThat(cockpit.getTemplatePath()).isEqualTo("/custom/path");
    }

    @Test
    void workflowModuleCockpitAdapterProperties_allGettersAndSetters() {
        VanillaBpCockpitProperties.WorkflowModuleCockpitAdapterProperties cockpit = new VanillaBpCockpitProperties.WorkflowModuleCockpitAdapterProperties();

        cockpit.setWorkflowModuleUri("http://example.com");
        cockpit.setUiUriType("EXTERNAL");
        cockpit.setUiUriPath("/ui");
        cockpit.setBpmnDescriptionLanguage("en");
        List<String> languages = Arrays.asList("en", "de");
        cockpit.setI18nLanguages(languages);

        assertThat(cockpit.getWorkflowModuleUri()).isEqualTo("http://example.com");
        assertThat(cockpit.getUiUriType()).isEqualTo("EXTERNAL");
        assertThat(cockpit.getUiUriPath()).isEqualTo("/ui");
        assertThat(cockpit.getBpmnDescriptionLanguage()).isEqualTo("en");
        assertThat(cockpit.getI18nLanguages()).isEqualTo(languages);
    }

    @Test
    void workflowCockpitAdapterProperties_allGettersAndSetters() {
        VanillaBpCockpitProperties.WorkflowCockpitAdapterProperties cockpit = new VanillaBpCockpitProperties.WorkflowCockpitAdapterProperties();

        cockpit.setBpmnDescriptionLanguage("de");
        List<String> languages = Arrays.asList("fr", "es");
        cockpit.setI18nLanguages(languages);

        assertThat(cockpit.getBpmnDescriptionLanguage()).isEqualTo("de");
        assertThat(cockpit.getI18nLanguages()).isEqualTo(languages);
    }
}
