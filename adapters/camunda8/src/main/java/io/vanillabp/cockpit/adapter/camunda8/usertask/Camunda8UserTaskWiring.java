package io.vanillabp.cockpit.adapter.camunda8.usertask;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceRepository;
import io.vanillabp.cockpit.adapter.common.CockpitCommonAdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.wiring.AbstractUserTaskWiring;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.SpringBeanUtil;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

public class Camunda8UserTaskWiring extends AbstractUserTaskWiring<Camunda8UserTaskConnectable, UserTaskMethodParameterFactory> {

    private final VanillaBpCockpitProperties vanillaBpCockpitProperties;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final Optional<Configuration> templating;

    private final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices;

    private final Camunda8UserTaskEventHandler userTaskEventHandler;

    private final ProcessInstanceRepository processInstanceRepository;

    private final Method noopUserTaskMethod;

    public Camunda8UserTaskWiring(
            final ApplicationContext applicationContext,
            final SpringBeanUtil springBeanUtil,
            final VanillaBpCockpitProperties vanillaBpCockpitProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            @Qualifier(CockpitCommonAdapterConfiguration.TEMPLATING_QUALIFIER)
            final Optional<Configuration> templating,
            final Map<Class<?>, AdapterAwareProcessService<?>> connectableServices,
            final Camunda8UserTaskEventHandler userTaskEventHandler,
            final ProcessInstanceRepository processInstanceRepository) throws Exception {
        
        super(applicationContext, springBeanUtil, new UserTaskMethodParameterFactory());
        this.connectableServices = connectableServices;
        this.vanillaBpCockpitProperties = vanillaBpCockpitProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.templating = templating;
        this.userTaskEventHandler = userTaskEventHandler;
        this.processInstanceRepository = processInstanceRepository;

        noopUserTaskMethod = getClass().getMethod("noopUserTaskMethod", PrefilledUserTaskDetails.class);

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

        CrudRepository<Object, Object> workflowAggregateRepository =
                (CrudRepository<Object, Object>) processService.getWorkflowAggregateRepository();


        Camunda8UserTaskHandler taskHandler = new Camunda8UserTaskHandler(
                connectable.getTaskDefinition(),
                vanillaBpCockpitProperties,
                applicationEventPublisher,
                templating,
                connectable.getBpmnProcessId(),
                connectable.getTitle(),
                processService,
                processInstanceRepository,
                workflowAggregateRepository,
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
