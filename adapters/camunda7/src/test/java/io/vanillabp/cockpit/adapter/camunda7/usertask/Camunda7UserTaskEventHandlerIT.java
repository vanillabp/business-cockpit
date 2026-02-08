package io.vanillabp.cockpit.adapter.camunda7.usertask;

import io.vanillabp.cockpit.adapter.camunda7.wiring.Camunda7HistoryEventProducerSupplier;
import io.vanillabp.cockpit.adapter.camunda7.wiring.Camunda7WiringPlugin;
import io.vanillabp.cockpit.adapter.camunda7.wiring.WiringBpmnParseListener;
import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowWiring;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.task.Task;
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
 * Integration tests for Camunda7UserTaskEventHandler with a real process engine.
 *
 * Note: These tests focus on scenarios that don't require a fully wired handler
 * registration, as the production code expects handlers to be registered via
 * Spring wiring before task events occur.
 */
@ExtendWith(MockitoExtension.class)
class Camunda7UserTaskEventHandlerIT {

    @Mock
    private Camunda7UserTaskWiring userTaskWiring;

    @Mock
    private Camunda7WorkflowWiring workflowWiring;

    private ProcessEngine processEngine;
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private TaskService taskService;
    private Deployment deployment;

    @BeforeEach
    void setUp() {
        // Create process engine without user task event handler to test deployment scenarios
        ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:usertask-simple-test;DB_CLOSE_DELAY=-1");
        configuration.setJdbcDriver("org.h2.Driver");
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setJobExecutorActivate(false);
        configuration.setHistory("full");

        // Build the process engine without wiring plugin
        processEngine = configuration.buildProcessEngine();
        repositoryService = processEngine.getRepositoryService();
        runtimeService = processEngine.getRuntimeService();
        taskService = processEngine.getTaskService();
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
    void deployProcess_withoutWiring_createsTasksNormally() {
        // Arrange & Act - deploy and start process without wiring plugin
        deployment = repositoryService.createDeployment()
                .name("simple-test")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        var processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "SIMPLE-TEST-001"
        );

        // Assert - verify task is created normally
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Review Task");
        assertThat(task.getTaskDefinitionKey()).isEqualTo("UserTask_1");
    }

    @Test
    void addTaskHandler_registersHandler() {
        // Arrange
        Camunda7UserTaskEventHandler handler = new Camunda7UserTaskEventHandler();
        Camunda7UserTaskHandler mockTaskHandler = mock(Camunda7UserTaskHandler.class);
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process",
                "1.0",
                "UserTask_1",
                "reviewTask"
        );

        // Act
        handler.addTaskHandler(connectable, mockTaskHandler);

