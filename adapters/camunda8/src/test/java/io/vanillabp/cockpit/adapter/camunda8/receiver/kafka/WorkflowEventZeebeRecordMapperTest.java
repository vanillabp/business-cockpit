package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.value.ProcessEventRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowEventZeebeRecordMapperTest {

    @Mock
    private ProcessInstanceCreationRecordValue processInstanceCreationRecord;

    @Mock
    private ProcessEventRecordValue processEventRecord;

    @Mock
    private ProcessInstanceRecordValue processInstanceRecord;

    @Mock
    private Record<?> record;

    // --- map (ProcessInstanceCreationRecordValue) ---

    @Test
    void map_processInstanceCreationRecord_mapsAllFields() {
        // Prepare process instance creation record
        when(processInstanceCreationRecord.getTenantId()).thenReturn("tenant-1");
        when(processInstanceCreationRecord.getProcessDefinitionKey()).thenReturn(2251799813685249L);
        when(processInstanceCreationRecord.getProcessInstanceKey()).thenReturn(2251799813685255L);
        when(processInstanceCreationRecord.getBpmnProcessId()).thenReturn("order-process");

        // Prepare process information with version tag
        final var processInfo = new KafkaController.ProcessInformation("order-process", "v1.0", 3);

        // Map to workflow created event
        final var event = WorkflowEventZeebeRecordMapper.map(processInstanceCreationRecord, processInfo);

        // Verify all fields are mapped correctly
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
        assertThat(event.getVersion()).isEqualTo("v1.0:3");
    }

    @Test
    void map_processInstanceCreationRecord_withoutVersionTag_usesVersionOnly() {
        // Prepare process instance creation record
        when(processInstanceCreationRecord.getTenantId()).thenReturn("tenant-1");
        when(processInstanceCreationRecord.getProcessDefinitionKey()).thenReturn(2251799813685249L);
        when(processInstanceCreationRecord.getProcessInstanceKey()).thenReturn(2251799813685255L);
        when(processInstanceCreationRecord.getBpmnProcessId()).thenReturn("order-process");

        // Prepare process information without version tag
        final var processInfo = new KafkaController.ProcessInformation("order-process", null, 5);

        // Map to workflow created event
        final var event = WorkflowEventZeebeRecordMapper.map(processInstanceCreationRecord, processInfo);

        // Verify version format without tag
        assertThat(event.getVersion()).isEqualTo("5");
    }

    // --- map (ProcessEventRecordValue) ---

    @Test
    void map_processEventRecord_mapsAllFields() {
        // Prepare process event record
        when(processEventRecord.getTenantId()).thenReturn("tenant-2");
        when(processEventRecord.getProcessDefinitionKey()).thenReturn(2251799813685300L);
        when(processEventRecord.getProcessInstanceKey()).thenReturn(2251799813685310L);

        // Prepare process information with version tag
        final var processInfo = new KafkaController.ProcessInformation("payment-process", "release-2.0", 7);

        // Map to workflow created event
        final var event = WorkflowEventZeebeRecordMapper.map(processEventRecord, processInfo);

        // Verify all fields are mapped correctly
        assertThat(event.getTenantId()).isEqualTo("tenant-2");
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685300L);
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685310L);
        assertThat(event.getBpmnProcessId()).isEqualTo("payment-process");
        assertThat(event.getVersion()).isEqualTo("release-2.0:7");
    }

    @Test
    void map_processEventRecord_withoutVersionTag_usesVersionOnly() {
        // Prepare process event record
        when(processEventRecord.getTenantId()).thenReturn("default");
        when(processEventRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(processEventRecord.getProcessInstanceKey()).thenReturn(2L);

        // Prepare process information without version tag
        final var processInfo = new KafkaController.ProcessInformation("simple-process", null, 1);

        // Map to workflow created event
        final var event = WorkflowEventZeebeRecordMapper.map(processEventRecord, processInfo);

        // Verify version format without tag
        assertThat(event.getVersion()).isEqualTo("1");
    }

    // --- addMetaData (for WorkflowCreatedEvent) ---

    @Test
    void addMetaData_toWorkflowCreatedEvent_setsKeyAndTimestamp() {
        // Prepare workflow created event
        when(processInstanceCreationRecord.getTenantId()).thenReturn("tenant-1");
        when(processInstanceCreationRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(processInstanceCreationRecord.getProcessInstanceKey()).thenReturn(2L);
        when(processInstanceCreationRecord.getBpmnProcessId()).thenReturn("process");
        final var processInfo = new KafkaController.ProcessInformation("process", null, 1);
        final var event = WorkflowEventZeebeRecordMapper.map(processInstanceCreationRecord, processInfo);

        // Prepare record with key and timestamp
        when(record.getKey()).thenReturn(12345L);
        when(record.getTimestamp()).thenReturn(1704067200000L);

        // Add metadata
        WorkflowEventZeebeRecordMapper.addMetaData(event, record);

        // Verify metadata is set
        assertThat(event.getKey()).isEqualTo(12345L);
        assertThat(event.getTimestamp()).isEqualTo(1704067200000L);
    }

    // --- map (ProcessInstanceRecordValue) ---

    @Test
    void map_processInstanceRecord_mapsAllFields() {
        // Prepare process instance record
        when(processInstanceRecord.getProcessInstanceKey()).thenReturn(2251799813685255L);
        when(processInstanceRecord.getProcessDefinitionKey()).thenReturn(2251799813685249L);
        when(processInstanceRecord.getBpmnProcessId()).thenReturn("order-process");
        when(processInstanceRecord.getVersion()).thenReturn(3);
        when(processInstanceRecord.getTenantId()).thenReturn("tenant-1");

        // Map to workflow lifecycle event
        final var event = WorkflowEventZeebeRecordMapper.map(processInstanceRecord);

        // Verify all fields are mapped correctly
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(3);
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
    }

    // --- addMetaData (for WorkflowLifeCycleEvent) ---

    @Test
    void addMetaData_toWorkflowLifeCycleEvent_setsKeyTimestampAndIntent_completed() {
        // Prepare workflow lifecycle event
        when(processInstanceRecord.getProcessInstanceKey()).thenReturn(1L);
        when(processInstanceRecord.getProcessDefinitionKey()).thenReturn(2L);
        when(processInstanceRecord.getBpmnProcessId()).thenReturn("process");
        when(processInstanceRecord.getVersion()).thenReturn(1);
        when(processInstanceRecord.getTenantId()).thenReturn("default");
        final var event = WorkflowEventZeebeRecordMapper.map(processInstanceRecord);

        // Prepare record with ELEMENT_COMPLETED intent
        when(record.getKey()).thenReturn(12345L);
        when(record.getTimestamp()).thenReturn(1704067200000L);
        final var completedIntent = mock(Intent.class);
        when(completedIntent.name()).thenReturn("ELEMENT_COMPLETED");
        when(record.getIntent()).thenReturn(completedIntent);

        // Add metadata
        WorkflowEventZeebeRecordMapper.addMetaData(event, record);

        // Verify metadata is set
        assertThat(event.getKey()).isEqualTo(12345L);
        assertThat(event.getTimestamp()).isEqualTo(1704067200000L);
        assertThat(event.getIntent()).isEqualTo(Camunda8WorkflowLifeCycleEvent.Intent.COMPLETED);
    }

    @Test
    void addMetaData_toWorkflowLifeCycleEvent_setsKeyTimestampAndIntent_terminated() {
        // Prepare workflow lifecycle event
        when(processInstanceRecord.getProcessInstanceKey()).thenReturn(1L);
        when(processInstanceRecord.getProcessDefinitionKey()).thenReturn(2L);
        when(processInstanceRecord.getBpmnProcessId()).thenReturn("process");
        when(processInstanceRecord.getVersion()).thenReturn(1);
        when(processInstanceRecord.getTenantId()).thenReturn("default");
        final var event = WorkflowEventZeebeRecordMapper.map(processInstanceRecord);

        // Prepare record with ELEMENT_TERMINATED intent
        when(record.getKey()).thenReturn(12345L);
        when(record.getTimestamp()).thenReturn(1704067200000L);
        final var terminatedIntent = mock(Intent.class);
        when(terminatedIntent.name()).thenReturn("ELEMENT_TERMINATED");
        when(record.getIntent()).thenReturn(terminatedIntent);

        // Add metadata
        WorkflowEventZeebeRecordMapper.addMetaData(event, record);

        // Verify intent is CANCELLED
        assertThat(event.getIntent()).isEqualTo(Camunda8WorkflowLifeCycleEvent.Intent.CANCELLED);
    }

    @Test
    void addMetaData_toWorkflowLifeCycleEvent_withUnknownIntent_setsIntentToNull() {
        // Prepare workflow lifecycle event
        when(processInstanceRecord.getProcessInstanceKey()).thenReturn(1L);
        when(processInstanceRecord.getProcessDefinitionKey()).thenReturn(2L);
        when(processInstanceRecord.getBpmnProcessId()).thenReturn("process");
        when(processInstanceRecord.getVersion()).thenReturn(1);
        when(processInstanceRecord.getTenantId()).thenReturn("default");
        final var event = WorkflowEventZeebeRecordMapper.map(processInstanceRecord);

        // Prepare record with unknown intent
        when(record.getKey()).thenReturn(12345L);
        when(record.getTimestamp()).thenReturn(1704067200000L);
        final var unknownIntent = mock(Intent.class);
        when(unknownIntent.name()).thenReturn("ELEMENT_ACTIVATING");
        when(record.getIntent()).thenReturn(unknownIntent);

        // Add metadata
        WorkflowEventZeebeRecordMapper.addMetaData(event, record);

        // Verify intent is null for unknown intent
        assertThat(event.getIntent()).isNull();
    }

    // --- ProcessInformation ---

    @Test
    void processInformation_storesAndReturnsValues() {
        // Create process information
        final var processInfo = new KafkaController.ProcessInformation("my-process", "v2.1", 5);

        // Verify all values are accessible
        assertThat(processInfo.getBpmnProcessId()).isEqualTo("my-process");
        assertThat(processInfo.getVersionTag()).isEqualTo("v2.1");
        assertThat(processInfo.getVersion()).isEqualTo(5);
    }

    @Test
    void processInformation_withNullVersionTag() {
        // Create process information without version tag
        final var processInfo = new KafkaController.ProcessInformation("simple-process", null, 1);

        // Verify version tag is null
        assertThat(processInfo.getBpmnProcessId()).isEqualTo("simple-process");
        assertThat(processInfo.getVersionTag()).isNull();
        assertThat(processInfo.getVersion()).isEqualTo(1);
    }

}
