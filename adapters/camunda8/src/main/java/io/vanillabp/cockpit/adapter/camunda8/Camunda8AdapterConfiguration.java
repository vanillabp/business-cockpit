package io.vanillabp.cockpit.adapter.camunda8;

import freemarker.template.Configuration;
import io.camunda.zeebe.spring.client.CamundaAutoConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentRepository;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentResourceRepository;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentService;
import io.vanillabp.cockpit.adapter.camunda8.receiver.redis.SpringRedisClient;
import io.vanillabp.cockpit.adapter.camunda8.receiver.redis.SpringRedisClientProperties;
import io.vanillabp.cockpit.adapter.camunda8.service.Camunda8BusinessCockpitService;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.Camunda8UserTaskEventPublisher;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8DeploymentAdapter;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowWiring;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceRepository;
import io.vanillabp.cockpit.adapter.common.CockpitCommonAdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.service.AdapterConfigurationBase;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.SpringDataUtil;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.CrudRepository;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


@AutoConfigurationPackage(basePackageClasses = Camunda8AdapterConfiguration.class)
@AutoConfigureAfter(CockpitCommonAdapterConfiguration.class)
@AutoConfigureBefore(CamundaAutoConfiguration.class)
@EnableConfigurationProperties({ SpringRedisClientProperties.class })
public class Camunda8AdapterConfiguration extends AdapterConfigurationBase<Camunda8BusinessCockpitService<?>> {

    public static final String ADAPTER_ID = "camunda8";

    @Value("${workerId}")
    private String workerId;

    @Value("${spring.application.name:@null}")
    private String applicationName;

    @Override
    public String getAdapterId() {
        return ADAPTER_ID;
    }

    @Bean
    public Camunda8UserTaskEventHandler camunda8UserTaskEventHandler(){
        return new Camunda8UserTaskEventHandler();
    }

    @Bean
    public Camunda8WorkflowEventHandler camunda8WorkflowEventHandler(
            final ApplicationEventPublisher applicationEventPublisher,
            final WorkflowPublishing workflowPublishing){
        return new Camunda8WorkflowEventHandler(applicationEventPublisher, workflowPublishing);
    }

    @Bean
    public SpringRedisClient springRedisClient(SpringRedisClientProperties springRedisClientProperties,
                                               Camunda8UserTaskEventHandler camunda8UserTaskEventHandler,
                                               Camunda8WorkflowEventHandler camunda8WorkflowEventHandler){
        return new SpringRedisClient(springRedisClientProperties, camunda8UserTaskEventHandler, camunda8WorkflowEventHandler);
    }

    @Bean
    public Camunda8UserTaskEventPublisher camunda8UserTaskEventPublisher(
            final UserTaskPublishing userTaskPublishing) {

        return new Camunda8UserTaskEventPublisher(
                userTaskPublishing);
    }


    @Bean
    public Camunda8UserTaskWiring camunda8UserTaskWiring(
            final ApplicationContext applicationContext,
            final CockpitProperties properties,
            final UserTasksWorkflowProperties workflowsCockpitProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Camunda8UserTaskEventHandler userTaskEventHandler,
            final ProcessInstanceRepository processInstanceRepository
    ) throws Exception {
        return new Camunda8UserTaskWiring(
                applicationContext,
                properties,
                workflowsCockpitProperties,
                applicationEventPublisher,
                templating,
                connectableServices,
                userTaskEventHandler,
                processInstanceRepository
        );
    }

    @Bean
    public Camunda8WorkflowWiring camunda8WorkflowWiring(
            final ApplicationContext applicationContext,
            final CockpitProperties cockpitProperties,
            final UserTasksWorkflowProperties workflowsCockpitProperties,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Collection<Camunda8BusinessCockpitService<?>> connectableCockpitServices,
            final Optional<Configuration> templating,
            final Camunda8WorkflowEventHandler workflowEventListener,
            final ProcessInstanceRepository processInstanceRepository){
        return new Camunda8WorkflowWiring(
                applicationContext,
                cockpitProperties,
                workflowsCockpitProperties,
                new WorkflowMethodParameterFactory(),
                connectableServices,
                getConnectableServices(),
                templating,
                workflowEventListener,
                processInstanceRepository
        );
    }

    @Bean
    public DeploymentService camunda8BusinessCockpitDeploymentService(
            final DeploymentRepository deploymentRepository,
            final DeploymentResourceRepository deploymentResourceRepository) {

        return new DeploymentService(
                deploymentRepository,
                deploymentResourceRepository
        );
    }

    @Bean
    public Camunda8DeploymentAdapter camunda8BusinessCockpitDeploymentAdapter(
            VanillaBpProperties properties,
            @Qualifier("camunda8BusinessCockpitDeploymentService") DeploymentService deploymentService,
            Camunda8UserTaskWiring camunda8UserTaskWiring,
            Camunda8WorkflowWiring camunda8WorkflowWiring){

        return new Camunda8DeploymentAdapter(
                applicationName,
                properties,
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

        final var result = new Camunda8BusinessCockpitService<WA>(
                workflowAggregateRepository,
                workflowAggregateClass,
                springDataUtil::getId,
                parseWorkflowAggregateIdFromBusinessKey
        );

        putConnectableService(workflowAggregateClass, result);

        return result;

    }
}
