package io.vanillabp.cockpit.adapter.camunda7;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda7.service.Camunda7BusinessCockpitService;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda7.usertask.publishing.Camunda7UserTaskEventPublisher;
import io.vanillabp.cockpit.adapter.camunda7.wiring.Camunda7HistoryEventProducerSupplier;
import io.vanillabp.cockpit.adapter.camunda7.wiring.Camunda7WiringPlugin;
import io.vanillabp.cockpit.adapter.camunda7.wiring.WiringBpmnParseListener;
import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowWiring;
import io.vanillabp.cockpit.adapter.common.CockpitCommonAdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.service.AdapterConfigurationBase;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.SpringDataUtil;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.repository.CrudRepository;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@AutoConfigurationPackage(basePackageClasses = Camunda7AdapterConfiguration.class)
@AutoConfigureAfter(CockpitCommonAdapterConfiguration.class)
@AutoConfigureBefore(
        value = { CamundaBpmAutoConfiguration.class },
        // according to https://github.com/camunda/camunda-bpm-platform/blob/204b40921f7749a916b28c1073e0dc8df5c27134/engine/src/main/java/org/camunda/bpm/engine/impl/task/TaskDefinition.java#L181
        // built-in-task-listeners will always be added to the beginning of the list
        // of task-listeners. As we need cockpit-task-listeners to be executed after
        // vanilla-bp-wiring-task-listeners this module has to be initialized first.
        name = { "io.vanillabp.camunda7.Camunda7AdapterConfiguration" })
public class Camunda7AdapterConfiguration extends AdapterConfigurationBase<Camunda7BusinessCockpitService<?>> {
    
    public static final String ADAPTER_ID = "camunda7";
    
    @Value("${workerId}")
    private String workerId;

    @Autowired
    @Lazy
    private TaskService taskService;

    @Autowired
    @Lazy
    private RuntimeService runtimeService;

    @Autowired
    @Lazy
    private HistoryService historyService;
    
    @Autowired
    @Lazy
    private RepositoryService repositoryService;

    @Autowired
    private CockpitProperties cockpitProperties;
    
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    @Autowired
    @Lazy
    private WorkflowPublishing workflowPublishing;
    
    private Camunda7HistoryEventProducerSupplier camunda7HistoryEventProducerSupplier = new Camunda7HistoryEventProducerSupplier();
    
    @Bean
    public Camunda7UserTaskEventHandler cockpitCamunda7UserTaskEventHandler() {
        
        return new Camunda7UserTaskEventHandler();
        
    }

    @Bean
    public WiringBpmnParseListener cockpitWiringBpmnParseListener(
            final Camunda7UserTaskWiring userTaskWiring,
            final Camunda7UserTaskEventHandler userTaskEventHandler,
            final Camunda7WorkflowWiring workflowWiring) {
        
        return new WiringBpmnParseListener(
                cockpitProperties.isUserTasksEnabled(),
                userTaskWiring,
                userTaskEventHandler,
                workflowWiring);
        
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
            final UserTasksWorkflowProperties workflowsCockpitProperties,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Camunda7UserTaskEventHandler camunda7UserTaskEventHandler) throws Exception {
        
        return new Camunda7UserTaskWiring(
                applicationContext,
                cockpitProperties,
                workflowsCockpitProperties,
                applicationEventPublisher,
                templating,
                connectableServices,
                new UserTaskMethodParameterFactory(),
                camunda7UserTaskEventHandler);
        
    }

    @Bean
    public Camunda7WorkflowWiring cockpitCamunda7WorkflowWiring(
            final ApplicationContext applicationContext,
            final UserTasksWorkflowProperties workflowsCockpitProperties,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Camunda7WorkflowEventHandler workflowEventListener) {
        return new Camunda7WorkflowWiring(
                applicationContext,
                cockpitProperties,
                workflowsCockpitProperties,
                new WorkflowMethodParameterFactory(),
                connectableServices,
                getConnectableServices(),
                templating,
                workflowEventListener
        );
    }

    @Bean
    public Camunda7HistoryEventProducerSupplier camunda7HistoryEventProducerSupplier() {
        
        return camunda7HistoryEventProducerSupplier;
        
    }
    
    @Bean
    public Camunda7WiringPlugin cockpitCamunda7WiringPlugin(
            final WiringBpmnParseListener wiringBpmnParseListener,
            final Camunda7HistoryEventProducerSupplier historyEventProducerSupplier) {
        
        return new Camunda7WiringPlugin(
                wiringBpmnParseListener,
                historyEventProducerSupplier);
        
    }

    @Bean
    public Camunda7WorkflowEventHandler cockpitCamunda7WorkflowEventHandler() {

        return new Camunda7WorkflowEventHandler(
                cockpitProperties,
                historyService,
                repositoryService,
                workflowPublishing,
                applicationEventPublisher);

    }
    
    @Override
    public String getAdapterId() {
        
        return ADAPTER_ID;
        
    }
    
    @Override
    public <WA> Camunda7BusinessCockpitService<?> newBusinessCockpitServiceImplementation(
            final SpringDataUtil springDataUtil,
            final Class<WA> workflowAggregateClass,
            final Class<?> workflowAggregateIdClass,
            final CrudRepository<WA, Object> workflowAggregateRepository) {
        
        final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
        if (String.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> businessKey;
        } else if (int.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Integer.valueOf(businessKey);
        } else if (long.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Long.valueOf(businessKey);
        } else if (float.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Float.valueOf(businessKey);
        } else if (double.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Double.valueOf(businessKey);
        } else if (byte.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Byte.valueOf(businessKey);
        } else if (BigInteger.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> new BigInteger(businessKey);
        } else {
            try {
                final var valueOfMethod = workflowAggregateIdClass.getMethod("valueOf", String.class);
                parseWorkflowAggregateIdFromBusinessKey = businessKey -> {
                        try {
                            return valueOfMethod.invoke(null, businessKey);
                        } catch (Exception e) {
                            throw new RuntimeException("Could not determine the workflow's aggregate id!", e);
                        }
                    };
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format(
                                "The id's class '%s' of the workflow-aggregate '%s' does not implement a method 'public static %s valueOf(String businessKey)'! Please add this method required by VanillaBP 'camunda7' adapter.",
                                workflowAggregateIdClass.getName(),
                                workflowAggregateClass.getName(),
                                workflowAggregateIdClass.getSimpleName()));
            }
        }
        
        final var result = new Camunda7BusinessCockpitService<WA>(
                taskService,
                runtimeService,
                springDataUtil::getId,
                workflowAggregateRepository,
                workflowAggregateClass,
                parseWorkflowAggregateIdFromBusinessKey,
                cockpitCamunda7UserTaskEventHandler(),
                cockpitCamunda7WorkflowEventHandler());
        
        putConnectableService(workflowAggregateClass, result);
        
        return result;
        
    }

}
