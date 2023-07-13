package io.vanillabp.cockpit.adapter.common.workflow;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import io.vanillabp.cockpit.adapter.common.wiring.parameters.PrefilledWorkflowDetailsMethodParameter;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.springboot.adapter.TaskHandlerBase;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.springframework.data.repository.CrudRepository;

public abstract class WorkflowHandlerBase extends TaskHandlerBase {

    public WorkflowHandlerBase(
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        
        super(workflowAggregateRepository, bean, method, parameters);
        
    }

    protected boolean processPrefilledWorkflowDetailsParameter(
            final Object[] args,
            final MethodParameter param,
            final Supplier<PrefilledWorkflowDetails> prefilledWorkflowDetailsSupplier) {

        if (!(param instanceof PrefilledWorkflowDetailsMethodParameter)) {
            return true;
        }

        args[param.getIndex()] = prefilledWorkflowDetailsSupplier.get();
        
        return false;
        
    }
    
}
