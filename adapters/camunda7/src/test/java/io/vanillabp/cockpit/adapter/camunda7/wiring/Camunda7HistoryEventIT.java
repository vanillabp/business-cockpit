package io.vanillabp.cockpit.adapter.camunda7.wiring;

import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowWiring;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.camunda.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
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
 * Integration tests for Camunda7 history event handling with a real process engine.
 */
@ExtendWith(MockitoExtension.class)
class Camunda7HistoryEventIT {

    @Mock
    private Camunda7UserTaskWiring userTaskWiring;

    @Mock
    private Camunda7WorkflowWiring workflowWiring;

    @Mock
    private Camunda7UserTaskEventHandler userTaskEventHandler;

    private ProcessEngine processEngine;
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private Deployment deployment;
    private Camunda7HistoryEventProducerSupplier historyEventProducerSupplier;

    @BeforeEach
    void setUp() {
        // Create the parse listener with user tasks enabled
        WiringBpmnParseListener parseListener = new WiringBpmnParseListener(
                true,
                userTaskWiring,
                userTaskEventHandler,
                workflowWiring
        );

        // Create history event producer supplier
        historyEventProducerSupplier = new Camunda7HistoryEventProducerSupplier();

        // Create the wiring plugin
        Camunda7WiringPlugin wiringPlugin = new Camunda7WiringPlugin(
                parseListener,
                historyEventProducerSupplier
        );

        // Configure process engine with the plugin and full history
        ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:history-event-test;DB_CLOSE_DELAY=-1");
        configuration.setJdbcDriver("org.h2.Driver");
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setJobExecutorActivate(false);
        configuration.setHistory("full");
        configuration.setHistoryLevel(HistoryLevel.HISTORY_LEVEL_FULL);

        // Register the plugin
        configuration.setProcessEnginePlugins(new LinkedList<>());
        configuration.getProcessEnginePlugins().add(wiringPlugin);

        // Build the process engine
        processEngine = configuration.buildProcessEngine();
        repositoryService = processEngine.getRepositoryService();
        runtimeService = processEngine.getRuntimeService();
    }

    @AfterEach
    void tearDown() {
        if (deployment != null) {
            // Clean up process instances first
            runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey("test-process")
                    .list()
                    .forEach(pi -> runtimeService.deleteProcessInstance(pi.getId(), "cleanup"));
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
        if (processEngine != null) {
            processEngine.close();
        }
    }

    @Test
    void historyEventProducerSupplier_capturesProducerDuringEngineInit() {
        // Assert - the supplier should have captured the history event producer
        HistoryEventProducer producer = historyEventProducerSupplier.getHistoryEventProducer();
        assertThat(producer).isNotNull();
    }

    @Test
    void deployAndStartProcess_createsHistoryEntries() {
        // Arrange - deploy the process
        deployment = repositoryService.createDeployment()
                .name("history-test")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        // Act - start a process instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "HISTORY-TEST-001"
        );

        // Assert - verify history entry is created
        HistoricProcessInstance historicInstance = processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(historicInstance).isNotNull();
        assertThat(historicInstance.getBusinessKey()).isEqualTo("HISTORY-TEST-001");
        assertThat(historicInstance.getProcessDefinitionKey()).isEqualTo("test-process");
        assertThat(historicInstance.getStartTime()).isNotNull();
    }

    @Test
    void startProcess_triggersWiringDuringDeployment() {
        // Arrange - deploy the process with deployment name
        deployment = repositoryService.createDeployment()
                .name("wiring-module")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        // Assert - verify wiring methods were called during deployment
        // The deployment name is used as workflow module ID when ThreadLocal is set
        verify(workflowWiring).wireService(eq("wiring-module"), eq("test-process"));
        verify(workflowWiring).wireWorkflow(eq("wiring-module"), eq("test-process"));
        verify(userTaskWiring).wireTask(eq("wiring-module"), any());
    }

    @Test
    void deployWithWorkflowModuleId_passesModuleIdToWiring() {
        // Arrange - set workflow module ID using ThreadLocal
        WiringBpmnParseListener.workflowModuleId.set("my-workflow-module");

        try {
            // Act - deploy the process
            deployment = repositoryService.createDeployment()
                    .name("my-workflow-module")
                    .addClasspathResource("bpmn/test-process.bpmn")
                    .deploy();

            // Assert - verify wiring received the module ID
            verify(workflowWiring).wireService(eq("my-workflow-module"), eq("test-process"));
            verify(workflowWiring).wireWorkflow(eq("my-workflow-module"), eq("test-process"));
            verify(userTaskWiring).wireTask(eq("my-workflow-module"), any());
        } finally {
            WiringBpmnParseListener.workflowModuleId.remove();
        }
    }

    @Test
    void completeProcess_createsEndHistoryEntry() {
        // Arrange - deploy and start process
        deployment = repositoryService.createDeployment()
                .name("complete-test")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "COMPLETE-TEST-001"
        );

        // Act - complete the user task to end the process
        var task = processEngine.getTaskService()
                .createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        processEngine.getTaskService().complete(task.getId());

        // Assert - verify the process is completed in history
        HistoricProcessInstance historicInstance = processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(historicInstance).isNotNull();
        assertThat(historicInstance.getEndTime()).isNotNull();
        assertThat(historicInstance.getState()).isEqualTo("COMPLETED");
    }

    @Test
    void cancelProcess_createsEndHistoryEntryWithDeleteReason() {
        // Arrange - deploy and start process
        deployment = repositoryService.createDeployment()
                .name("cancel-test")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "CANCEL-TEST-001"
        );

        // Act - delete/cancel the process instance
        runtimeService.deleteProcessInstance(processInstance.getId(), "Cancelled by test");

        // Assert - verify the process is cancelled in history with reason
        HistoricProcessInstance historicInstance = processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(historicInstance).isNotNull();
        assertThat(historicInstance.getEndTime()).isNotNull();
        assertThat(historicInstance.getDeleteReason()).isEqualTo("Cancelled by test");
    }

    @Test
    void historyEventProducerSupplier_canBeSetExternally() {
        // Arrange
        Camunda7HistoryEventProducerSupplier supplier = new Camunda7HistoryEventProducerSupplier();
        HistoryEventProducer mockProducer = mock(HistoryEventProducer.class);

        // Act
        supplier.setHistoryEventProducer(mockProducer);

        // Assert
        assertThat(supplier.getHistoryEventProducer()).isSameAs(mockProducer);
    }
}
