package io.vanillabp.cockpit.adapter.common.wiring.parameters;

import io.vanillabp.springboot.parameters.MethodParameterFactory;

public class UserTaskMethodParameterFactory extends MethodParameterFactory {

    public PrefilledUserTaskDetailsMethodParameter getPrefilledUserTaskDetailsParameter(
            final int index,
            final String parameter) {

        return new PrefilledUserTaskDetailsMethodParameter(
                index,
                parameter);

    }

}
