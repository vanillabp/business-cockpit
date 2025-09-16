package io.vanillabp.cockpit.adapter.camunda8.usertask;

import freemarker.template.Configuration;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.worker.JobWorkerBuilderStep1;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import io.vanillabp.cockpit.adapter.common.CockpitCommonAdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.wiring.AbstractUserTaskWiring;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.ModuleAwareBpmnDeployment;
import io.vanillabp.springboot.adapter.SpringBeanUtil;
import io.vanillabp.springboot.adapter.SpringDataUtil;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.CrudRepository;

public class Camunda8UserTaskWiring extends AbstractUserTaskWiring<Camunda8UserTaskConnectable, UserTaskMethodParameterFactory> implements Consumer<CamundaClient> {

    public static final String TASKDEFINITION_USERTASK_DETAILSPROVIDER = "io.vanillabp.businesscockpit:";

    private final VanillaBpCockpitProperties vanillaBpCockpitProperties;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final Optional<Configuration> templating;

    private final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices;

    private final Camunda8UserTaskEventHandler userTaskEventHandler;

    private final Method noopUserTaskMethod;

    private final ObjectProvider<Camunda8UserTaskHandler> userTaskHandlers;

    private CamundaClient client;

    private final Map<String, JobWorkerBuilderStep1.JobWorkerBuilderStep3> workers = new HashMap<>();

    private final SpringDataUtil springDataUtil;

    private final String workerId;

    final Camunda8VanillaBpProperties camunda8Properties;

    public Camunda8UserTaskWiring(
            final ApplicationContext applicationContext,
            final Camunda8VanillaBpProperties camunda8Properties,
            final String workerId,
            final SpringBeanUtil springBeanUtil,
            final SpringDataUtil springDataUtil,
            final VanillaBpCockpitProperties vanillaBpCockpitProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Camunda8UserTaskEventHandler userTaskEventHandler,
            final ObjectProvider<Camunda8UserTaskHandler> userTaskHandlers) throws Exception {
        
        super(applicationContext, springBeanUtil, new UserTaskMethodParameterFactory());
        this.workerId = workerId;
        this.connectableServices = connectableServices;
        this.vanillaBpCockpitProperties = vanillaBpCockpitProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.templating = templating;
        this.userTaskEventHandler = userTaskEventHandler;
        this.userTaskHandlers = userTaskHandlers;
        this.springDataUtil = springDataUtil;
        this.camunda8Properties = camunda8Properties;

        noopUserTaskMethod = getClass().getMethod("noopUserTaskMethod", PrefilledUserTaskDetails.class);

    }

    /**
     * Called by <i>Camunda8DeploymentAdapter#processBpmnModel(String, BpmnModelInstanceImpl, boolean)</i> to
     * ensure client is available before using wire-methods.
     */
    @Override
    public void accept(
            final CamundaClient client) {

        this.client = client;

    }

    @EventListener
    public void openWorkers(
            final ModuleAwareBpmnDeployment.BpmnModelCacheProcessed event) {

        userTaskEventHandler.updateVersionInfos(event);

        workers
                .values()
                .forEach(JobWorkerBuilderStep1.JobWorkerBuilderStep3::open);

    }

    public void wireTask(
            final String workflowModuleId,
            final Camunda8UserTaskConnectable connectable) {

        final var workflowAggregateAndServiceClass =
                determineAndValidateWorkflowAggregateAndServiceClass(
                        connectable.getBpmnProcessId());
        final var workflowAggregateClass = workflowAggregateAndServiceClass.getKey();
        
        final var processService = connectableServices
                .get(workflowAggregateClass);
        
        final var methodFound = super.wireTask(
                connectable,
                true,
                (method, annotation) -> methodMatchesTaskDefinition(connectable, method, annotation),
                (method, annotation) -> methodMatchesElementId(connectable, method, annotation),
                (method, annotation) -> validateParameters(workflowAggregateClass, method),
                (bean, method, parameters) -> connectToBpms(workflowModuleId, processService, bean, connectable, method, parameters));
        
        if (methodFound) {
            return;
        }
        
        final var noopUserTaskMethodParameters = validateParameters(
                workflowAggregateClass,
                noopUserTaskMethod);

        connectToBpms(
                workflowModuleId,
                processService,
                this,
                connectable,
                noopUserTaskMethod,
                noopUserTaskMethodParameters);

    }
    
    private void connectToBpms(
            final String workflowModuleId,
            final AdapterAwareProcessService<?> processService,
            final Object bean,
            final Camunda8UserTaskConnectable connectable,
            final Method method,
            final List<MethodParameter> parameters) {
        
        if (!UserTaskDetails.class.isAssignableFrom(method.getReturnType())) {
            throw new RuntimeException(
                    "Method '"
                    + method.getName()
                    + "' of class '"
                    + determineBeanClass(bean).getName()
                    + "' annotated with @"
                    + getAnnotationType().getName()
                    + " has not a return-type of '"
                    + UserTaskDetails.class.getName()
                    + "' which is mandatory for methods providing user-task details!");
        }

        final var jobType = TASKDEFINITION_USERTASK_DETAILSPROVIDER + connectable.getTaskDefinition();
        if (workers.containsKey(jobType)) {
            return;
        }

        CrudRepository<Object, Object> workflowAggregateRepository =
                (CrudRepository<Object, Object>) processService.getWorkflowAggregateRepository();
        final var aggregateIdPropertyName = springDataUtil.getIdName(processService.getWorkflowAggregateClass());

        final var taskHandler = userTaskHandlers.getObject(
                connectable.getTaskDefinition(),
                vanillaBpCockpitProperties,
                applicationEventPublisher,
                templating,
                connectable.getBpmnProcessId(),
                connectable.getVersionInfo(),
                connectable.getBpmnProcessName(),
                connectable.getTitle(),
                processService,
                aggregateIdPropertyName,
                workflowAggregateRepository,
                bean,
                method,
                parameters,
                client
        );
        userTaskEventHandler.addTaskHandler(connectable, taskHandler);

        final var worker = client
                .newWorker()
                .jobType(jobType)
                .handler(userTaskEventHandler)
                .name(workerId)
                .fetchVariables(List.of(aggregateIdPropertyName));

        final var workerProperties = camunda8Properties.getWorkerProperties(
                workflowModuleId,
                connectable.getBpmnProcessId(),
                connectable.getTaskDefinition());
        workerProperties.applyToWorker(worker);

        workers.put(
                jobType,
                connectable.getTenantId() != null
                        ? worker.tenantId(connectable.getTenantId())
                        : worker);

    }

    @SuppressWarnings("unchecked")
    protected List<MethodParameter> validateParameters(
            final Class<?> workflowAggregateClass,
            final Method method) {
        
        return super.validateParameters(
                method,
                (m, parameter, index) -> validateWorkflowAggregateParameter(
                        workflowAggregateClass,
                        m,
                        parameter,
                        index),
                super::validateTaskParam,
                super::validateDetailsEvent,
                super::validateMultiInstanceTotal,
                super::validateMultiInstanceIndex,
                super::validateMultiInstanceElement,
                super::validatePrefilledUserTaskDetails);
        
    }
        
    public UserTaskDetails noopUserTaskMethod(
            final PrefilledUserTaskDetails userTaskDetails) {
        
        return userTaskDetails;
        
    }
    
}
