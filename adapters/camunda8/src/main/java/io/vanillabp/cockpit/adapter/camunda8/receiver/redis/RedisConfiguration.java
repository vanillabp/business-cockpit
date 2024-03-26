package io.vanillabp.cockpit.adapter.camunda8.receiver.redis;

import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.zeebe.redis.connect.java.ZeebeRedis;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties({ SpringRedisClientProperties.class })
@ConditionalOnClass(ZeebeRedis.class)
@ConditionalOnProperty(prefix = SpringRedisClientProperties.PREFIX, name = "url")
public class RedisConfiguration {

    @Bean
    public SpringRedisClient springRedisClient(SpringRedisClientProperties springRedisClientProperties,
                                               Camunda8UserTaskEventHandler camunda8UserTaskEventHandler,
                                               Camunda8WorkflowEventHandler camunda8WorkflowEventHandler){
        return new SpringRedisClient(springRedisClientProperties, camunda8UserTaskEventHandler, camunda8WorkflowEventHandler);
    }
}
