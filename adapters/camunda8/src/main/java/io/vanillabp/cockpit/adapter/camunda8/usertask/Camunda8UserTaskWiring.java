package io.vanillabp.cockpit.adapter.camunda8.usertask;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8Connectable;
import io.vanillabp.cockpit.adapter.common.CockpitCommonAdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.wiring.AbstractUserTaskWiring;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Camunda8UserTaskWiring extends AbstractUserTaskWiring<Camunda8Connectable, UserTaskMethodParameterFactory> {

    private final CockpitProperties properties;

    private final UserTasksWorkflowProperties workflowsCockpitProperties;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final Optional<Configuration> templating;

    private final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices;

    private final Camunda8UserTaskEventHandler userTaskEventHandler;

    private final Method noopUserTaskMethod;

    public Camunda8UserTaskWiring(
            final ApplicationContext applicationContext,
            final CockpitProperties properties,
            final UserTasksWorkflowProperties workflowsCockpitProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Camunda8UserTaskEventHandler userTaskEventHandler) throws Exception {
        
        super(applicationContext, new UserTaskMethodParameterFactory());
        this.connectableServices = connectableServices;
        this.properties = properties;
        this.workflowsCockpitProperties = workflowsCockpitProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.templating = templating;
        this.userTaskEventHandler = userTaskEventHandler;

        noopUserTaskMethod = getClass().getMethod("noopUserTaskMethod", PrefilledUserTaskDetails.class);

    }
    
    public void wireTask(
            final String workflowModuleId,
            final Camunda8Connectable connectable) {

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
    
    protected boolean methodMatchesTaskDefinition(
            final Camunda8Connectable connectable,
            final Method method,
            final UserTaskDetailsProvider annotation) {

        return super.methodMatchesTaskDefinition(connectable, method, annotation) ||
                annotation.taskDefinition().equals(connectable.getTaskDefinition());

    }
    
    private void connectToBpms(
            final String workflowModuleId,
            final AdapterAwareProcessService<?> processService,
            final Object bean,
            final Camunda8Connectable connectable,
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
        
        final var repository = processService.getWorkflowAggregateRepository();

        final var userTasksProperties = new UserTasksProperties[1];
        final var userTaskProperties = workflowsCockpitProperties
                .getWorkflows()
                .stream()
                .filter(props -> props.matches(workflowModuleId, connectable.getBpmnProcessId()))
                .findFirst()
                .map(p -> {
                    userTasksProperties[0] = p;
                    return p;
                })
                .get()
                .getUserTasks()
                .get(connectable.getTaskDefinition());

        Camunda8UserTaskHandler taskHandler = new Camunda8UserTaskHandler(
                connectable.getTaskDefinition(),
                properties,
                userTasksProperties[0],
                userTaskProperties,
                applicationEventPublisher,
                templating,
                connectable.getBpmnProcessId(),
                processService,
                (CrudRepository<Object, Object>) repository,
                bean,
                method,
                parameters
        );
        userTaskEventHandler.addTaskHandler(connectable, taskHandler);
        
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
