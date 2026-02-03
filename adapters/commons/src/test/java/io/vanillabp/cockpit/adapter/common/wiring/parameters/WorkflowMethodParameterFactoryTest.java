package io.vanillabp.cockpit.adapter.common.wiring.parameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowMethodParameterFactoryTest {

    private WorkflowMethodParameterFactory factory;

    @BeforeEach
    void setUp() {
        factory = new WorkflowMethodParameterFactory();
    }

    @Test
    void getPrefilledWorkflowDetailsParameter_createsParameterWithCorrectIndex() {
        PrefilledWorkflowDetailsMethodParameter param = factory.getPrefilledWorkflowDetailsParameter(0, "param1");

        assertThat(param).isNotNull();
        assertThat(param.getIndex()).isEqualTo(0);
    }

    @Test
    void getPrefilledWorkflowDetailsParameter_createsParameterWithCorrectName() {
        PrefilledWorkflowDetailsMethodParameter param = factory.getPrefilledWorkflowDetailsParameter(1, "workflowDetails");

        assertThat(param).isNotNull();
        assertThat(param.getParameter()).isEqualTo("workflowDetails");
    }

    @Test
    void getPrefilledWorkflowDetailsParameter_createsCorrectType() {
        PrefilledWorkflowDetailsMethodParameter param = factory.getPrefilledWorkflowDetailsParameter(2, "details");

        assertThat(param).isInstanceOf(PrefilledWorkflowDetailsMethodParameter.class);
    }

    @Test
    void factoryCanCreateMultipleParameters() {
        PrefilledWorkflowDetailsMethodParameter param1 = factory.getPrefilledWorkflowDetailsParameter(0, "w1");
        PrefilledWorkflowDetailsMethodParameter param2 = factory.getPrefilledWorkflowDetailsParameter(1, "w2");
        PrefilledWorkflowDetailsMethodParameter param3 = factory.getPrefilledWorkflowDetailsParameter(2, "w3");

        assertThat(param1.getIndex()).isEqualTo(0);
        assertThat(param2.getIndex()).isEqualTo(1);
        assertThat(param3.getIndex()).isEqualTo(2);
        assertThat(param1.getParameter()).isEqualTo("w1");
        assertThat(param2.getParameter()).isEqualTo("w2");
        assertThat(param3.getParameter()).isEqualTo("w3");
    }
}
