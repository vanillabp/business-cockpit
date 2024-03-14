package io.vanillabp.cockpit.adapter.camunda8.receiver.redis;


import io.lettuce.core.RedisClient;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.redis.connect.java.ZeebeRedis;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                .addProcessInstanceCreationListener(this::onProcessInstanceCreation)
                .addProcessInstanceListener(this::onProcessInstanceEvent)
                .addJobListener(this::onJob)
                .build();
        this.camunda8UserTaskEventHandler = camunda8UserTaskEventHandler;
        this.camunda8WorkflowEventHandler = camunda8WorkflowEventHandler;
    }

    private void onProcessInstanceCreation(
            Schema.ProcessInstanceCreationRecord processInstanceCreationRecord) {

        logger.debug("Process Instance Creation Record:\n{}", processInstanceCreationRecord.toString());
        if(processInstanceCreationRecord.getMetadata().getKey() != -1){
            camunda8WorkflowEventHandler.notify(
                    WorkflowEventProtobufMapper.map(processInstanceCreationRecord));
        }
    }

    private void onProcessInstanceEvent(
            Schema.ProcessInstanceRecord processInstanceRecord) {

        logger.debug("Process Instance Record:\n{}", processInstanceRecord.toString());
        String intent = processInstanceRecord.getMetadata().getIntent();
        if(processInstanceRecord.hasMetadata() &&
                (intent.equals("ELEMENT_TERMINATED") || intent.equals("ELEMENT_COMPLETED")) &&
                processInstanceRecord.getBpmnElementType().equals("PROCESS")){
            camunda8WorkflowEventHandler.notify(
                    WorkflowEventProtobufMapper.map(processInstanceRecord));
        }
    }


    private void onJob(Schema.JobRecord jobRecord){
        if(!jobRecord.getMetadata().getRecordType().equals(Schema.RecordMetadata.RecordType.EVENT) ||
                !jobRecord.getType().equals("io.camunda.zeebe:userTask")) {
            return;
        }

        logger.info("Job with id {} and type {} opened", jobRecord.getElementId(), jobRecord.getType());
        logger.debug("Job Record:\n{}", jobRecord);

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
