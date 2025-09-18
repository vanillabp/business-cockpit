package io.vanillabp.cockpit.adapter.common.usertask;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.wiring.TemplatingHandlerBase;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.DetailsEventMethodParameter;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.PrefilledUserTaskDetailsMethodParameter;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.data.repository.CrudRepository;

public abstract class UserTaskHandlerBase extends TemplatingHandlerBase {

    public UserTaskHandlerBase(
            final VanillaBpCockpitProperties properties,
            final Optional<Configuration> templating,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        
        super(properties, templating, workflowAggregateRepository, bean, method, parameters);
        
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

    protected boolean processDetailsEventParameter(
            final Object[] args,
            final MethodParameter param,
            final Supplier<DetailsEvent.Event> detailsEventSupplier) {

        if (!(param instanceof DetailsEventMethodParameter)) {
            return true;
        }

        args[param.getIndex()] = detailsEventSupplier.get();

        return false;

    }

}
