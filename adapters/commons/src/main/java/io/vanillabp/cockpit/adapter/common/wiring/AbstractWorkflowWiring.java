package io.vanillabp.cockpit.adapter.common.wiring;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.spi.cockpit.WorkflowDetailsProvider;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.springboot.adapter.Connectable;
import io.vanillabp.springboot.adapter.wiring.AbstractTaskWiring;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.springframework.context.ApplicationContext;

public abstract class AbstractWorkflowWiring<T extends Connectable, M extends WorkflowMethodParameterFactory>
        extends AbstractTaskWiring<T, WorkflowDetailsProvider, M> {

    public AbstractWorkflowWiring(
            final ApplicationContext applicationContext,
            final M methodParameterFactory) {

        super(applicationContext, methodParameterFactory);
        
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

}
