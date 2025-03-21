package io.vanillabp.cockpit.adapter.common.wiring;

import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishing;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.springboot.adapter.Connectable;
import io.vanillabp.springboot.adapter.SpringBeanUtil;
import io.vanillabp.springboot.adapter.wiring.AbstractTaskWiring;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.ApplicationContext;

public abstract class AbstractWorkflowWiring<T extends Connectable, M extends WorkflowMethodParameterFactory, BCS extends BusinessCockpitServiceImplementation<?>>
        extends AbstractTaskWiring<T, WorkflowDetailsProvider, M> {

    private static final Set<String> registeredWorkflowModules = new HashSet<>();

    private final WorkflowModulePublishing workflowModulePublishing;

    public AbstractWorkflowWiring(
            final ApplicationContext applicationContext,
            final SpringBeanUtil springBeanUtil,
            final M methodParameterFactory,
            final WorkflowModulePublishing workflowModulePublishing) {

        super(applicationContext, springBeanUtil, methodParameterFactory);
        this.workflowModulePublishing = workflowModulePublishing;
        
    }
    
    @Override
    protected Class<WorkflowDetailsProvider> getAnnotationType() {
        
        return WorkflowDetailsProvider.class;
        
    }

    protected MethodParameter validatePrefilledWorkflowDetails(
            final Method method,
            final Parameter parameter,
            final int index) {

        if (!parameter.getType().equals(PrefilledWorkflowDetails.class)) {
            return null;
        }
        
        return methodParameterFactory
                .getPrefilledWorkflowDetailsParameter(
                        index,
                        parameter.getName());
        
    }

    protected abstract BCS connectToBpms(
            String workflowModuleId,
            Class<?> workflowAggregateClass,
            String bpmnProcessId,
            boolean isPrimary);
    
    public BCS wireService(
            final String workflowModuleId,
            final String bpmnProcessId) {

        final var workflowAggregateAndServiceClass =
                determineAndValidateWorkflowAggregateAndServiceClass(bpmnProcessId);
        final var workflowAggregateClass = workflowAggregateAndServiceClass.getKey();
        final var workflowServiceClass = workflowAggregateAndServiceClass.getValue();

        final var isPrimaryProcessWiring = isPrimaryProcessWiring(
                workflowModuleId,
                bpmnProcessId,
                workflowServiceClass);

        if (!registeredWorkflowModules.contains(workflowModuleId)) {
            registerWorkflowModule(workflowModuleId);
            registeredWorkflowModules.add(workflowModuleId);
        }

        final var service = connectToBpms(
                workflowModuleId,
                workflowAggregateClass,
                bpmnProcessId,
                isPrimaryProcessWiring);

        if (isPrimaryProcessWiring && (service == null)) {
            throw new RuntimeException(
                    "The workflow service\n   "
                    + workflowServiceClass.getName()
                    + "\nhas to auto-wire a property\n   BusinessProcessService<"
                    + workflowAggregateClass.getName()
                    + "> businessCockpitService\n(by using @Autowired or constructor injection)!"
                    + "Add this to the service even it is unused.");
        }

        return service;

    }

    private void registerWorkflowModule(
            final String workflowModuleId) {

        final var event = new RegisterWorkflowModuleEvent();
        event.setId(workflowModuleId);
        event.setUri(getWorkflowModuleUri(workflowModuleId));
        event.setTaskProviderApiUriPath(getTaskProviderApiUriPath(workflowModuleId));
        event.setWorkflowProviderApiUriPath(getWorkflowProviderApiUriPath(workflowModuleId));
        workflowModulePublishing.publish(event);

    }

    /*
     * Not used in case of workflows but derived from AbstractTaskWiring
     */
    @Override
    protected boolean methodMatchesElementId(
            final T connectable,
            final Method method,
            final WorkflowDetailsProvider annotation) {
        return false;
    }

    /*
     * Not used in case of workflows but derived from AbstractTaskWiring
     */
    @Override
    protected boolean methodMatchesTaskDefinition(
            final T connectable,
            final Method method,
            final WorkflowDetailsProvider annotation) {
        return false;
    }

    protected abstract String getWorkflowModuleUri(String workflowModuleId);

    protected abstract String getTaskProviderApiUriPath(String workflowModuleId);

    protected abstract String getWorkflowProviderApiUriPath(String workflowModuleId);
    
}