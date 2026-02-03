package io.vanillabp.cockpit.adapter.common.wiring.parameters;

import io.vanillabp.springboot.parameters.MethodParameter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrefilledWorkflowDetailsMethodParameterTest {

    @Test
    void constructor_setsIndexAndParameter() {
        PrefilledWorkflowDetailsMethodParameter param = new PrefilledWorkflowDetailsMethodParameter(3, "workflowParam");

        assertThat(param.getIndex()).isEqualTo(3);
        assertThat(param.getParameter()).isEqualTo("workflowParam");
    }

    @Test
    void extendsMethodParameter() {
        PrefilledWorkflowDetailsMethodParameter param = new PrefilledWorkflowDetailsMethodParameter(0, "param");

        assertThat(param).isInstanceOf(MethodParameter.class);
    }

    @Test
    void canCreateWithZeroIndex() {
        PrefilledWorkflowDetailsMethodParameter param = new PrefilledWorkflowDetailsMethodParameter(0, "first");

        assertThat(param.getIndex()).isEqualTo(0);
        assertThat(param.getParameter()).isEqualTo("first");
    }

    @Test
    void canCreateWithLargeIndex() {
        PrefilledWorkflowDetailsMethodParameter param = new PrefilledWorkflowDetailsMethodParameter(999, "large");

        assertThat(param.getIndex()).isEqualTo(999);
    }

    @Test
    void canCreateWithEmptyParameterName() {
        PrefilledWorkflowDetailsMethodParameter param = new PrefilledWorkflowDetailsMethodParameter(2, "");

        assertThat(param.getParameter()).isEmpty();
    }
}
