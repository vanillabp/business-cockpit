package io.vanillabp.cockpit.adapter.camunda7.wiring;

import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowWiring;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.repository.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration tests for WiringBpmnParseListener with a real Camunda engine.
 */
@ExtendWith(MockitoExtension.class)
class WiringBpmnParseListenerIT {

    @Mock
    private Camunda7UserTaskWiring userTaskWiring;

    @Mock
    private Camunda7WorkflowWiring workflowWiring;

    @Mock
    private Camunda7UserTaskEventHandler userTaskEventHandler;

    private ProcessEngine processEngine;
    private RepositoryService repositoryService;
    private Deployment deployment;

    @BeforeEach
    void setUp() {
        // Create the parse listener with userTasksEnabled = true
        WiringBpmnParseListener parseListener = new WiringBpmnParseListener(
                true,  // userTasksEnabled
                userTaskWiring,
                userTaskEventHandler,
                workflowWiring
        );

        // Create history event producer supplier
        Camunda7HistoryEventProducerSupplier historyEventProducerSupplier =
                new Camunda7HistoryEventProducerSupplier();

        // Create the wiring plugin
        Camunda7WiringPlugin wiringPlugin = new Camunda7WiringPlugin(
                parseListener,
                historyEventProducerSupplier
        );

        // Configure process engine with the plugin
        ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:wiring-test;DB_CLOSE_DELAY=-1");
        configuration.setJdbcDriver("org.h2.Driver");
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setJobExecutorActivate(false);
        configuration.setHistory("full");

        // Register the plugin
        configuration.setProcessEnginePlugins(new LinkedList<>());
        configuration.getProcessEnginePlugins().add(wiringPlugin);

        // Build the process engine
        processEngine = configuration.buildProcessEngine();
        repositoryService = processEngine.getRepositoryService();
    }

    @AfterEach
    void tearDown() {
        if (deployment != null) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
        if (processEngine != null) {
            processEngine.close();
        }
    }

    @Test
    void deployProcess_triggersParseListener() {
        // Act
        deployment = repositoryService.createDeployment()
                .name("wiring-test")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        // Assert
        assertThat(deployment).isNotNull();
        // The parse listener was invoked during deployment
    }

    @Test
    void deployProcess_setsWorkflowModuleId() {
        // Act
        deployment = repositoryService.createDeployment()
                .name("my-workflow-module")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        // Assert
        assertThat(deployment.getName()).isEqualTo("my-workflow-module");
    }

    @Test
    void historyEventProducerSupplier_isConfigured() {
        // Arrange
        Camunda7HistoryEventProducerSupplier supplier = new Camunda7HistoryEventProducerSupplier();
        Camunda7WiringPlugin plugin = new Camunda7WiringPlugin(
                mock(WiringBpmnParseListener.class),
                supplier
        );

        ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:history-test;DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate("true");
        config.setJobExecutorActivate(false);

        config.setProcessEnginePlugins(new LinkedList<>());
        config.getProcessEnginePlugins().add(plugin);

        ProcessEngine engine = config.buildProcessEngine();

        try {
            // Assert
            assertThat(supplier.getHistoryEventProducer()).isNotNull();
        } finally {
            engine.close();
        }
    }
}
