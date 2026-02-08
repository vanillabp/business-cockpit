package io.vanillabp.cockpit.adapter.camunda8.usertask;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda8UserTaskWiringTest {

    @Test
    void jobTypeDetailsProvider_hasCorrectValue() {
        // Verify the JOBTYPE constant
        assertThat(Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER)
                .isEqualTo("io.vanillabp.businesscockpit:");
    }

}
