package io.vanillabp.cockpit.adapter.camunda8.deployments;

import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowWiring;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class Camunda8DeploymentAdapterTest {

    @Mock
    private VanillaBpProperties properties;

    @Mock
    private Camunda8VanillaBpProperties camunda8Properties;

    @Mock
    private VanillaBpCockpitProperties cockpitProperties;

    @Mock
    private Camunda8UserTaskWiring camunda8UserTaskWiring;

    @Mock
    private Camunda8WorkflowWiring camunda8WorkflowWiring;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private Camunda8DeploymentAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new Camunda8DeploymentAdapter(
                "test-app",
                properties,
                camunda8Properties,
                cockpitProperties,
                camunda8UserTaskWiring,
                camunda8WorkflowWiring,
                applicationEventPublisher);
    }

    @Test
    void getAdapterId_returnsExpectedValue() {
        // The adapter ID constant should be defined
        assertThat(Camunda8AdapterConfiguration.ADAPTER_ID).isNotNull();
        assertThat(Camunda8AdapterConfiguration.ADAPTER_ID).isEqualTo("camunda8");
    }

    @Test
    void modelCachePrefix_hasCorrectValue() {
        // Verify MODELCACHE_PREFIX
        assertThat(Camunda8DeploymentAdapter.MODELCACHE_PREFIX).isEqualTo("C8_");
    }

    @Test
    void versionInfoCurrent_hasCorrectValue() {
        // Verify VERSIONINFO_CURRENT
        assertThat(Camunda8DeploymentAdapter.VERSIONINFO_CURRENT).isEqualTo("current");
    }

    @Test
    void adapterPackage_hasCorrectValue() {
        // Verify ADAPTER_PACKAGE
        assertThat(Camunda8DeploymentAdapter.ADAPTER_PACKAGE).isEqualTo("io.vanillabp.camunda8.businesscockpit");
    }

    @Test
    void propertyDeploymentPriority_hasCorrectValue() {
        // Verify PROPERTY_DEPLOYMENT_PRIORITY
        assertThat(Camunda8DeploymentAdapter.PROPERTY_DEPLOYMENT_PRIORITY).isEqualTo("io.vanillabp.deployment.priority");
    }

    @Test
    void propertyTaskListenerPrefixes_hasCorrectValue() {
        // Verify PROPERTY_TASKLISTENER_PREFIXES
        assertThat(Camunda8DeploymentAdapter.PROPERTY_TASKLISTENER_PREFIXES).isEqualTo("io.vanillabp.businesscockpit.tasklistener.prefixes");
    }

    @Test
    void propertyExecutionListenerPrefixes_hasCorrectValue() {
        // Verify PROPERTY_EXECUTIONLISTENER_PREFIXES
        assertThat(Camunda8DeploymentAdapter.PROPERTY_EXECUTIONLISTENER_PREFIXES).isEqualTo("io.vanillabp.businesscockpit.executionlistener.prefixes");
    }

    @Test
    void processDefinitionNeedsKafka_withUnknownDefinition_returnsFalse() {
        // Query for unknown process definition
        final var result = adapter.processDefinitionNeedsKafka(999L);

        // Should return false for unknown definitions
        assertThat(result).isFalse();
    }

    @Test
    void processDefinitionNeedsKafka_withZeroKey_returnsFalse() {
        // Query for process definition with key 0
        final var result = adapter.processDefinitionNeedsKafka(0L);

        // Should return false
        assertThat(result).isFalse();
    }

    @Test
    void processDefinitionNeedsKafka_withNegativeKey_returnsFalse() {
        // Query for process definition with negative key
        final var result = adapter.processDefinitionNeedsKafka(-1L);

        // Should return false
        assertThat(result).isFalse();
    }

    @Test
    void initializeCrossCuttingProperties_doesNotThrow() {
        // Verify that initializeCrossCuttingProperties can be called without throwing
        Camunda8DeploymentAdapter.initializeCrossCuttingProperties();
        // No exception means success
    }

    @Test
    void deployBpmnModels_withNoClient_doesNotThrow() {
        // Without a client set, deployBpmnModels should handle the case gracefully
        // This tests the initial state
        adapter.deployBpmnModels();
        // No exception means success - empty cache is handled
    }

}
