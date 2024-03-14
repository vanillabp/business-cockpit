package io.vanillabp.cockpit.adapter.camunda8.receiver.redis;

import com.google.protobuf.Value;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.zeebe.exporter.proto.Schema;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserTaskEventProtobufMapper {

    public static Camunda8UserTaskCreatedEvent mapToUserTaskCreatedInformation(Schema.JobRecord task){
        Camunda8UserTaskCreatedEvent userTaskCreatedEvent = new Camunda8UserTaskCreatedEvent();

        userTaskCreatedEvent.setKey(task.getMetadata().getKey());
        userTaskCreatedEvent.setTimestamp(task.getMetadata().getTimestamp());

        userTaskCreatedEvent.setProcessDefinitionKey(task.getProcessDefinitionKey());
        userTaskCreatedEvent.setBpmnProcessId(task.getBpmnProcessId());
        userTaskCreatedEvent.setElementId(task.getElementId());

        userTaskCreatedEvent.setProcessInstanceKey(task.getProcessInstanceKey());
        userTaskCreatedEvent.setElementInstanceKey(task.getElementInstanceKey());
        userTaskCreatedEvent.setWorkflowDefinitionVersion(task.getWorkflowDefinitionVersion());
//        userTaskCreatedEvent.setBusinessKey();

        if(task.hasCustomHeaders()){
            addInfoFromCustomHeaders(task.getCustomHeaders().getFieldsMap(), userTaskCreatedEvent);
        }

        return userTaskCreatedEvent;
    }

    public static Camunda8UserTaskLifecycleEvent mapToUserTaskLifecycleInformation(Schema.JobRecord task){
        Camunda8UserTaskLifecycleEvent userTaskLifecycleEvent = new Camunda8UserTaskLifecycleEvent();

        userTaskLifecycleEvent.setKey(task.getMetadata().getKey());
        userTaskLifecycleEvent.setTimestamp(task.getMetadata().getTimestamp());

        userTaskLifecycleEvent.setProcessDefinitionKey(task.getProcessDefinitionKey());
        userTaskLifecycleEvent.setBpmnProcessId(task.getBpmnProcessId());
        userTaskLifecycleEvent.setElementId(task.getElementId());

        userTaskLifecycleEvent.setProcessInstanceKey(task.getProcessInstanceKey());
        userTaskLifecycleEvent.setElementInstanceKey(task.getElementInstanceKey());
        userTaskLifecycleEvent.setWorkflowDefinitionVersion(task.getWorkflowDefinitionVersion());

        userTaskLifecycleEvent.setIntent(
                Camunda8UserTaskLifecycleEvent.Intent.valueOf(
                        task.getMetadata().getIntent()));

        if(task.hasCustomHeaders()){
            addFormKey(task.getCustomHeaders().getFieldsMap(), userTaskLifecycleEvent);
        }

        return userTaskLifecycleEvent;
    }

    public static void addInfoFromCustomHeaders(Map<String, Value> customHeaderFieldsMap,
                                                Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        addFormKey(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
        addCandidateUsers(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
        addCandidateGroups(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
        addAssignee(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
//        addDueDate(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
//        addFollowUpDate(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
    }

    private static void addFormKey(Map<String, Value> fieldsMap,
                                   Camunda8UserTaskLifecycleEvent camunda8UserTaskLifecycleEvent){
        Value value = fieldsMap.get("io.camunda.zeebe:formKey");
        if(value != null){
            camunda8UserTaskLifecycleEvent.setFormKey(value.getStringValue());
        }
    }

    private static void addFormKey(Map<String, Value> fieldsMap,
                                   Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        Value value = fieldsMap.get("io.camunda.zeebe:formKey");
        if(value != null){
            camunda8UserTaskCreatedEvent.setFormKey(value.getStringValue());
        }
    }

    private static void addCandidateUsers(Map<String, Value> fieldsMap,
                                          Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        Value value = fieldsMap.get("io.camunda.zeebe:candidateUsers");
        if(value != null){
            camunda8UserTaskCreatedEvent.setCandidateGroups(getValuesStringList(value));
        }
    }


    private static void addCandidateGroups(Map<String, Value> fieldsMap,
                                           Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        Value value = fieldsMap.get("io.camunda.zeebe:candidateGroups");
        if(value != null){
            camunda8UserTaskCreatedEvent.setCandidateGroups(getValuesStringList(value));
        }
    }

    private static void addAssignee(Map<String, Value> fieldsMap,
                                    Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        Value value = fieldsMap.get("io.camunda.zeebe:assignee");
        if(value != null){
            camunda8UserTaskCreatedEvent.setAssignee(value.getStringValue());
        }
    }


    private static void addDueDate(Map<String, Value> fieldsMap, Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent) {
        Value value = fieldsMap.get("io.camunda.zeebe:dueDate");
        if(value != null){
            camunda8UserTaskCreatedEvent.setDueDate(OffsetDateTime.parse(value.getStringValue()));
        }
    }

    private static void addFollowUpDate(Map<String, Value> fieldsMap, Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent) {
        Value value = fieldsMap.get("io.camunda.zeebe:followUpDate");
        if(value != null){
            camunda8UserTaskCreatedEvent.setFollowUpDate(OffsetDateTime.parse(value.getStringValue()));
        }
    }


    @NotNull
    private static List<String> getValuesStringList(Value value) {
        return value
                .getListValue()
                .getValuesList()
                .stream()
                .map(Value::getStringValue)
                .collect(Collectors.toList());
    }

}
