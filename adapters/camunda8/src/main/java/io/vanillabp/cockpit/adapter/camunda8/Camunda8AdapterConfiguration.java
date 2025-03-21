package io.vanillabp.cockpit.adapter.camunda8;

import freemarker.template.Configuration;
import io.camunda.zeebe.spring.client.CamundaAutoConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.deployments.Camunda8DeploymentAdapter;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentPersistence;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentService;
import io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstancePersistence;
import io.vanillabp.cockpit.adapter.camunda8.service.Camunda8BusinessCockpitService;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.Camunda8UserTaskEventPublisher;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowWiring;
import io.vanillabp.cockpit.adapter.common.CockpitCommonAdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.service.AdapterConfigurationBase;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishing;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.SpringBeanUtil;
import io.vanillabp.springboot.adapter.SpringDataUtil;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.repository.CrudRepository;


@AutoConfigurationPackage(basePackageClasses = Camunda8AdapterConfiguration.class)
@EnableConfigurationProperties(Camunda8VanillaBpProperties.class)
@AutoConfigureAfter(CockpitCommonAdapterConfiguration.class)
@AutoConfigureBefore(CamundaAutoConfiguration.class)
public class Camunda8AdapterConfiguration extends AdapterConfigurationBase<Camunda8BusinessCockpitService<?>> {

    public static final String ADAPTER_ID = "camunda8";

    @Value("${workerId}")
    private String workerId;

    @Value("${spring.application.name:@null}")
    private String applicationName;

    @Lazy
    @Autowired
    private Camunda8WorkflowEventHandler workflowEventHandler;

    @Autowired
    private ProcessInstancePersistence processInstancePersistence;

    @Override
    public String getAdapterId() {
        return ADAPTER_ID;
    }

    @Bean
    public Camunda8UserTaskEventHandler camunda8BusinessCockpitUserTaskEventHandler(){
        return new Camunda8UserTaskEventHandler();
    }

    @Bean
    public Camunda8WorkflowEventHandler ccamunda8BusinessCockpitWorkflowEventHandler(
            final ApplicationEventPublisher applicationEventPublisher,
            final WorkflowPublishing workflowPublishing){
        return new Camunda8WorkflowEventHandler(applicationEventPublisher, workflowPublishing);
    }


    @Bean
    public Camunda8UserTaskEventPublisher camunda8BusinessCockpitUserTaskEventPublisher(
            final UserTaskPublishing userTaskPublishing) {

        return new Camunda8UserTaskEventPublisher(
                userTaskPublishing);
    }

    @Bean
    public Camunda8UserTaskWiring camunda8BusinessCockpitUserTaskWiring(
            final ApplicationContext applicationContext,
            final VanillaBpCockpitProperties properties,
            final SpringBeanUtil springBeanUtil,
            final ApplicationEventPublisher applicationEventPublisher,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Camunda8UserTaskEventHandler userTaskEventHandler
    ) throws Exception {
        return new Camunda8UserTaskWiring(
                applicationContext,
                springBeanUtil,
                properties,
                applicationEventPublisher,
                templating,
                connectableServices,
                userTaskEventHandler,
                processInstancePersistence
        );
    }

    @Bean
    public Camunda8WorkflowWiring camunda8BusinessCockpitWorkflowWiring(
            final ApplicationContext applicationContext,
            final VanillaBpCockpitProperties cockpitProperties,
            final Camunda8VanillaBpProperties camunda8Properties,
            final SpringBeanUtil springBeanUtil,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            final Camunda8WorkflowEventHandler workflowEventListener,
            final WorkflowModulePublishing workflowModulePublishing){
        return new Camunda8WorkflowWiring(
                applicationContext,
                cockpitProperties,
                camunda8Properties,
                springBeanUtil,
                new WorkflowMethodParameterFactory(),
                connectableServices,
                getConnectableServices(),
                templating,
                workflowEventListener,
                processInstancePersistence,
                workflowModulePublishing);
    }

    @Bean
    public DeploymentService camunda8BusinessCockpitDeploymentService(
            final DeploymentPersistence deploymentPersistence) {

        return new DeploymentService(deploymentPersistence);

    }

    @Bean
    public Camunda8DeploymentAdapter camunda8BusinessCockpitDeploymentAdapter(
            final VanillaBpProperties properties,
            final Camunda8VanillaBpProperties camunda8Properties,
            final @Qualifier("camunda8BusinessCockpitDeploymentService") DeploymentService deploymentService,
            final Camunda8UserTaskWiring camunda8UserTaskWiring,
            final Camunda8WorkflowWiring camunda8WorkflowWiring){

        return new Camunda8DeploymentAdapter(
                applicationName,
                properties,
                camunda8Properties,
                deploymentService,
                camunda8UserTaskWiring,
                camunda8WorkflowWiring);
    }

    @Override
    public <WA> Camunda8BusinessCockpitService<?> newBusinessCockpitServiceImplementation(
            final SpringDataUtil springDataUtil,
            final Class<WA> workflowAggregateClass,
            final Class<?> workflowAggregateIdClass,
            final CrudRepository<WA, Object> workflowAggregateRepository) {

        final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
        if (String.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> businessKey;
        } else if (int.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Integer::valueOf;
        } else if (long.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Long::valueOf;
        } else if (float.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Float::valueOf;
        } else if (double.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Double::valueOf;
        } else if (byte.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Byte::valueOf;
        } else if (BigInteger.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = BigInteger::new;
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

        final var result = new Camunda8BusinessCockpitService<WA>(
                workflowAggregateRepository,
                workflowAggregateClass,
                springDataUtil::getId,
                parseWorkflowAggregateIdFromBusinessKey,
                springDataUtil.getIdName(workflowAggregateClass),
                processInstancePersistence,
                workflowEventHandler
        );

        putConnectableService(workflowAggregateClass, result);

        return result;

    }

    public Set<String> getIdNames(){
        return this.getConnectableServices()
                .stream()
                .map(Camunda8BusinessCockpitService::getWorkflowAggregateIdName)
                .collect(Collectors.toSet());
    }

}
