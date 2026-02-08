package io.vanillabp.cockpit.adapter.camunda7;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.junit5.ProcessEngineExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using an embedded Camunda 7 process engine.
 */
@ExtendWith(ProcessEngineExtension.class)
class Camunda7ProcessEngineIT {

    private ProcessEngine processEngine;
    private RuntimeService runtimeService;
    private TaskService taskService;
    private RepositoryService repositoryService;
    private HistoryService historyService;

    private Deployment deployment;

    @BeforeEach
    void setUp(ProcessEngine processEngine) {
        this.processEngine = processEngine;
        this.runtimeService = processEngine.getRuntimeService();
        this.taskService = processEngine.getTaskService();
        this.repositoryService = processEngine.getRepositoryService();
        this.historyService = processEngine.getHistoryService();

        // Deploy the test process
        deployment = repositoryService.createDeployment()
                .name("test-deployment")
                .addClasspathResource("bpmn/test-process.bpmn")
                .deploy();
    }

    @AfterEach
    void tearDown() {
        // Clean up
        if (deployment != null) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    void deployProcess_success() {
        // Assert
        assertThat(deployment).isNotNull();
        assertThat(deployment.getId()).isNotNull();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        assertThat(processDefinition).isNotNull();
        assertThat(processDefinition.getKey()).isEqualTo("test-process");
        assertThat(processDefinition.getVersionTag()).isEqualTo("1.0");
    }

    @Test
    void startProcessInstance_createsInstance() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", "ORDER-123");

        // Act
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "ORDER-123",
                variables
        );

        // Assert
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessInstanceId()).isNotNull();
        assertThat(processInstance.getBusinessKey()).isEqualTo("ORDER-123");
    }

    @Test
    void startProcessInstance_createsUserTask() {
        // Arrange
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "ORDER-456"
        );

        // Act
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .list();

        // Assert
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("Review Task");
        assertThat(task.getTaskDefinitionKey()).isEqualTo("UserTask_1");
    }

    @Test
    void completeUserTask_completesProcess() {
        // Arrange
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "ORDER-789"
        );

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        // Act
        taskService.complete(task.getId());

        // Assert - process should be completed
        ProcessInstance completedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(completedInstance).isNull();

        // Verify in history
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(historicInstance).isNotNull();
        assertThat(historicInstance.getEndTime()).isNotNull();
    }

    @Test
    void queryProcessInstanceByBusinessKey_returnsInstance() {
        // Arrange
        runtimeService.startProcessInstanceByKey("test-process", "QUERY-TEST-001");

        // Act
        ProcessInstance result = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey("QUERY-TEST-001")
                .singleResult();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessKey()).isEqualTo("QUERY-TEST-001");
    }

    @Test
    void assignTask_setsAssignee() {
        // Arrange
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "ASSIGN-TEST"
        );
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        // Act
        taskService.setAssignee(task.getId(), "john.doe");

        // Assert
        Task updatedTask = taskService.createTaskQuery()
                .taskId(task.getId())
                .singleResult();
        assertThat(updatedTask.getAssignee()).isEqualTo("john.doe");
    }

    @Test
    void getProcessDefinition_returnsVersionInfo() {
        // Arrange
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("test-process")
                .singleResult();

        // Assert
        assertThat(processDefinition.getVersionTag()).isEqualTo("1.0");
        assertThat(processDefinition.getVersion()).isEqualTo(1);
    }

    @Test
    void setProcessVariables_storesVariables() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", 12345);
        variables.put("amount", 99.99);
        variables.put("customer", "Test Customer");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "test-process",
                "VARS-TEST",
                variables
        );

        // Act
        Object orderId = runtimeService.getVariable(processInstance.getId(), "orderId");
        Object amount = runtimeService.getVariable(processInstance.getId(), "amount");
        Object customer = runtimeService.getVariable(processInstance.getId(), "customer");

        // Assert
        assertThat(orderId).isEqualTo(12345);
        assertThat(amount).isEqualTo(99.99);
        assertThat(customer).isEqualTo("Test Customer");
    }
}
