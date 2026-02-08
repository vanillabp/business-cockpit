package io.vanillabp.cockpit.adapter.camunda8.usertask.publishing;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessUserTaskAfterTransactionEventTest {

    @Test
    void constructor_withSource_createsInstance() {
        // Create event with source
        final var source = new Object();
        final var event = new ProcessUserTaskAfterTransactionEvent(source);

        // Should be created successfully
        assertThat(event).isNotNull();
        assertThat(event.getSource()).isSameAs(source);
    }

    @Test
    void extendsApplicationEvent() {
        // Create instance
        final var event = new ProcessUserTaskAfterTransactionEvent(new Object());

        // Should extend ApplicationEvent
        assertThat(event).isInstanceOf(ApplicationEvent.class);
    }

    @Test
    void getTimestamp_returnsNonZero() {
        // Create event
        final var event = new ProcessUserTaskAfterTransactionEvent(new Object());

        // Timestamp should be set
        assertThat(event.getTimestamp()).isGreaterThan(0);
    }

}
