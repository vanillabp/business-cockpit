package io.vanillabp.cockpit.adapter.camunda8.wiring;

import io.camunda.zeebe.model.bpmn.instance.Process;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Camunda8UserTaskConnectableTest {

    @Mock
    private Process process;

    private Camunda8UserTaskConnectable connectable;

    @BeforeEach
    void setUp() {
        connectable = new Camunda8UserTaskConnectable(
                "my-module",
                "tenant-1",
                process,
                "v1.0",
                "Activity_ReviewOrder",
                "review-task",
                "Review Order");
    }

    @Test
    void getWorkflowModuleId_returnsConfiguredValue() {
        assertThat(connectable.getWorkflowModuleId()).isEqualTo("my-module");
    }

    @Test
    void getTenantId_returnsConfiguredValue() {
        assertThat(connectable.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void getElementId_returnsConfiguredValue() {
        assertThat(connectable.getElementId()).isEqualTo("Activity_ReviewOrder");
    }

    @Test
    void isExecutableProcess_delegatesToProcess() {
        when(process.isExecutable()).thenReturn(true);
        assertThat(connectable.isExecutableProcess()).isTrue();
    }

    @Test
    void isExecutableProcess_returnsFalse_whenProcessNotExecutable() {
        when(process.isExecutable()).thenReturn(false);
        assertThat(connectable.isExecutableProcess()).isFalse();
    }

    @Test
    void getBpmnProcessId_delegatesToProcess() {
        when(process.getId()).thenReturn("order-process");
        assertThat(connectable.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void getBpmnProcessName_delegatesToProcess() {
        when(process.getName()).thenReturn("Order Process");
        assertThat(connectable.getBpmnProcessName()).isEqualTo("Order Process");
    }

    @Test
    void getVersionInfo_returnsConfiguredValue() {
        assertThat(connectable.getVersionInfo()).isEqualTo("v1.0");
    }

    @Test
    void updateVersionInfo_updatesValue() {
        connectable.updateVersionInfo("v2.0");
        assertThat(connectable.getVersionInfo()).isEqualTo("v2.0");
    }

    @Test
    void getTaskDefinition_returnsConfiguredValue() {
        assertThat(connectable.getTaskDefinition()).isEqualTo("review-task");
    }

    @Test
    void getTitle_returnsConfiguredValue() {
        assertThat(connectable.getTitle()).isEqualTo("Review Order");
    }

}
