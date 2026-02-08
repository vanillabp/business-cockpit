package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;

import at.phactum.zeebe.exporters.kafka.serde.RecordId;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.vanillabp.cockpit.adapter.camunda8.deployments.Camunda8DeploymentAdapter;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaControllerTest {

    @Mock
    private Camunda8DeploymentAdapter deploymentAdapter;

    @Mock
    private Camunda8UserTaskEventHandler userTaskEventHandler;

    @Mock
    private Camunda8WorkflowEventHandler workflowEventHandler;

    @Mock
    private ConsumerRecord<RecordId, Record<?>> consumerRecord;

    @Mock
    private Record<JobRecordValue> jobRecord;

    @Mock
    private Record<ProcessInstanceRecordValue> processInstanceRecord;

    @Mock
    private JobRecordValue jobRecordValue;

    @Mock
    private ProcessInstanceRecordValue processInstanceRecordValue;

    private KafkaController controller;

    @BeforeEach
    void setUp() {
        controller = new KafkaController(deploymentAdapter, userTaskEventHandler, workflowEventHandler);
    }

    @Test
    void processInformation_constructor_setsValues() {
        // Create ProcessInformation
        final var info = new KafkaController.ProcessInformation(
                "order-process",
                "v1.0.0",
                3L);

        // Verify values
        assertThat(info.getBpmnProcessId()).isEqualTo("order-process");
        assertThat(info.getVersionTag()).isEqualTo("v1.0.0");
        assertThat(info.getVersion()).isEqualTo(3L);
    }

    @Test
    void consumeUserTaskEvent_withJobValueType_nonEventRecordType_doesNotProcess() {
        // Set up consumer record with JOB value type but COMMAND record type
        doReturn(jobRecord).when(consumerRecord).value();
        doReturn(ValueType.JOB).when(jobRecord).getValueType();
        doReturn(jobRecordValue).when(jobRecord).getValue();
        doReturn(RecordType.COMMAND).when(jobRecord).getRecordType();

        // Consume event
        controller.consumeUserTaskEvent(consumerRecord);

        // Should not process non-event record types
        verify(userTaskEventHandler, never()).processCreatedEvent(any());
    }

    @Test
    void consumeUserTaskEvent_withJobEvent_nonUserTaskType_doesNotProcess() {
        // Set up consumer record for a non-user-task job
        doReturn(jobRecord).when(consumerRecord).value();
        doReturn(ValueType.JOB).when(jobRecord).getValueType();
        doReturn(jobRecordValue).when(jobRecord).getValue();
        doReturn(RecordType.EVENT).when(jobRecord).getRecordType();
        doReturn("some-other-type").when(jobRecordValue).getType();

        // Consume event
        controller.consumeUserTaskEvent(consumerRecord);

        // Should not process non-user-task types
        verify(userTaskEventHandler, never()).processCreatedEvent(any());
    }

    @Test
    void consumeUserTaskEvent_withProcessInstanceType_forNonKafkaProcess_skips() {
        // Set up consumer record for process instance
        doReturn(processInstanceRecord).when(consumerRecord).value();
        doReturn(ValueType.PROCESS_INSTANCE).when(processInstanceRecord).getValueType();
        doReturn(processInstanceRecordValue).when(processInstanceRecord).getValue();
        doReturn(12345L).when(processInstanceRecordValue).getProcessDefinitionKey();
        doReturn(false).when(deploymentAdapter).processDefinitionNeedsKafka(12345L);

        // Consume event
        controller.consumeUserTaskEvent(consumerRecord);

        // Should skip process definitions that don't need Kafka
        verify(workflowEventHandler, never()).processLifecycleEvent(any());
    }

    @Test
    void consumeUserTaskEvent_withProcessInstanceType_nonProcessElement_skips() {
        // Set up consumer record for process instance with non-process element
        doReturn(processInstanceRecord).when(consumerRecord).value();
        doReturn(ValueType.PROCESS_INSTANCE).when(processInstanceRecord).getValueType();
        doReturn(processInstanceRecordValue).when(processInstanceRecord).getValue();
        doReturn(12345L).when(processInstanceRecordValue).getProcessDefinitionKey();
        doReturn(true).when(deploymentAdapter).processDefinitionNeedsKafka(12345L);
        doReturn(ProcessInstanceIntent.ELEMENT_COMPLETED).when(processInstanceRecord).getIntent();
        doReturn(BpmnElementType.SERVICE_TASK).when(processInstanceRecordValue).getBpmnElementType();

        // Consume event
        controller.consumeUserTaskEvent(consumerRecord);

        // Should skip non-process elements
        verify(workflowEventHandler, never()).processLifecycleEvent(any());
    }

}
