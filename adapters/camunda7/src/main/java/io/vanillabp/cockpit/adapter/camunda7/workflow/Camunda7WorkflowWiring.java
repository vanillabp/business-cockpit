package io.vanillabp.cockpit.adapter.camunda7.workflow;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7Connectable;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.wiring.AbstractWorkflowWiring;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.cockpit.commons.rest.adapter.versioning.ApiVersionAware;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.service.WorkflowService;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.wiring.ConnectBean;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

import freemarker.template.Configuration;

public class Camunda7WorkflowWiring extends AbstractWorkflowWiring<Camunda7Connectable, WorkflowMethodParameterFactory> {

    private final CockpitProperties cockpitProperties;
    
    private final UserTasksWorkflowProperties workflowsCockpitProperties;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices;

    private final ApiVersionAware bpmsApiVersionAware;

    private final Camunda7WorkflowEventSpringListener workflowEventListener;

    private final Optional<Configuration> templating;

    public Camunda7WorkflowWiring(
            ApplicationContext applicationContext,
            ApplicationEventPublisher applicationEventPublisher,
            CockpitProperties cockpitProperties,
            final UserTasksWorkflowProperties workflowsCockpitProperties,
            WorkflowMethodParameterFactory methodParameterFactory,
            Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            ApiVersionAware bpmsApiVersionAware,
            Optional<Configuration> templating,
            Camunda7WorkflowEventSpringListener workflowEventListener) {
        super(applicationContext, methodParameterFactory);
        this.cockpitProperties = cockpitProperties;
        this.workflowsCockpitProperties = workflowsCockpitProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.connectableServices = connectableServices;
        this.bpmsApiVersionAware = bpmsApiVersionAware;
        this.workflowEventListener = workflowEventListener;
        this.templating = templating;
    }

    public boolean wireWorkflow(String workflowModuleId, String bpmnProcessId) {
        final var workflowAggregateAndServiceClass =
                determineAndValidateWorkflowAggregateAndServiceClass(bpmnProcessId);
        final var workflowAggregateClass = workflowAggregateAndServiceClass.getKey();
        final var processService = connectableServices.get(workflowAggregateClass);

        final var methodFound = superWireWorkflow(
                bpmnProcessId,
                true,
                (method, annotation) -> validateParameters(workflowAggregateClass, method),
                (bean, method, parameters) -> connectToBpms(workflowModuleId, processService, bpmnProcessId, bean, method, parameters)
        );

        /*
        if (methodFound) {
            return true;
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
*/
        return true;
    }

    // from io.vanillabp.springboot.adapter.wiring.AbstractTaskWiring.wireTask(T connectable, boolean allowNoMethodFound, BiFunction<Method, A, Boolean> methodMatchesTaskDefinition, BiFunction<Method, A, Boolean> methodMatchesElementId, BiFunction<Method, A, List<MethodParameter>> validateParameters, ConnectBean connect)
    protected boolean superWireWorkflow(
            String bpmnProcessId,
            boolean allowNoMethodFound,
            BiFunction<Method, Annotation, List<MethodParameter>> validateParameters,
            ConnectBean connect
    ) {

        StringBuilder foundMethodNames = new StringBuilder();
        AtomicInteger matchingMethods = new AtomicInteger(0);

        this.applicationContext.getBeansWithAnnotation(WorkflowService.class).entrySet().stream().filter((bean) -> {
            return this.isAboutConnectableProcess(bpmnProcessId, bean.getValue());
        }).forEach((bean) -> {
            this.connectWorkflowDetailProviderBean(foundMethodNames, matchingMethods, (String)bean.getKey(), bean.getValue(), validateParameters, connect);
        });

        if (matchingMethods.get() > 1) {
            throw new RuntimeException(
                    "More than one method annotated with @"
                            + getAnnotationType().getName()
                            + " is for process '"
                            + bpmnProcessId
                            + "': "
                            + foundMethodNames);
        }

        if (matchingMethods.get() == 0) {
            if (allowNoMethodFound) {
                return false;
            }

            throw new RuntimeException(
                    "No public method annotated with @"
                            + getAnnotationType().getName()
                            + " is matching process '"
                            + bpmnProcessId
                            + "'");
        }

        return true;

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
            return Map.entry((Method)m.getKey(), (List)validateParameters.apply((Method)m.getKey(), (Annotation)m.getValue()));
        }).forEach((m) -> {
            connect.connect(bean, (Method)m.getKey(), (List)m.getValue());
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
            final String bpmnProcessId,
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

        final var workflowProperties = workflowsCockpitProperties
                .getWorkflows()
                .stream()
                .filter(props -> props.matches(workflowModuleId, bpmnProcessId))
                .findFirst()
                .get();

        @SuppressWarnings("unchecked")
        final var workflowHandler = new Camunda7WorkflowHandler(
                cockpitProperties,
                workflowProperties,
                applicationEventPublisher,
                bpmsApiVersionAware,
                processService,
                bpmnProcessId,
                templating,
                (CrudRepository<Object, Object>) processService.getWorkflowAggregateRepository(),
                bean,
                method,
                parameters);
        workflowEventListener.addWorkflowHandler(bpmnProcessId, workflowHandler);

    }
}
