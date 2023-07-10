package io.vanillabp.cockpit.adapter.camunda7;

import java.util.Map;
import java.util.Optional;

import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda7.usertask.publishing.Camunda7UserTaskEventPublisher;
import io.vanillabp.cockpit.adapter.camunda7.wiring.Camunda7WiringPlugin;
import io.vanillabp.cockpit.adapter.camunda7.wiring.WiringBpmnParseListener;
import io.vanillabp.cockpit.adapter.common.CockpitCommonAdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.cockpit.commons.rest.adapter.versioning.ApiVersionAware;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;

@AutoConfigurationPackage(basePackageClasses = Camunda7AdapterConfiguration.class)
@AutoConfigureAfter(CockpitCommonAdapterConfiguration.class)
@AutoConfigureBefore(
        value = { CamundaBpmAutoConfiguration.class },
        // according to https://github.com/camunda/camunda-bpm-platform/blob/204b40921f7749a916b28c1073e0dc8df5c27134/engine/src/main/java/org/camunda/bpm/engine/impl/task/TaskDefinition.java#L181
        // built-in-task-listeners will always be added to the beginning of the list
        // of task-listeners. As we need cockpit-task-listeners to be executed after
        // vanilla-bp-wiring-task-listeners this module has to be initialized first.
        name = { "io.vanillabp.camunda7.Camunda7AdapterConfiguration" }) // 
public class Camunda7AdapterConfiguration {
    
    @Value("${workerId}")
    private String workerId;
    
    @Bean
    public Camunda7UserTaskEventHandler cockpitCamunda7UserTaskEventHandler() {
        
        return new Camunda7UserTaskEventHandler();
        
    }
    
    @Bean
    public WiringBpmnParseListener cockpitWiringBpmnParseListener(
            final CockpitProperties properties,
            final Camunda7UserTaskWiring userTaskWiring,
            final Camunda7UserTaskEventHandler userTaskEventHandler) {
        
        return new WiringBpmnParseListener(
                properties.isUserTasksEnabled(),
                userTaskWiring,
                userTaskEventHandler);
        
    }
    
    @Bean
    public Camunda7UserTaskEventPublisher camunda7UserTaskEventPublisher(
            final UserTaskPublishing userTaskPublishing) {
        
        return new Camunda7UserTaskEventPublisher(
                userTaskPublishing);
        
    }
    
    @Bean
    public Camunda7UserTaskWiring cockpitCamunda7UserTaskWiring(
            final ApplicationContext applicationContext,
            final CockpitProperties properties,
            final UserTasksWorkflowProperties workflowsCockpitProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            @Qualifier("BpmsApi") ApiVersionAware bpmsApiVersionAware,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Camunda7UserTaskEventHandler camunda7UserTaskEventHandler) throws Exception {
        
        return new Camunda7UserTaskWiring(
                applicationContext,
                properties,
                workflowsCockpitProperties,
                applicationEventPublisher,
                templating,
                bpmsApiVersionAware,
                connectableServices,
                new UserTaskMethodParameterFactory(),
                camunda7UserTaskEventHandler);
        
    }
    
    @Bean
    public Camunda7WiringPlugin cockpitCamunda7WiringPlugin(
            final WiringBpmnParseListener wiringBpmnParseListener) {
        
        return new Camunda7WiringPlugin(
                wiringBpmnParseListener);
        
    }
    
}
