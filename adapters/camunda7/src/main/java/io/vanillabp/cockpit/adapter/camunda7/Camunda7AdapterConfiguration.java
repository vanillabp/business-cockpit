package io.vanillabp.cockpit.adapter.camunda7;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import org.camunda.bpm.engine.RepositoryService;
import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda7.usertask.publishing.Camunda7UserTaskEventPublisher;
import io.vanillabp.cockpit.adapter.camunda7.wiring.Camunda7WiringPlugin;
import io.vanillabp.cockpit.adapter.camunda7.wiring.WiringBpmnParseListener;
import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowEventSpringListener;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.Camunda7WorkflowEventPublisher;
import io.vanillabp.cockpit.adapter.common.CockpitCommonAdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.commons.rest.adapter.versioning.ApiVersionAware;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;

@AutoConfigurationPackage(basePackageClasses = Camunda7AdapterConfiguration.class)
@AutoConfigureAfter(CockpitCommonAdapterConfiguration.class)
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

    @Bean
    public Camunda7WorkflowEventSpringListener camunda7WorkflowEventSpringListener(
            final ApplicationEventPublisher applicationEventPublisher,
            final CockpitProperties cockpitProperties,
            final ApiVersionAware bpmsApiVersionAware,
            final RepositoryService repositoryService) {

        return new Camunda7WorkflowEventSpringListener(applicationEventPublisher,
                cockpitProperties,
                bpmsApiVersionAware,
                repositoryService);

    }

    @Bean
    public Camunda7WorkflowEventPublisher camunda7WorkflowEventPublisher(
            final WorkflowPublishing workflowPublishing) {

        return new Camunda7WorkflowEventPublisher(
                workflowPublishing);

    }

}
