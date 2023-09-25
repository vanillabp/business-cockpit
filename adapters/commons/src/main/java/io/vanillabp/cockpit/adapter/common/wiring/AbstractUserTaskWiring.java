package io.vanillabp.cockpit.adapter.common.wiring;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.context.ApplicationContext;

import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.springboot.adapter.Connectable;
import io.vanillabp.springboot.adapter.wiring.AbstractTaskWiring;
import io.vanillabp.springboot.parameters.MethodParameter;

public abstract class AbstractUserTaskWiring<T extends Connectable, M extends UserTaskMethodParameterFactory>
        extends AbstractTaskWiring<T, UserTaskDetailsProvider, M> {

    public AbstractUserTaskWiring(
            final ApplicationContext applicationContext,
            final M methodParameterFactory) {

        super(applicationContext, methodParameterFactory);
        
    }
    
    @Override
    protected Class<UserTaskDetailsProvider> getAnnotationType() {
        
        return UserTaskDetailsProvider.class;
        
    }

    protected MethodParameter validatePrefilledUserTaskDetails(
            final Method method,
            final Parameter parameter,
            final int index) {

        if (!parameter.getType().equals(PrefilledUserTaskDetails.class)) {
            return null;
        }
        
        return methodParameterFactory
                .getPrefilledUserTaskDetailsParameter(
                        index,
                        parameter.getName());
        
    }

}
