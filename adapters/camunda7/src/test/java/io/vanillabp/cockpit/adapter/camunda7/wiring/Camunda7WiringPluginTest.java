package io.vanillabp.cockpit.adapter.camunda7.wiring;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Camunda7WiringPlugin}.
 *
 * Note: The preInit method is tested via integration tests since it depends
 * on the actual Camunda engine version at runtime.
 */
@ExtendWith(MockitoExtension.class)
class Camunda7WiringPluginTest {

    @Mock
    private WiringBpmnParseListener wiringBpmnParseListener;

    @Mock
    private Camunda7HistoryEventProducerSupplier historyEventProducerSupplier;

    @Mock
    private ProcessEngineConfigurationImpl processEngineConfiguration;

    @Mock
    private HistoryEventProducer historyEventProducer;

    private Camunda7WiringPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new Camunda7WiringPlugin(wiringBpmnParseListener, historyEventProducerSupplier);
    }

    @Test
    void constructor_setsFields() {
        // Assert - plugin was created without exception
        assertThat(plugin).isNotNull();
    }

    @Test
    void postInit_setsHistoryEventProducer() {
        // Arrange
        when(processEngineConfiguration.getHistoryEventProducer()).thenReturn(historyEventProducer);

        // Act
        plugin.postInit(processEngineConfiguration);

        // Assert
        verify(historyEventProducerSupplier).setHistoryEventProducer(historyEventProducer);
    }

    @Test
    void postInit_setsNullHistoryEventProducerIfNotConfigured() {
        // Arrange
        when(processEngineConfiguration.getHistoryEventProducer()).thenReturn(null);

        // Act
        plugin.postInit(processEngineConfiguration);

        // Assert
        verify(historyEventProducerSupplier).setHistoryEventProducer(null);
    }
}
