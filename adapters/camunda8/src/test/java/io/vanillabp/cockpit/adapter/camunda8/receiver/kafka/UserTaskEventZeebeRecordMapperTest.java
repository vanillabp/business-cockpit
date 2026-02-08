package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTaskEventZeebeRecordMapperTest {

    @Mock
    private JobRecordValue jobRecord;

    @Mock
    private Record<?> record;

    // --- mapToUserTaskCreatedInformation ---

    @Test
    void mapToUserTaskCreatedInformation_mapsAllBasicFields() {
        // Prepare job record with basic fields
        when(jobRecord.getProcessDefinitionKey()).thenReturn(2251799813685249L);
        when(jobRecord.getBpmnProcessId()).thenReturn("order-process");
        when(jobRecord.getTenantId()).thenReturn("tenant-1");
        when(jobRecord.getElementId()).thenReturn("Activity_ReviewOrder");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2251799813685255L);
        when(jobRecord.getElementInstanceKey()).thenReturn(2251799813685260L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(3);
        when(jobRecord.getVariables()).thenReturn(Map.of("orderId", "ORD-123"));
        when(jobRecord.getCustomHeaders()).thenReturn(null);

        // Map to user task created event
        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskCreatedInformation(jobRecord);

        // Verify all fields are mapped correctly
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
        assertThat(event.getElementId()).isEqualTo("Activity_ReviewOrder");
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
        assertThat(event.getElementInstanceKey()).isEqualTo(2251799813685260L);
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(3);
        assertThat(event.getVariables()).containsEntry("orderId", "ORD-123");
    }

    @Test
    void mapToUserTaskCreatedInformation_withFormKeyHeader_mapsFormKey() {
        // Prepare job record with form key in custom headers
        when(jobRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(jobRecord.getBpmnProcessId()).thenReturn("process");
        when(jobRecord.getTenantId()).thenReturn("default");
        when(jobRecord.getElementId()).thenReturn("task1");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2L);
        when(jobRecord.getElementInstanceKey()).thenReturn(3L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(1);
        when(jobRecord.getVariables()).thenReturn(Map.of());

        // Custom headers with form key
        final var customHeaders = new HashMap<String, String>();
        customHeaders.put("io.camunda.zeebe:formKey", "camunda-forms:bpmn:userTaskForm_1");
        when(jobRecord.getCustomHeaders()).thenReturn(customHeaders);

        // Map to user task created event
        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskCreatedInformation(jobRecord);

        // Verify form key is mapped
        assertThat(event.getFormKey()).isEqualTo("camunda-forms:bpmn:userTaskForm_1");
    }

    @Test
    void mapToUserTaskCreatedInformation_withAssigneeHeader_mapsAssignee() {
        // Prepare job record with assignee in custom headers
        when(jobRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(jobRecord.getBpmnProcessId()).thenReturn("process");
        when(jobRecord.getTenantId()).thenReturn("default");
        when(jobRecord.getElementId()).thenReturn("task1");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2L);
        when(jobRecord.getElementInstanceKey()).thenReturn(3L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(1);
        when(jobRecord.getVariables()).thenReturn(Map.of());

        // Custom headers with assignee
        final var customHeaders = new HashMap<String, String>();
        customHeaders.put("io.camunda.zeebe:assignee", "john.doe");
        when(jobRecord.getCustomHeaders()).thenReturn(customHeaders);

        // Map to user task created event
        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskCreatedInformation(jobRecord);

        // Verify assignee is mapped
        assertThat(event.getAssignee()).isEqualTo("john.doe");
    }

    @Test
    void mapToUserTaskCreatedInformation_withNullCustomHeaders_handlesGracefully() {
        // Prepare job record without custom headers
        when(jobRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(jobRecord.getBpmnProcessId()).thenReturn("process");
        when(jobRecord.getTenantId()).thenReturn("default");
        when(jobRecord.getElementId()).thenReturn("task1");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2L);
        when(jobRecord.getElementInstanceKey()).thenReturn(3L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(1);
        when(jobRecord.getVariables()).thenReturn(Map.of());
        when(jobRecord.getCustomHeaders()).thenReturn(null);

        // Map should not throw exception
        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskCreatedInformation(jobRecord);

        // Verify event is created
        assertThat(event).isNotNull();
        assertThat(event.getFormKey()).isNull();
        assertThat(event.getAssignee()).isNull();
    }

    // --- addMetaData for UserTaskCreatedEvent ---

    @Test
    void addMetaData_toUserTaskCreatedEvent_setsKeyAndTimestamp() {
        // Prepare event and record
        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskCreatedInformation(createMinimalJobRecord());
        when(record.getKey()).thenReturn(12345L);
        when(record.getTimestamp()).thenReturn(1704067200000L);

        // Add metadata
        UserTaskEventZeebeRecordMapper.addMetaData(event, record);

        // Verify metadata is set
        assertThat(event.getKey()).isEqualTo(12345L);
        assertThat(event.getTimestamp()).isEqualTo(1704067200000L);
    }

    // --- mapToUserTaskLifecycleInformation ---

    @Test
    void mapToUserTaskLifecycleInformation_mapsAllBasicFields() {
        // Prepare job record with basic fields
        when(jobRecord.getProcessDefinitionKey()).thenReturn(2251799813685249L);
        when(jobRecord.getBpmnProcessId()).thenReturn("order-process");
        when(jobRecord.getTenantId()).thenReturn("tenant-1");
        when(jobRecord.getElementId()).thenReturn("Activity_ReviewOrder");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2251799813685255L);
        when(jobRecord.getElementInstanceKey()).thenReturn(2251799813685260L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(3);
        when(jobRecord.getCustomHeaders()).thenReturn(null);

        // Map to lifecycle event
        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskLifecycleInformation(jobRecord);

        // Verify all fields are mapped correctly
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
        assertThat(event.getElementId()).isEqualTo("Activity_ReviewOrder");
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
        assertThat(event.getElementInstanceKey()).isEqualTo(2251799813685260L);
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(3);
    }

    @Test
    void mapToUserTaskLifecycleInformation_withFormKeyHeader_mapsFormKey() {
        // Prepare job record with form key
        when(jobRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(jobRecord.getBpmnProcessId()).thenReturn("process");
        when(jobRecord.getTenantId()).thenReturn("default");
        when(jobRecord.getElementId()).thenReturn("task1");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2L);
        when(jobRecord.getElementInstanceKey()).thenReturn(3L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(1);

        final var customHeaders = new HashMap<String, String>();
        customHeaders.put("io.camunda.zeebe:formKey", "embedded:deployment:form.html");
        when(jobRecord.getCustomHeaders()).thenReturn(customHeaders);

        // Map to lifecycle event
        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskLifecycleInformation(jobRecord);

        // Verify form key is mapped
        assertThat(event.getFormKey()).isEqualTo("embedded:deployment:form.html");
    }

    // --- addMetaData for UserTaskLifecycleEvent ---

    @Test
    void addMetaData_toUserTaskLifecycleEvent_setsKeyTimestampAndIntent() {
        // Prepare lifecycle event
        when(jobRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(jobRecord.getBpmnProcessId()).thenReturn("process");
        when(jobRecord.getTenantId()).thenReturn("default");
        when(jobRecord.getElementId()).thenReturn("task1");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2L);
        when(jobRecord.getElementInstanceKey()).thenReturn(3L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(1);
        when(jobRecord.getCustomHeaders()).thenReturn(null);

        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskLifecycleInformation(jobRecord);

        // Mock record with COMPLETED intent
        when(record.getKey()).thenReturn(12345L);
        when(record.getTimestamp()).thenReturn(1704067200000L);
        final var completedIntent = mock(Intent.class);
        when(completedIntent.name()).thenReturn("COMPLETED");
        when(record.getIntent()).thenReturn(completedIntent);

        // Add metadata
        UserTaskEventZeebeRecordMapper.addMetaData(event, record);

        // Verify metadata is set
        assertThat(event.getKey()).isEqualTo(12345L);
        assertThat(event.getTimestamp()).isEqualTo(1704067200000L);
        assertThat(event.getIntent()).isEqualTo(Camunda8UserTaskLifecycleEvent.Intent.COMPLETED);
    }

    @Test
    void addMetaData_toUserTaskLifecycleEvent_withCanceledIntent() {
        // Prepare lifecycle event
        when(jobRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(jobRecord.getBpmnProcessId()).thenReturn("process");
        when(jobRecord.getTenantId()).thenReturn("default");
        when(jobRecord.getElementId()).thenReturn("task1");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2L);
        when(jobRecord.getElementInstanceKey()).thenReturn(3L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(1);
        when(jobRecord.getCustomHeaders()).thenReturn(null);

        final var event = UserTaskEventZeebeRecordMapper.mapToUserTaskLifecycleInformation(jobRecord);

        // Mock record with CANCELED intent
        when(record.getKey()).thenReturn(12345L);
        when(record.getTimestamp()).thenReturn(1704067200000L);
        final var canceledIntent = mock(Intent.class);
        when(canceledIntent.name()).thenReturn("CANCELED");
        when(record.getIntent()).thenReturn(canceledIntent);

        // Add metadata
        UserTaskEventZeebeRecordMapper.addMetaData(event, record);

        // Verify intent is CANCELED
        assertThat(event.getIntent()).isEqualTo(Camunda8UserTaskLifecycleEvent.Intent.CANCELED);
    }

    // --- Helper methods ---

    private JobRecordValue createMinimalJobRecord() {
        when(jobRecord.getProcessDefinitionKey()).thenReturn(1L);
        when(jobRecord.getBpmnProcessId()).thenReturn("process");
        when(jobRecord.getTenantId()).thenReturn("default");
        when(jobRecord.getElementId()).thenReturn("task1");
        when(jobRecord.getProcessInstanceKey()).thenReturn(2L);
        when(jobRecord.getElementInstanceKey()).thenReturn(3L);
        when(jobRecord.getProcessDefinitionVersion()).thenReturn(1);
        when(jobRecord.getVariables()).thenReturn(Map.of());
        when(jobRecord.getCustomHeaders()).thenReturn(null);
        return jobRecord;
    }

}
