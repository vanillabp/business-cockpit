package io.vanillabp.cockpit.adapter.common.wiring.parameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTaskMethodParameterFactoryTest {

    private UserTaskMethodParameterFactory factory;

    @BeforeEach
    void setUp() {
        factory = new UserTaskMethodParameterFactory();
    }

    @Test
    void getPrefilledUserTaskDetailsParameter_createsParameterWithCorrectIndex() {
        PrefilledUserTaskDetailsMethodParameter param = factory.getPrefilledUserTaskDetailsParameter(0, "param1");

        assertThat(param).isNotNull();
        assertThat(param.getIndex()).isEqualTo(0);
    }

    @Test
    void getPrefilledUserTaskDetailsParameter_createsParameterWithCorrectName() {
        PrefilledUserTaskDetailsMethodParameter param = factory.getPrefilledUserTaskDetailsParameter(1, "testParam");

        assertThat(param).isNotNull();
        assertThat(param.getParameter()).isEqualTo("testParam");
    }

    @Test
    void getPrefilledUserTaskDetailsParameter_createsCorrectType() {
        PrefilledUserTaskDetailsMethodParameter param = factory.getPrefilledUserTaskDetailsParameter(2, "details");

        assertThat(param).isInstanceOf(PrefilledUserTaskDetailsMethodParameter.class);
    }

    @Test
    void getDetailsEventParameter_createsParameterWithCorrectIndex() {
        DetailsEventMethodParameter param = factory.getDetailsEventParameter(0, "event");

        assertThat(param).isNotNull();
        assertThat(param.getIndex()).isEqualTo(0);
    }

    @Test
    void getDetailsEventParameter_createsParameterWithCorrectName() {
        DetailsEventMethodParameter param = factory.getDetailsEventParameter(3, "eventParam");

        assertThat(param).isNotNull();
        assertThat(param.getParameter()).isEqualTo("eventParam");
    }

    @Test
    void getDetailsEventParameter_createsCorrectType() {
        DetailsEventMethodParameter param = factory.getDetailsEventParameter(1, "event");

        assertThat(param).isInstanceOf(DetailsEventMethodParameter.class);
    }

    @Test
    void factoryCanCreateMultipleParameters() {
        PrefilledUserTaskDetailsMethodParameter details1 = factory.getPrefilledUserTaskDetailsParameter(0, "d1");
        PrefilledUserTaskDetailsMethodParameter details2 = factory.getPrefilledUserTaskDetailsParameter(1, "d2");
        DetailsEventMethodParameter event = factory.getDetailsEventParameter(2, "e1");

        assertThat(details1.getIndex()).isEqualTo(0);
        assertThat(details2.getIndex()).isEqualTo(1);
        assertThat(event.getIndex()).isEqualTo(2);
    }
}
