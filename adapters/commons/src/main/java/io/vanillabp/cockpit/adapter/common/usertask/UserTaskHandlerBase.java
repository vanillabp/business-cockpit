package io.vanillabp.cockpit.adapter.common.usertask;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.repository.CrudRepository;

import io.vanillabp.cockpit.adapter.common.wiring.parameters.PrefilledUserTaskDetailsMethodParameter;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.springboot.adapter.TaskHandlerBase;
import io.vanillabp.springboot.parameters.MethodParameter;

public abstract class UserTaskHandlerBase extends TaskHandlerBase {

    public UserTaskHandlerBase(
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        
        super(workflowAggregateRepository, bean, method, parameters);
        
    }

    protected boolean processPrefilledUserTaskDetailsParameter(
            final Object[] args,
            final MethodParameter param,
            final Supplier<PrefilledUserTaskDetails> prefilledUserTaskDetailsSupplier) {

        if (!(param instanceof PrefilledUserTaskDetailsMethodParameter)) {
            return true;
        }

        args[param.getIndex()] = prefilledUserTaskDetailsSupplier.get();
        
        return false;
        
    }
    
}
