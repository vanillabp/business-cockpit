package io.vanillabp.cockpit.commons.rest.adapter;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RetryTest {

    @Test
    void defaultValues_areSetCorrectly() {
        Retry retry = new Retry();

        assertThat(retry.isEnabled()).isFalse();
        assertThat(retry.getMaxAttempts()).isEqualTo(5);
        assertThat(retry.getPeriod()).isEqualTo(Duration.ofMillis(100));
        assertThat(retry.getMaxPeriod()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void setEnabled_toTrue_updatesValue() {
        Retry retry = new Retry();
        retry.setEnabled(true);

        assertThat(retry.isEnabled()).isTrue();
    }

    @Test
    void setMaxAttempts_updatesValue() {
        Retry retry = new Retry();
        retry.setMaxAttempts(10);

        assertThat(retry.getMaxAttempts()).isEqualTo(10);
    }

    @Test
    void setPeriod_updatesValue() {
        Retry retry = new Retry();
        retry.setPeriod(Duration.ofMillis(500));

        assertThat(retry.getPeriod()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void setMaxPeriod_updatesValue() {
        Retry retry = new Retry();
        retry.setMaxPeriod(Duration.ofSeconds(5));

        assertThat(retry.getMaxPeriod()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void fullConfiguration_worksCorrectly() {
        Retry retry = new Retry();
        retry.setEnabled(true);
        retry.setMaxAttempts(3);
        retry.setPeriod(Duration.ofMillis(200));
        retry.setMaxPeriod(Duration.ofSeconds(2));

        assertThat(retry.isEnabled()).isTrue();
        assertThat(retry.getMaxAttempts()).isEqualTo(3);
        assertThat(retry.getPeriod()).isEqualTo(Duration.ofMillis(200));
        assertThat(retry.getMaxPeriod()).isEqualTo(Duration.ofSeconds(2));
    }
}
