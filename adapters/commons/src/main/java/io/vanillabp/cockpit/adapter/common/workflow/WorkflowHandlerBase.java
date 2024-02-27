package io.vanillabp.cockpit.adapter.common.workflow;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.wiring.TemplatingHandlerBase;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.PrefilledWorkflowDetailsMethodParameter;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class WorkflowHandlerBase extends TemplatingHandlerBase {

    public WorkflowHandlerBase(
            final VanillaBpCockpitProperties properties,
            final Optional<Configuration> templating,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        
        super(properties, templating, workflowAggregateRepository, bean, method, parameters);
        
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
