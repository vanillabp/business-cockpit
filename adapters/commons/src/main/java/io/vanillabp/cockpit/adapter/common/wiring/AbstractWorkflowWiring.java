package io.vanillabp.cockpit.adapter.common.wiring;

import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.springboot.adapter.Connectable;
import io.vanillabp.springboot.adapter.SpringBeanUtil;
import io.vanillabp.springboot.adapter.wiring.AbstractTaskWiring;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public abstract class AbstractWorkflowWiring<T extends Connectable, M extends WorkflowMethodParameterFactory, BCS extends BusinessCockpitServiceImplementation<?>>
        extends AbstractTaskWiring<T, WorkflowDetailsProvider, M> {

    public AbstractWorkflowWiring(
            final ApplicationContext applicationContext,
            final SpringBeanUtil springBeanUtil,
            final M methodParameterFactory) {

        super(applicationContext, springBeanUtil, methodParameterFactory);
        
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

        return connectToBpms(
                workflowModuleId,
                workflowAggregateClass,
                bpmnProcessId,
                isPrimaryProcessWiring);
        
    }
    
}