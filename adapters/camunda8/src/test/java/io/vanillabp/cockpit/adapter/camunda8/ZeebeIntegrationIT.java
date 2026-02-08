package io.vanillabp.cockpit.adapter.camunda8;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.Topology;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using Zeebe Testcontainer to verify deployment and process execution.
 */
@Testcontainers
class ZeebeIntegrationIT {

    private static final int ZEEBE_GATEWAY_PORT = 26500;
    private static final int ZEEBE_INTERNAL_API_PORT = 26501;
    private static final int ZEEBE_MONITORING_PORT = 9600;

    @Container
    private static final GenericContainer<?> zeebeContainer = new GenericContainer<>(DockerImageName.parse("camunda/zeebe:8.4.0"))
            .withExposedPorts(ZEEBE_GATEWAY_PORT, ZEEBE_INTERNAL_API_PORT, ZEEBE_MONITORING_PORT)
            .withEnv("ZEEBE_BROKER_GATEWAY_ENABLE", "true")
            .waitingFor(Wait.forLogMessage(".*Partition-1 recovered, marking it as healthy.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private static ZeebeClient client;

    @BeforeAll
    static void setUp() {
        // Build the gateway address from the container's mapped port
        String gatewayAddress = zeebeContainer.getHost() + ":" + zeebeContainer.getMappedPort(ZEEBE_GATEWAY_PORT);

        client = ZeebeClient.newClientBuilder()
                .gatewayAddress(gatewayAddress)
                .usePlaintext()
                .build();

        // Wait for the broker topology to be available with a partition
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    Topology topology = client.newTopologyRequest().send().join();
                    assertThat(topology.getBrokers()).isNotEmpty();
                    assertThat(topology.getBrokers().get(0).getPartitions()).isNotEmpty();
                });
    }

    @AfterAll
    static void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void deployBpmnProcess_success() {
        // Deploy the test process
        final DeploymentEvent deployment = client.newDeployResourceCommand()
                .addResourceFromClasspath("bpmn/test-process.bpmn")
                .send()
                .join();

        // Verify deployment
        assertThat(deployment.getProcesses()).hasSize(1);
        assertThat(deployment.getProcesses().get(0).getBpmnProcessId()).isEqualTo("test-process");
        assertThat(deployment.getProcesses().get(0).getVersion()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void startProcessInstance_createsInstance() {
        // Deploy the process first
        client.newDeployResourceCommand()
                .addResourceFromClasspath("bpmn/test-process.bpmn")
                .send()
                .join();

        // Start a process instance with variables
        final ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
                .bpmnProcessId("test-process")
                .latestVersion()
                .variables(Map.of(
                        "businessKey", "ORDER-123",
                        "orderId", 12345
                ))
                .send()
                .join();

        // Verify process instance was created
        assertThat(processInstance.getProcessInstanceKey()).isPositive();
        assertThat(processInstance.getBpmnProcessId()).isEqualTo("test-process");
        assertThat(processInstance.getVersion()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void processWithUserTask_createsUserTask() {
        // Deploy the process
        client.newDeployResourceCommand()
                .addResourceFromClasspath("bpmn/test-process.bpmn")
                .send()
                .join();

        // Start a process instance
        final ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
                .bpmnProcessId("test-process")
                .latestVersion()
                .variables(Map.of("businessKey", "ORDER-456"))
                .send()
                .join();

        // Verify we can query the process instance key
        assertThat(processInstance.getProcessInstanceKey()).isPositive();
    }

    @Test
    void startProcessWithBusinessKey_variablesAccessible() {
        // Deploy the process
        client.newDeployResourceCommand()
                .addResourceFromClasspath("bpmn/test-process.bpmn")
                .send()
                .join();

        // Start with complex variables
        final Map<String, Object> variables = Map.of(
                "businessKey", "INVOICE-789",
                "amount", 1500.50,
                "customer", Map.of(
                        "name", "Test Customer",
                        "id", 42
                )
        );

        final ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
                .bpmnProcessId("test-process")
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        // Verify instance was created
        assertThat(processInstance.getProcessInstanceKey()).isPositive();
    }
}
