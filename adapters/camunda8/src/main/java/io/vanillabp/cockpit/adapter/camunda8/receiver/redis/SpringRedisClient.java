package io.vanillabp.cockpit.adapter.camunda8.receiver.redis;


import com.google.protobuf.ByteString;
import io.camunda.zeebe.model.bpmn.impl.BpmnParser;
import io.camunda.zeebe.model.bpmn.instance.UserTask;
import io.lettuce.core.RedisClient;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.redis.connect.java.ZeebeRedis;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public class SpringRedisClient {

    private final ZeebeRedis zeebeRedis;
    private final RedisClient redisClient;
    private final Camunda8UserTaskEventHandler camunda8UserTaskEventHandler;
    private final Camunda8WorkflowEventHandler camunda8WorkflowEventHandler;

    Logger logger = LoggerFactory.getLogger(SpringRedisClient.class);


    public SpringRedisClient(SpringRedisClientProperties redisConfig,
                             Camunda8UserTaskEventHandler camunda8UserTaskEventHandler,
                             Camunda8WorkflowEventHandler camunda8WorkflowEventHandler) {

        redisClient = RedisClient.create(redisConfig.getUri());
        zeebeRedis = ZeebeRedis.newBuilder(redisClient)
                .consumerGroup(redisConfig.getConsumerGroup())
                .consumerId(redisConfig.getConsumerId())
                .addDeploymentListener(this::onDeployment)
                .addIncidentListener(this::onIncident)
                .addJobListener(this::onJob)
                .addProcessInstanceCreationListener(this::onProcessInstanceCreation)
                .addProcessInstanceListener(this::onProcessInstanceEvent)
                .addProcessEventListener(this::onProcessEvent)
                .build();
        this.camunda8UserTaskEventHandler = camunda8UserTaskEventHandler;
        this.camunda8WorkflowEventHandler = camunda8WorkflowEventHandler;
    }

    private void onProcessEvent(Schema.ProcessEventRecord processEventRecord) {
        logger.info("Process event record: \n{}", processEventRecord.toString());
    }


    private void onDeployment(Schema.DeploymentRecord deploymentRecord){

        /*
         * From the deployment record we get deployed resources, process metadata and dmn metadata lists.
         * Unfortunately the resources do not contain the information, if they are DMNs or BPMNs, so we must
         * match their names with the meta data to apply the corresponding method.
         */

        List<String> processResourceNames = deploymentRecord
                .getProcessMetadataList()
                .stream()
                .map(Schema.DeploymentRecord.ProcessMetadata::getResourceName)
                .toList();

        deploymentRecord
                .getResourcesList()
                .stream()
                .filter(resource -> processResourceNames.contains(resource.getResourceName()))
                .map(Schema.DeploymentRecord.Resource::getResource)
                .map(ByteString::toStringUtf8)
                .forEach(this::onResourceDeployment);
    }

    private void onResourceDeployment(String process){
        InputStream targetStream = new ByteArrayInputStream(process.getBytes());

        new BpmnParser()
                .parseModelFromStream(targetStream)
                .getModelElementsByType(UserTask.class)
                .forEach(this::onUserTaskDeployed);
    }

    private void onUserTaskDeployed(UserTask userTask) {
        logger.info("User task with id {} detected", userTask.getId());
    }


    private void onIncident(Schema.IncidentRecord incidentRecord){
        logger.info("An incident happened for process with id {}", incidentRecord.getBpmnProcessId());
    }

    private void onProcessInstanceCreation(
            Schema.ProcessInstanceCreationRecord processInstanceCreationRecord) {

        logger.debug("Process Instance Creation Record: {}", processInstanceCreationRecord.toString());
        if(processInstanceCreationRecord.getMetadata().getKey() != -1){
            camunda8WorkflowEventHandler.notify(
                    WorkflowEventProtobufMapper.map(processInstanceCreationRecord));
        }
    }

    private void onProcessInstanceEvent(
            Schema.ProcessInstanceRecord processInstanceRecord) {

        logger.debug("Process Instance event detected:\n{}", processInstanceRecord.toString());
        String intent = processInstanceRecord.getMetadata().getIntent();
        if(processInstanceRecord.hasMetadata() &&
                (intent.equals("ELEMENT_TERMINATED") || intent.equals("ELEMENT_COMPLETED")) &&
                processInstanceRecord.getBpmnElementType().equals("PROCESS")){
            camunda8WorkflowEventHandler.notify(
                    WorkflowEventProtobufMapper.map(processInstanceRecord));
        }
    }


    private void onJob(Schema.JobRecord jobRecord){
        if(!jobRecord.getMetadata().getRecordType().equals(Schema.RecordMetadata.RecordType.EVENT)) {
            return;
        }

        if(!jobRecord.getType().equals("io.camunda.zeebe:userTask")) {
            return;
        }

        logger.info("Job with id {} and type {} opened", jobRecord.getElementId(), jobRecord.getType());
        logger.debug("Job Record: {}", jobRecord);

        if(jobRecord.getMetadata().getIntent().equals("CREATED")) {
            camunda8UserTaskEventHandler.notify(
                    UserTaskEventProtobufMapper.mapToUserTaskCreatedInformation(jobRecord));
        } else {
            camunda8UserTaskEventHandler.notify(
                    UserTaskEventProtobufMapper.mapToUserTaskLifecycleInformation(jobRecord));
        }
    }

    @PreDestroy
    void shutdownClient(){
        zeebeRedis.close();
        redisClient.shutdown();
    }
}
