package io.vanillabp.cockpit.adapter.common.wiring.parameters;

import io.vanillabp.springboot.parameters.MethodParameterFactory;

public class WorkflowMethodParameterFactory extends MethodParameterFactory {

    public PrefilledWorkflowDetailsMethodParameter getPrefilledWorkflowDetailsParameter(
            final int index,
            final String parameter) {

        return new PrefilledWorkflowDetailsMethodParameter(
                index,
                parameter);

    }

}
