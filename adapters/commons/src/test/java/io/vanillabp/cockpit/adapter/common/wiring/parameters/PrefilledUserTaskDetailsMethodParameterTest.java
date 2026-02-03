package io.vanillabp.cockpit.adapter.common.wiring.parameters;

import io.vanillabp.springboot.parameters.MethodParameter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrefilledUserTaskDetailsMethodParameterTest {

    @Test
    void constructor_setsIndexAndParameter() {
        PrefilledUserTaskDetailsMethodParameter param = new PrefilledUserTaskDetailsMethodParameter(5, "testParam");

        assertThat(param.getIndex()).isEqualTo(5);
        assertThat(param.getParameter()).isEqualTo("testParam");
    }

    @Test
    void extendsMethodParameter() {
        PrefilledUserTaskDetailsMethodParameter param = new PrefilledUserTaskDetailsMethodParameter(0, "param");

        assertThat(param).isInstanceOf(MethodParameter.class);
    }

    @Test
    void canCreateWithZeroIndex() {
        PrefilledUserTaskDetailsMethodParameter param = new PrefilledUserTaskDetailsMethodParameter(0, "first");

        assertThat(param.getIndex()).isEqualTo(0);
        assertThat(param.getParameter()).isEqualTo("first");
    }

    @Test
    void canCreateWithLargeIndex() {
        PrefilledUserTaskDetailsMethodParameter param = new PrefilledUserTaskDetailsMethodParameter(100, "large");

        assertThat(param.getIndex()).isEqualTo(100);
    }

    @Test
    void canCreateWithEmptyParameterName() {
        PrefilledUserTaskDetailsMethodParameter param = new PrefilledUserTaskDetailsMethodParameter(1, "");

        assertThat(param.getParameter()).isEmpty();
    }
}
