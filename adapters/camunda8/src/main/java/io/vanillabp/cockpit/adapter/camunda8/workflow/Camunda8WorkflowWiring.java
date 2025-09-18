package io.vanillabp.cockpit.adapter.camunda8.workflow;

import freemarker.template.Configuration;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.worker.JobWorkerBuilderStep1;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties;
import io.vanillabp.cockpit.adapter.camunda8.service.Camunda8BusinessCockpitService;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8WorkflowConnectable;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.wiring.AbstractWorkflowWiring;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishing;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.service.WorkflowService;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.ModuleAwareBpmnDeployment;
import io.vanillabp.springboot.adapter.SpringBeanUtil;
import io.vanillabp.springboot.adapter.SpringDataUtil;
import io.vanillabp.springboot.adapter.wiring.ConnectBean;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

public class Camunda8WorkflowWiring extends AbstractWorkflowWiring<Camunda8UserTaskConnectable, WorkflowMethodParameterFactory, Camunda8BusinessCockpitService<?>>
        implements Consumer<CamundaClient> {

    public static final String TASKDEFINITION_WORKFLOW_DETAILSPROVIDER = "io.vanillabp.businesscockpit:";

    private final VanillaBpCockpitProperties properties;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final String workerId;

    private final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices;

    private final Collection<Camunda8BusinessCockpitService<?>> connectableCockpitServices;

    private final Camunda8WorkflowEventHandler workflowEventHandler;

    private final Optional<Configuration> templating;

    private final Camunda8VanillaBpProperties camunda8Properties;

    private final ObjectProvider<Camunda8WorkflowHandler> workflowHandlers;

    private final SpringDataUtil springDataUtil;

    private final Method noopWorkflowDetailsMethod;

    private final Map<String, JobWorkerBuilderStep1.JobWorkerBuilderStep3> workers = new HashMap<>();

    private CamundaClient client;

    public Camunda8WorkflowWiring(
            final ApplicationContext applicationContext,
            final ApplicationEventPublisher applicationEventPublisher,
            final String workerId,
            final VanillaBpCockpitProperties properties,
            final Camunda8VanillaBpProperties camunda8Properties,
            final SpringBeanUtil springBeanUtil,
            final SpringDataUtil springDataUtil,
            final WorkflowMethodParameterFactory methodParameterFactory,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Collection<Camunda8BusinessCockpitService<?>> connectableCockpitServices,
            final Optional<Configuration> templating,
            final Camunda8WorkflowEventHandler workflowEventHandler,
            final WorkflowModulePublishing workflowModulePublishing,
            final ObjectProvider<Camunda8WorkflowHandler> workflowHandlers) throws Exception {

        super(applicationContext, springBeanUtil, methodParameterFactory, workflowModulePublishing);
        this.applicationEventPublisher = applicationEventPublisher;
        this.workerId = workerId;
        this.properties = properties;
        this.springDataUtil = springDataUtil;
        this.connectableServices = connectableServices;
        this.connectableCockpitServices = connectableCockpitServices;
        this.workflowEventHandler = workflowEventHandler;
        this.templating = templating;
        this.camunda8Properties = camunda8Properties;
        this.workflowHandlers = workflowHandlers;

        noopWorkflowDetailsMethod = getClass().getMethod("noopWorkflowDetailsMethod", PrefilledWorkflowDetails.class);

    }

    @Override
    public void accept(
            final CamundaClient camundaClient) {
        this.client = camundaClient;
    }

    @EventListener
    public void openWorkers(
            final ModuleAwareBpmnDeployment.BpmnModelCacheProcessed event) {

        workflowEventHandler.updateVersionInfos(event);

        workers
                .values()
                .forEach(JobWorkerBuilderStep1.JobWorkerBuilderStep3::open);

    }

    public Camunda8BusinessCockpitService<?> wireService(
            final String workflowModuleId,
            final Camunda8WorkflowConnectable camunda8WorkflowConnectable) {
        return super.wireService(workflowModuleId, camunda8WorkflowConnectable.getBpmnProcessId());
    }

    public void wireWorkflow(String workflowModuleId, Camunda8WorkflowConnectable connectable) {
        String bpmnProcessId = connectable.getBpmnProcessId();
        
        final var workflowAggregateAndServiceClass =
                determineAndValidateWorkflowAggregateAndServiceClass(bpmnProcessId);
        final var workflowAggregateClass = workflowAggregateAndServiceClass.getKey();
        final var processService = connectableServices.get(workflowAggregateClass);

        superWireWorkflow(
                workflowModuleId,
                bpmnProcessId,
                processService,
                connectable,
                workflowAggregateClass,
                true,
                (method, annotation) -> validateParameters(workflowAggregateClass, method),
                (bean, method, parameters) -> connectToBpms(workflowModuleId, processService, connectable, bean, method, parameters)
        );

    }

    @Override
    protected Camunda8BusinessCockpitService<?> connectToBpms(
            String workflowModuleId,
            Class<?> workflowAggregateClass,
            String bpmnProcessId,
            boolean isPrimary) {

        final var bcService = connectableCockpitServices
                .stream()
                .filter(service -> service.getWorkflowAggregateClass().equals(workflowAggregateClass))
                .findFirst()
                .orElse(null);

        // BusinessCockpitService is not required
        if (bcService != null) {
            bcService.wire(
                    this.client,
                    workflowModuleId,
                    bpmnProcessId,
                    isPrimary);
        }

        return bcService;
    }

    // from io.vanillabp.springboot.adapter.wiring.AbstractTaskWiring.wireTask(T connectable, boolean allowNoMethodFound, BiFunction<Method, A, Boolean> methodMatchesTaskDefinition, BiFunction<Method, A, Boolean> methodMatchesElementId, BiFunction<Method, A, List<MethodParameter>> validateParameters, ConnectBean connect)
    protected void superWireWorkflow(
            String workflowModuleId,
            String bpmnProcessId,
            AdapterAwareProcessService<?> processService,
            Camunda8WorkflowConnectable connectable,
            Class<?> workflowAggregateClass,
            boolean allowNoMethodFound,
            BiFunction<Method, Annotation, List<MethodParameter>> validateParameters,
            ConnectBean connect
    ) {

        StringBuilder foundMethodNames = new StringBuilder();
        AtomicInteger matchingMethods = new AtomicInteger(0);

        this.applicationContext
                .getBeansWithAnnotation(WorkflowService.class)
                .entrySet()
                .stream()
                .filter((bean) -> this.isAboutConnectableProcess(bpmnProcessId, bean.getValue()))
                .forEach((bean) -> this.connectWorkflowDetailProviderBean(
                        foundMethodNames,
                        matchingMethods,
                        bean.getKey(),
                        bean.getValue(),
                        validateParameters,
                        connect));

        if (matchingMethods.get() == 1) {
            return;
        }

        if (matchingMethods.get() > 1) {
            throw new RuntimeException(
                    "More than one method annotated with @"
                            + getAnnotationType().getName()
                            + " is for process '"
                            + bpmnProcessId
                            + "': "
                            + foundMethodNames);
        }

        if (!allowNoMethodFound) {
            throw new RuntimeException(
                    "No public method annotated with @"
                            + getAnnotationType().getName()
                            + " is matching process '"
                            + bpmnProcessId
                            + "'");
        }

        final var noopWorkflowDetailsMethodParameters = validateParameters(
                workflowAggregateClass,
                noopWorkflowDetailsMethod);

        connectToBpms(
                workflowModuleId,
                processService,
                connectable,
                this,
                noopWorkflowDetailsMethod,
                noopWorkflowDetailsMethodParameters);

    }

    protected void connectWorkflowDetailProviderBean(
            StringBuilder foundMethodNames,
            AtomicInteger matchingMethods,
            String beanName,
            Object bean,
            BiFunction<Method, Annotation, List<MethodParameter>> validateParameters,
            ConnectBean connect) {
        Class<?> beanClass = this.determineBeanClass(bean);
        Arrays.stream(beanClass.getMethods()).flatMap((method) -> {
            return Arrays.stream(method.getAnnotationsByType(this.getAnnotationType())).map((annotation) -> {
                return Map.entry(method, annotation);
            });
        }).peek((m) -> {
            if (foundMethodNames.length() > 0) {
                foundMethodNames.append(", ");
            }

            foundMethodNames.append(((Method)m.getKey()).toString());
        }).filter((m) -> {
            return matchingMethods.getAndIncrement() == 0;
        }).map((m) -> {
            return Map.entry((Method)m.getKey(), validateParameters.apply((Method)m.getKey(), (Annotation)m.getValue()));
        }).forEach((m) -> {
            connect.connect(bean, (Method)m.getKey(), m.getValue());
        });
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
                super::validatePrefilledWorkflowDetails);

    }

    private void connectToBpms(
            final String workflowModuleId,
            final AdapterAwareProcessService<?> processService,
            final Camunda8WorkflowConnectable connectable,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {

        if (!WorkflowDetails.class.isAssignableFrom(method.getReturnType())) {
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

        final var jobType = Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER + connectable.getTaskDefinition();
        if (workers.containsKey(jobType)) {
            return;
        }

        final var aggregateIdPropertyName = springDataUtil.getIdName(processService.getWorkflowAggregateClass());

        final var workflowHandler = workflowHandlers.getObject(
                properties,
                applicationEventPublisher,
                processService,
                connectable.getBpmnProcessId(),
                connectable.getVersionInfo(),
                connectable.getBpmnProcessName(),
                templating,
                aggregateIdPropertyName,
                processService.getWorkflowAggregateRepository(),
                bean,
                method,
                parameters,
                client);

        workflowEventHandler.addWorkflowHandler(connectable, workflowHandler);

        final var worker = client
                .newWorker()
                .jobType(jobType)
                .handler(workflowEventHandler)
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

    @Override
    protected String getWorkflowModuleUri(
            final String workflowModuleId) {

        return properties.getWorkflowModuleUri(workflowModuleId);

    }

    @Override
    protected String getTaskProviderApiUriPath(
            final String workflowModuleId) {

        return null;

    }

    @Override
    protected String getWorkflowProviderApiUriPath(
            final String workflowModuleId) {

        return null;

    }

    public WorkflowDetails noopWorkflowDetailsMethod(
            final PrefilledWorkflowDetails prefilledWorkflowDetails) {

        return prefilledWorkflowDetails;

    }

}
