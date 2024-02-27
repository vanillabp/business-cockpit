package io.vanillabp.cockpit.adapter.camunda7.usertask;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.wiring.AbstractUserTaskWiring;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.SpringBeanUtil;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Camunda7UserTaskWiring extends AbstractUserTaskWiring<Camunda7Connectable, UserTaskMethodParameterFactory> {

    private final VanillaBpCockpitProperties properties;

    private final ApplicationEventPublisher applicationEventPublisher;
    
    private final Optional<Configuration> templating;
    
    private final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices;
    
    private final Camunda7UserTaskEventHandler userTaskEventHandler;

    private final Method noopUserTaskMethod;
    
    public Camunda7UserTaskWiring(
            final ApplicationContext applicationContext,
            final SpringBeanUtil springBeanUtil,
            final VanillaBpCockpitProperties properties,
            final ApplicationEventPublisher applicationEventPublisher,
            final Optional<Configuration> templating,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final UserTaskMethodParameterFactory methodParameterFactory,
            final Camunda7UserTaskEventHandler userTaskEventHandler) throws Exception {
        
        super(applicationContext, springBeanUtil, methodParameterFactory);
        this.connectableServices = connectableServices;
        this.properties = properties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.templating = templating;
        this.userTaskEventHandler = userTaskEventHandler;

        noopUserTaskMethod = getClass().getMethod("noopUserTaskMethod", PrefilledUserTaskDetails.class);

    }
    
    public boolean wireTask(
            final String workflowModuleId,
            final Camunda7Connectable connectable) {

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

        return true;
        
    }
    
    protected boolean methodMatchesTaskDefinition(
            final Camunda7Connectable connectable,
            final Method method,
            final UserTaskDetailsProvider annotation) {
        
        if (super.methodMatchesTaskDefinition(connectable, method, annotation)) {
            return true;
        }

        if (annotation.taskDefinition().equals(connectable.getTaskDefinition())) {
            return true;
        }

        return false;
        
    }
    
    private void connectToBpms(
            final String workflowModuleId,
            final AdapterAwareProcessService<?> processService,
            final Object bean,
            final Camunda7Connectable connectable,
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
                
        @SuppressWarnings("unchecked")
        final var taskHandler = new Camunda7UserTaskHandler(
                connectable.getTaskDefinition(),
                properties,
                applicationEventPublisher,
                templating,
                connectable.getBpmnProcessId(),
                processService,
                (CrudRepository<Object, Object>) repository,
                bean,
                method,
                parameters);
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
