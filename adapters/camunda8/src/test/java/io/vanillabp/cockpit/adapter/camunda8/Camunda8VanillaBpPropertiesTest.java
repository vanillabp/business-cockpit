package io.vanillabp.cockpit.adapter.camunda8;

import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties.AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties.TaskProperties;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties.WorkerProperties;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties.WorkflowAdapterProperties;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties.WorkflowModuleAdapterProperties;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda8VanillaBpPropertiesTest {

    private Camunda8VanillaBpProperties properties;

    @BeforeEach
    void setUp() {
        properties = new Camunda8VanillaBpProperties();
    }

    // --- getTenantId ---

    @Test
    void getTenantId_withNoConfiguration_returnsWorkflowModuleIdAsTenant() {
        // No workflow modules configured
        properties.setWorkflowModules(Map.of());

        // Tenant ID should be the workflow module ID when using tenants is true by default
        final var tenantId = properties.getTenantId("my-module");

        assertThat(tenantId).isEqualTo("my-module");
    }

    @Test
    void getTenantId_withUseTenantsFalse_returnsNull() {
        // Configure adapter with useTenants = false
        final var adapterConfig = new AdapterConfiguration();
        adapterConfig.setUseTenants(false);

        final var moduleProps = new WorkflowModuleAdapterProperties();
        moduleProps.setAdapters(Map.of(Camunda8AdapterConfiguration.ADAPTER_ID, adapterConfig));

        properties.setWorkflowModules(Map.of("my-module", moduleProps));

        // Tenant ID should be null when useTenants is false
        final var tenantId = properties.getTenantId("my-module");

        assertThat(tenantId).isNull();
    }

    @Test
    void getTenantId_withExplicitTenantId_returnsConfiguredTenant() {
        // Configure adapter with explicit tenant ID
        final var adapterConfig = new AdapterConfiguration();
        adapterConfig.setUseTenants(true);
        adapterConfig.setTenantId("custom-tenant");

        final var moduleProps = new WorkflowModuleAdapterProperties();
        moduleProps.setAdapters(Map.of(Camunda8AdapterConfiguration.ADAPTER_ID, adapterConfig));

        properties.setWorkflowModules(Map.of("my-module", moduleProps));

        // Tenant ID should be the configured custom tenant
        final var tenantId = properties.getTenantId("my-module");

        assertThat(tenantId).isEqualTo("custom-tenant");
    }

    // --- getWorkerProperties ---

    @Test
    void getWorkerProperties_withNoConfiguration_returnsDefaultProperties() {
        // No workflow modules configured
        properties.setWorkflowModules(Map.of());

        // Get worker properties for non-existent module
        final var workerProps = properties.getWorkerProperties("unknown-module", "process", "task");

        // Should return empty/default worker properties
        assertThat(workerProps).isNotNull();
        assertThat(workerProps.getTaskTimeout()).isNull();
        assertThat(workerProps.getPollInterval()).isNull();
    }

    @Test
    void getWorkerProperties_withModuleLevelConfig_appliesModuleSettings() {
        // Configure worker properties at module level
        final var adapterConfig = new AdapterConfiguration();
        adapterConfig.setTaskTimeout(Duration.ofMinutes(5));
        adapterConfig.setPollInterval(Duration.ofSeconds(30));

        final var moduleProps = new WorkflowModuleAdapterProperties();
        moduleProps.setAdapters(Map.of(Camunda8AdapterConfiguration.ADAPTER_ID, adapterConfig));

        properties.setWorkflowModules(Map.of("my-module", moduleProps));

        // Get worker properties
        final var workerProps = properties.getWorkerProperties("my-module", null, null);

        // Verify module-level settings are applied
        assertThat(workerProps.getTaskTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(workerProps.getPollInterval()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void getWorkerProperties_withWorkflowLevelConfig_overridesModuleSettings() {
        // Configure module-level settings
        final var moduleAdapterConfig = new AdapterConfiguration();
        moduleAdapterConfig.setTaskTimeout(Duration.ofMinutes(5));
        moduleAdapterConfig.setPollInterval(Duration.ofSeconds(30));

        // Configure workflow-level settings
        final var workflowAdapterConfig = new WorkerProperties();
        workflowAdapterConfig.setTaskTimeout(Duration.ofMinutes(10));

        final var workflowProps = new WorkflowAdapterProperties();
        workflowProps.setAdapters(Map.of(Camunda8AdapterConfiguration.ADAPTER_ID, workflowAdapterConfig));

        final var moduleProps = new WorkflowModuleAdapterProperties();
        moduleProps.setAdapters(Map.of(Camunda8AdapterConfiguration.ADAPTER_ID, moduleAdapterConfig));
        moduleProps.setWorkflows(Map.of("my-process", workflowProps));

        properties.setWorkflowModules(Map.of("my-module", moduleProps));

        // Get worker properties for specific workflow
        final var workerProps = properties.getWorkerProperties("my-module", "my-process", null);

        // Workflow-level task timeout should override module-level
        assertThat(workerProps.getTaskTimeout()).isEqualTo(Duration.ofMinutes(10));
        // Poll interval should be inherited from module level
        assertThat(workerProps.getPollInterval()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void getWorkerProperties_withTaskLevelConfig_overridesWorkflowSettings() {
        // Configure workflow-level settings
        final var workflowAdapterConfig = new WorkerProperties();
        workflowAdapterConfig.setTaskTimeout(Duration.ofMinutes(10));
        workflowAdapterConfig.setPollInterval(Duration.ofSeconds(30));

        // Configure task-level settings
        final var taskAdapterConfig = new WorkerProperties();
        taskAdapterConfig.setTaskTimeout(Duration.ofMinutes(2));

        final var taskProps = new TaskProperties();
        taskProps.setAdapters(Map.of(Camunda8AdapterConfiguration.ADAPTER_ID, taskAdapterConfig));

        final var workflowProps = new WorkflowAdapterProperties();
        workflowProps.setAdapters(Map.of(Camunda8AdapterConfiguration.ADAPTER_ID, workflowAdapterConfig));
        workflowProps.setTasks(Map.of("my-task", taskProps));

        final var moduleProps = new WorkflowModuleAdapterProperties();
        moduleProps.setWorkflows(Map.of("my-process", workflowProps));

        properties.setWorkflowModules(Map.of("my-module", moduleProps));

        // Get worker properties for specific task
        final var workerProps = properties.getWorkerProperties("my-module", "my-process", "my-task");

        // Task-level task timeout should override workflow-level
        assertThat(workerProps.getTaskTimeout()).isEqualTo(Duration.ofMinutes(2));
        // Poll interval should be inherited from workflow level
        assertThat(workerProps.getPollInterval()).isEqualTo(Duration.ofSeconds(30));
    }

    // --- WorkerProperties ---

    @Test
    void workerProperties_apply_copiesNonNullValues() {
        // Create original with some values
        final var original = new WorkerProperties();
        original.setTaskTimeout(Duration.ofMinutes(5));
        original.setPollInterval(Duration.ofSeconds(30));
        original.setStreamEnabled(true);

        // Apply to new instance
        final var target = new WorkerProperties();
        target.apply(original);

        // Verify values are copied
        assertThat(target.getTaskTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(target.getPollInterval()).isEqualTo(Duration.ofSeconds(30));
        assertThat(target.isStreamEnabled()).isTrue();
    }

    @Test
    void workerProperties_apply_doesNotOverwriteWithNull() {
        // Create target with existing values
        final var target = new WorkerProperties();
        target.setTaskTimeout(Duration.ofMinutes(10));
        target.setPollInterval(Duration.ofSeconds(60));

        // Create original with only some values
        final var original = new WorkerProperties();
        original.setTaskTimeout(Duration.ofMinutes(5));
        // pollInterval is null

        // Apply original to target
        target.apply(original);

        // Task timeout should be updated
        assertThat(target.getTaskTimeout()).isEqualTo(Duration.ofMinutes(5));
        // Poll interval should remain unchanged (original was null)
        assertThat(target.getPollInterval()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void workerProperties_setAndGet_allProperties() {
        final var props = new WorkerProperties();

        // Set all properties
        props.setTaskTimeout(Duration.ofMinutes(5));
        props.setPollInterval(Duration.ofSeconds(30));
        props.setPollRequestTimeout(Duration.ofSeconds(10));
        props.setStreamEnabled(true);
        props.setStreamTimeout(Duration.ofMinutes(1));

        // Verify all getters
        assertThat(props.getTaskTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(props.getPollInterval()).isEqualTo(Duration.ofSeconds(30));
        assertThat(props.getPollRequestTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(props.isStreamEnabled()).isTrue();
        assertThat(props.getStreamTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

    // --- AdapterConfiguration ---

    @Test
    void adapterConfiguration_defaultValues() {
        final var config = new AdapterConfiguration();

        // Default useTenants should be true
        assertThat(config.isUseTenants()).isTrue();
        assertThat(config.getTenantId()).isNull();
    }

    @Test
    void adapterConfiguration_setAndGet() {
        final var config = new AdapterConfiguration();

        config.setUseTenants(false);
        config.setTenantId("my-tenant");

        assertThat(config.isUseTenants()).isFalse();
        assertThat(config.getTenantId()).isEqualTo("my-tenant");
    }

    // --- setWorkflowModules ---

    @Test
    void setWorkflowModules_setsWorkflowModuleIdOnProperties() {
        // Create module properties
        final var moduleProps = new WorkflowModuleAdapterProperties();

        // Set workflow modules
        properties.setWorkflowModules(Map.of("my-module", moduleProps));

        // Verify workflowModuleId is set automatically
        assertThat(moduleProps.workflowModuleId).isEqualTo("my-module");
    }

    // --- WorkflowAdapterProperties ---

    @Test
    void workflowAdapterProperties_setWorkflows_setsBpmnProcessIdAndWorkflowModule() {
        // Create workflow properties
        final var workflowProps = new WorkflowAdapterProperties();

        // Create module properties and set workflows
        final var moduleProps = new WorkflowModuleAdapterProperties();
        moduleProps.setWorkflows(Map.of("my-process", workflowProps));

        // Verify bpmnProcessId is set automatically
        assertThat(workflowProps.getBpmnProcessId()).isEqualTo("my-process");

        // Verify parent reference is set
        assertThat(workflowProps.getWorkflowModule()).isSameAs(moduleProps);
    }

}