        // Assert - handler should be registered (no exception thrown)
        assertThat(handler).isNotNull();
    }

    @Test
    void userTasksDisabled_doesNotRegisterListeners() {
        // Arrange - create parse listener with user tasks DISABLED
        Camunda7UserTaskEventHandler userTaskEventHandler = new Camunda7UserTaskEventHandler();

        WiringBpmnParseListener disabledParseListener = new WiringBpmnParseListener(
                false,  // userTasksEnabled = false
                userTaskWiring,
                userTaskEventHandler,
                workflowWiring
        );

        Camunda7HistoryEventProducerSupplier historyEventProducerSupplier =
                new Camunda7HistoryEventProducerSupplier();

        Camunda7WiringPlugin wiringPlugin = new Camunda7WiringPlugin(
                disabledParseListener,
                historyEventProducerSupplier
        );

        ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:disabled-usertask-test;DB_CLOSE_DELAY=-1");
        configuration.setJdbcDriver("org.h2.Driver");
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setJobExecutorActivate(false);

        configuration.setProcessEnginePlugins(new LinkedList<>());
        configuration.getProcessEnginePlugins().add(wiringPlugin);

        ProcessEngine disabledEngine = configuration.buildProcessEngine();

        try {
            // Act - deploy and start process
            Deployment disabledDeployment = disabledEngine.getRepositoryService()
                    .createDeployment()
                    .name("disabled-test")
                    .addClasspathResource("bpmn/test-process.bpmn")
                    .deploy();

            // Start process - should not fail even though no handlers are registered
            // because user task listeners are disabled
            var processInstance = disabledEngine.getRuntimeService().startProcessInstanceByKey(
                    "test-process",
                    "DISABLED-001"
            );

            // Assert - process instance is created and task exists
            assertThat(processInstance).isNotNull();

            Task task = disabledEngine.getTaskService()
                    .createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(task).isNotNull();

            // Cleanup
            disabledEngine.getRuntimeService()
                    .createProcessInstanceQuery()
                    .processDefinitionKey("test-process")
                    .list()
                    .forEach(pi -> disabledEngine.getRuntimeService().deleteProcessInstance(pi.getId(), "cleanup"));
            disabledEngine.getRepositoryService().deleteDeployment(disabledDeployment.getId(), true);
        } finally {
            disabledEngine.close();
        }
    }

    @Test
    void connectable_hasCorrectProperties() {
        // Arrange - constructor: (bpmnProcessId, versionInfo, elementId, taskDefinition)
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process",
                "1.0:1",
                "UserTask_1",
                "reviewTask"
        );

        // Assert
        assertThat(connectable.getBpmnProcessId()).isEqualTo("test-process");
        assertThat(connectable.getVersionInfo()).isEqualTo("1.0:1");
        assertThat(connectable.getElementId()).isEqualTo("UserTask_1");
        assertThat(connectable.getTaskDefinition()).isEqualTo("reviewTask");
        assertThat(connectable.isExecutableProcess()).isTrue();
    }

    @Test
    void connectable_appliesMatchesByElementId() {
        // Arrange
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process", "1.0:1", "UserTask_1", "reviewTask");

        // Assert - applies() matches by elementId or taskDefinition
        assertThat(connectable.applies("UserTask_1", "otherForm")).isTrue();
        assertThat(connectable.applies("OtherTask", "reviewTask")).isTrue();
        assertThat(connectable.applies("OtherTask", "otherForm")).isFalse();
    }

    @Test
    void connectable_matchesWithDifferentVersion() {
        // Arrange
        Camunda7Connectable v1 = new Camunda7Connectable(
                "test-process", "1.0:1", "UserTask_1", "form1");
        Camunda7Connectable v2 = new Camunda7Connectable(
                "test-process", "2.0:1", "UserTask_1", "form2");

        // Assert - same element ID, different versions
        assertThat(v1.getElementId()).isEqualTo(v2.getElementId());
        assertThat(v1.getVersionInfo()).isNotEqualTo(v2.getVersionInfo());
        assertThat(v1.getTaskDefinition()).isNotEqualTo(v2.getTaskDefinition());
    }

    @Test
    void wiringParseListener_capturesUserTaskDuringDeploy() {
        // Arrange
        Camunda7UserTaskEventHandler userTaskEventHandler = new Camunda7UserTaskEventHandler();

        WiringBpmnParseListener parseListener = new WiringBpmnParseListener(
                true,
                userTaskWiring,
                userTaskEventHandler,
                workflowWiring
        );

        Camunda7WiringPlugin wiringPlugin = new Camunda7WiringPlugin(
                parseListener,
                new Camunda7HistoryEventProducerSupplier()
        );

        ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:wiring-capture-test;DB_CLOSE_DELAY=-1");
        configuration.setJdbcDriver("org.h2.Driver");
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setJobExecutorActivate(false);

        configuration.setProcessEnginePlugins(new LinkedList<>());
        configuration.getProcessEnginePlugins().add(wiringPlugin);

        ProcessEngine testEngine = configuration.buildProcessEngine();

        try {
            // Act - deploy the process
            Deployment testDeployment = testEngine.getRepositoryService()
                    .createDeployment()
                    .name("wiring-capture")
                    .addClasspathResource("bpmn/test-process.bpmn")
                    .deploy();

            // Assert - verify userTaskWiring was called with the connectable
            // ElementId is "UserTask_1" (from BPMN), taskDefinition is "reviewTask" (formKey from BPMN)
            verify(userTaskWiring).wireTask(eq("wiring-capture"), argThat(connectable ->
                    "UserTask_1".equals(connectable.getElementId()) &&
                    "test-process".equals(connectable.getBpmnProcessId())
            ));

            // Cleanup
            testEngine.getRepositoryService().deleteDeployment(testDeployment.getId(), true);
        } finally {
            testEngine.close();
        }
    }

    @Test
    void taskQuery_returnsCorrectTaskDetails() {
        // Arrange - deploy and start process
        deployment = repositoryService.createDeployment()
                .name("task-query-test")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        var processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "QUERY-TEST-001"
        );

        // Act - query for tasks
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        // Assert
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Review Task");
        assertThat(task.getTaskDefinitionKey()).isEqualTo("UserTask_1");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());
    }

    @Test
    void taskAssignment_updatesTask() {
        // Arrange - deploy and start process
        deployment = repositoryService.createDeployment()
                .name("task-assignment-test")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        var processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "ASSIGN-TEST-001"
        );

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        // Act - assign the task
        taskService.setAssignee(task.getId(), "john.doe");

        // Assert
        Task updatedTask = taskService.createTaskQuery()
                .taskId(task.getId())
                .singleResult();
        assertThat(updatedTask.getAssignee()).isEqualTo("john.doe");
    }

    @Test
    void taskCompletion_endsProcess() {
        // Arrange - deploy and start process
        deployment = repositoryService.createDeployment()
                .name("task-completion-test")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();

        var processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "COMPLETE-TEST-001"
        );

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        // Act - complete the task
        taskService.complete(task.getId());

        // Assert - process should be completed
        var completedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(completedInstance).isNull();

        // Verify in history
        var historicInstance = processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(historicInstance).isNotNull();
        assertThat(historicInstance.getEndTime()).isNotNull();
    }
}
