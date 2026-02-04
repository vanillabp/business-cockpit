package io.vanillabp.cockpit.commons.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncPropertiesTest {

    @Test
    void defaultValues_areSetCorrectly() {
        AsyncProperties properties = new AsyncProperties();

        assertThat(properties.getCorePoolSize()).isEqualTo(2);
        assertThat(properties.getMaxPoolSize()).isEqualTo(50);
        assertThat(properties.getQueueCapacity()).isEqualTo(5);
    }

    @Test
    void setCorePoolSize_updatesValue() {
        AsyncProperties properties = new AsyncProperties();
        properties.setCorePoolSize(10);

        assertThat(properties.getCorePoolSize()).isEqualTo(10);
    }

    @Test
    void setMaxPoolSize_updatesValue() {
        AsyncProperties properties = new AsyncProperties();
        properties.setMaxPoolSize(100);

        assertThat(properties.getMaxPoolSize()).isEqualTo(100);
    }

    @Test
    void setQueueCapacity_updatesValue() {
        AsyncProperties properties = new AsyncProperties();
        properties.setQueueCapacity(20);

        assertThat(properties.getQueueCapacity()).isEqualTo(20);
    }
}
