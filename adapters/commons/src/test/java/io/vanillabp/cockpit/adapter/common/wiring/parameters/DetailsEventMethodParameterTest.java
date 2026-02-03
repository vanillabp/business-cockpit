package io.vanillabp.cockpit.adapter.common.wiring.parameters;

import io.vanillabp.springboot.parameters.MethodParameter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DetailsEventMethodParameterTest {

    @Test
    void constructor_setsIndexAndParameter() {
        DetailsEventMethodParameter param = new DetailsEventMethodParameter(7, "eventParam");

        assertThat(param.getIndex()).isEqualTo(7);
        assertThat(param.getParameter()).isEqualTo("eventParam");
    }

    @Test
    void extendsMethodParameter() {
        DetailsEventMethodParameter param = new DetailsEventMethodParameter(0, "param");

        assertThat(param).isInstanceOf(MethodParameter.class);
    }

    @Test
    void canCreateWithZeroIndex() {
        DetailsEventMethodParameter param = new DetailsEventMethodParameter(0, "firstEvent");

        assertThat(param.getIndex()).isEqualTo(0);
        assertThat(param.getParameter()).isEqualTo("firstEvent");
    }

    @Test
    void canCreateWithLargeIndex() {
        DetailsEventMethodParameter param = new DetailsEventMethodParameter(50, "largeIndex");

        assertThat(param.getIndex()).isEqualTo(50);
    }

    @Test
    void canCreateWithEmptyParameterName() {
        DetailsEventMethodParameter param = new DetailsEventMethodParameter(1, "");

        assertThat(param.getParameter()).isEmpty();
    }
}
