package io.vanillabp.cockpit.simulator.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The broker properties the embedded Kafka of the simulator is configured with.
 *
 * <p>Every single one of them was a start-up failure or a dead port during the Spring Kafka 4.1 upgrade, and
 * none of them is checked by the compiler:
 *
 * <ul>
 * <li>{@code EmbeddedKafkaKraftBroker} runs broker and KRaft controller in one process. Overriding
 * {@code listeners} without a {@code CONTROLLER} entry makes Kafka refuse to start - "controller.listener.names
 * must contain at least one value appearing in the listeners configuration".</li>
 * <li>{@code KafkaClusterTestKit} pre-binds a socket for its own broker listener name, {@code EXTERNAL}, and
 * that port wins over the configured one. A fixed-port listener therefore needs a name of its own, otherwise
 * the broker silently listens somewhere else and {@code localhost:9092} - what the class promises and what
 * every run configuration uses - is closed.</li>
 * <li>{@code kafkaPorts(..)} is not a substitute: the broker stores the value and never reads it.</li>
 * <li>The inter-broker listener has to be one of the advertised listeners, and {@code EXTERNAL} cannot be
 * advertised because its port is only known after start-up.</li>
 * </ul>
 *
 * <p>The properties are asserted rather than a broker being started: a real start binds 9092 and would
 * collide with a running development environment, and it would take tens of seconds.
 */
class EmbeddedKafkaConfigurationTest {

    @SuppressWarnings("unchecked")
    private Properties brokerProperties() throws Exception {

        final var configuration = new EmbeddedKafkaConfiguration();
        ReflectionTestUtils.setField(configuration, "serverAddress", "10.0.0.1");

        final var broker = configuration.broker();

        return (Properties) ReflectionTestUtils.getField(broker, "brokerProperties");

    }

    @Test
    void theControllerListenerIsPartOfTheListeners() throws Exception {

        assertThat(brokerProperties().getProperty("listeners"))
                .as("combined mode needs a controller listener, and its port stays 0 so the testkit's "
                        + "prebound socket and the quorum voter address keep matching")
                .contains("CONTROLLER://localhost:0");

    }

    @Test
    void theFixedPortsUseListenerNamesOfTheirOwn() throws Exception {

        final var listeners = brokerProperties().getProperty("listeners");

        assertThat(listeners).contains("LOCAL://localhost:9092", "REMOTE://10.0.0.1:9093");
        assertThat(listeners)
                .as("EXTERNAL is the testkit's own listener - given a port here it would be overridden by "
                        + "the prebound socket and 9092 would end up closed")
                .contains("EXTERNAL://localhost:0");

    }

    @Test
    void onlyTheReachableListenersAreAdvertised() throws Exception {

        final var advertised = brokerProperties().getProperty("advertised.listeners");

        assertThat(advertised).isEqualTo("LOCAL://localhost:9092,REMOTE://10.0.0.1:9093");
        assertThat(advertised)
                .as("a controller listener must not be advertised, and EXTERNAL's port is unknown here")
                .doesNotContain("CONTROLLER", "EXTERNAL");

    }

    @Test
    void theInterBrokerListenerIsAdvertised() throws Exception {

        final var properties = brokerProperties();

        assertThat(properties.getProperty("inter.broker.listener.name")).isEqualTo("LOCAL");
        assertThat(properties.getProperty("advertised.listeners"))
                .contains(properties.getProperty("inter.broker.listener.name") + "://");

    }

    @Test
    void everyListenerHasAProtocolMapping() throws Exception {

        final var properties = brokerProperties();
        final var protocolMap = properties.getProperty("listener.security.protocol.map");

        for (final var listener : properties.getProperty("listeners").split(",")) {
            final var name = listener.substring(0, listener.indexOf("://"));
            assertThat(protocolMap)
                    .as("listener %s has no entry in listener.security.protocol.map", name)
                    .contains(name + ":PLAINTEXT");
        }

    }

}
