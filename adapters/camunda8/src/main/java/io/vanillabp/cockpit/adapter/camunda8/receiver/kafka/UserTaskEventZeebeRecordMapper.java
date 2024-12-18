package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.commons.utils.DateTimeUtil;
import java.util.Map;

public class UserTaskEventZeebeRecordMapper {

    public static Camunda8UserTaskCreatedEvent mapToUserTaskCreatedInformation(JobRecordValue jobRecord){
        Camunda8UserTaskCreatedEvent userTaskCreatedEvent = new Camunda8UserTaskCreatedEvent();

        userTaskCreatedEvent.setProcessDefinitionKey(jobRecord.getProcessDefinitionKey());
        userTaskCreatedEvent.setBpmnProcessId(jobRecord.getBpmnProcessId());
        userTaskCreatedEvent.setTenantId(jobRecord.getTenantId());
        userTaskCreatedEvent.setElementId(jobRecord.getElementId());

        userTaskCreatedEvent.setProcessInstanceKey(jobRecord.getProcessInstanceKey());
        userTaskCreatedEvent.setElementInstanceKey(jobRecord.getElementInstanceKey());
        userTaskCreatedEvent.setWorkflowDefinitionVersion(jobRecord.getProcessDefinitionVersion());
        userTaskCreatedEvent.setBusinessKey(jobRecord.getTenantId());

        if(jobRecord.getCustomHeaders() != null){
            addInfoFromCustomHeaders(jobRecord.getCustomHeaders(), userTaskCreatedEvent);
        }

        return userTaskCreatedEvent;
    }

    public static void addMetaData(Camunda8UserTaskCreatedEvent userTaskCreatedEvent, Record<?> task){
        userTaskCreatedEvent.setKey(task.getKey());
        userTaskCreatedEvent.setTimestamp(
                DateTimeUtil.fromMilliseconds(task.getTimestamp()));
    }

    public static Camunda8UserTaskLifecycleEvent mapToUserTaskLifecycleInformation(JobRecordValue task){
        Camunda8UserTaskLifecycleEvent userTaskLifecycleEvent = new Camunda8UserTaskLifecycleEvent();

        userTaskLifecycleEvent.setProcessDefinitionKey(task.getProcessDefinitionKey());
        userTaskLifecycleEvent.setBpmnProcessId(task.getBpmnProcessId());
        userTaskLifecycleEvent.setTenantId(task.getTenantId());
        userTaskLifecycleEvent.setElementId(task.getElementId());

        userTaskLifecycleEvent.setProcessInstanceKey(task.getProcessInstanceKey());
        userTaskLifecycleEvent.setElementInstanceKey(task.getElementInstanceKey());
        userTaskLifecycleEvent.setWorkflowDefinitionVersion(task.getProcessDefinitionVersion());


        if(task.getCustomHeaders() != null){
            addFormKey(task.getCustomHeaders(), userTaskLifecycleEvent);
        }

        return userTaskLifecycleEvent;
    }

    public static void addMetaData(Camunda8UserTaskLifecycleEvent userTaskLifecycleEvent, Record<?> task){
        userTaskLifecycleEvent.setKey(task.getKey());
        userTaskLifecycleEvent.setTimestamp(
                DateTimeUtil.fromMilliseconds(task.getTimestamp()));
        userTaskLifecycleEvent.setIntent(
                Camunda8UserTaskLifecycleEvent.Intent.valueOf(task.getIntent().name()));
    }


    public static void addInfoFromCustomHeaders(Map<String, String> customHeaderFieldsMap,
                                                Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        addFormKey(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
        addCandidateUsers(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
        addCandidateGroups(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
        addAssignee(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
//        addDueDate(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
//        addFollowUpDate(customHeaderFieldsMap, camunda8UserTaskCreatedEvent);
    }

    private static void addFormKey(Map<String, String> fieldsMap,
                                   Camunda8UserTaskLifecycleEvent camunda8UserTaskLifecycleEvent){
        String value = fieldsMap.get("io.camunda.zeebe:formKey");
        if(value != null){
            camunda8UserTaskLifecycleEvent.setFormKey(value);
        }
    }

    private static void addFormKey(Map<String, String> fieldsMap,
                                   Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        String value = fieldsMap.get("io.camunda.zeebe:formKey");
        if(value != null){
            camunda8UserTaskCreatedEvent.setFormKey(value);
        }
    }

    private static void addCandidateUsers(Map<String, String> fieldsMap,
                                          Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        String value = fieldsMap.get("io.camunda.zeebe:candidateUsers");
//        if(value != null){
//            camunda8UserTaskCreatedEvent.setCandidateUsers(getValuesStringList(value));
//        }
    }


    private static void addCandidateGroups(Map<String, String> fieldsMap,
                                           Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        String value = fieldsMap.get("io.camunda.zeebe:candidateGroups");
//        if(value != null){
//            camunda8UserTaskCreatedEvent.setCandidateGroups(getValuesStringList(value));
//        }
    }

    private static void addAssignee(Map<String, String> fieldsMap,
                                    Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent){
        String value = fieldsMap.get("io.camunda.zeebe:assignee");
        if(value != null){
            camunda8UserTaskCreatedEvent.setAssignee(value);
        }
    }
}