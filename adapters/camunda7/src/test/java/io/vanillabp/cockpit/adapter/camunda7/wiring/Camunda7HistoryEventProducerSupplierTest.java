package io.vanillabp.cockpit.adapter.camunda7.wiring;

import org.camunda.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link Camunda7HistoryEventProducerSupplier}.
 */
class Camunda7HistoryEventProducerSupplierTest {

    private Camunda7HistoryEventProducerSupplier supplier;

    @BeforeEach
    void setUp() {
        supplier = new Camunda7HistoryEventProducerSupplier();
    }

    @Test
    void getHistoryEventProducer_returnsNullInitially() {
        // Act
        HistoryEventProducer result = supplier.getHistoryEventProducer();

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void setHistoryEventProducer_setsProducer() {
        // Arrange
        HistoryEventProducer mockProducer = mock(HistoryEventProducer.class);

        // Act
        supplier.setHistoryEventProducer(mockProducer);

        // Assert
        assertThat(supplier.getHistoryEventProducer()).isSameAs(mockProducer);
    }

    @Test
    void setHistoryEventProducer_canSetNull() {
        // Arrange
        HistoryEventProducer mockProducer = mock(HistoryEventProducer.class);
        supplier.setHistoryEventProducer(mockProducer);

        // Act
        supplier.setHistoryEventProducer(null);

        // Assert
        assertThat(supplier.getHistoryEventProducer()).isNull();
    }

    @Test
    void setHistoryEventProducer_canReplaceProducer() {
        // Arrange
        HistoryEventProducer producer1 = mock(HistoryEventProducer.class);
        HistoryEventProducer producer2 = mock(HistoryEventProducer.class);
        supplier.setHistoryEventProducer(producer1);

        // Act
        supplier.setHistoryEventProducer(producer2);

        // Assert
        assertThat(supplier.getHistoryEventProducer()).isSameAs(producer2);
    }
}
