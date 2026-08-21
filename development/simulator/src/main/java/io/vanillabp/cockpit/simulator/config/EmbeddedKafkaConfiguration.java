package io.vanillabp.cockpit.simulator.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import kafka.testkit.KafkaClusterTestKit;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashSet;

/**
 * Use localhost:9092 to connect to.
 * <p>
 * For external addresses you can use Spring Boot property
 * &quote;server.address&quot; to bind to at port 9093. Port 9094 is used by the KRaft controller of
 * the broker itself and is of no use to clients.
 * <p>
 * The broker is part of the "kafka-sync" mode and started by default, so reporting by Kafka needs
 * no Kafka installation at all. To report to a real broker instead - e.g. the single-node Kafka of
 * {@code development/docker-compose.yaml}, which listens at the same address - switch the embedded
 * one off:
 * <ul>
 *   <li>{@code -Dkafka.embedded=false} keeps the client at its default {@code localhost:9092}</li>
 *   <li>{@code -Dspring.kafka.bootstrap-servers=<host>:<port>} points the client somewhere else and
 *       implies switching the embedded broker off - it would be pointless (and would fail binding
 *       the port if the external broker runs locally)</li>
 * </ul>
 *
 * @author https://stackoverflow.com/questions/63812994/how-do-i-implement-in-memory-or-embedded-kafka-not-for-testing-purposes
 */
@Configuration
@Profile("kafka-sync")
public class EmbeddedKafkaConfiguration {

    private static String LOCAL_BROKER_PORT = "9092";
    private static String EXTERNAL_BROKER_PORT = "9093";
    private static String CONTROLLER_PORT = "9094";

    /**
     * The listener names {@link KafkaClusterTestKit} uses: it asks the broker for the bound port of
     * "EXTERNAL" after startup, and a KRaft node in combined mode requires the listener named by
     * {@code controller.listener.names} to be part of {@code listeners}. Overriding the listeners
     * with names of one's own choice therefore breaks the cluster, either while validating the
     * configuration or right after it started.
     */
    private static String CLIENT_LISTENER = "EXTERNAL";
    private static String REMOTE_LISTENER = "REMOTE";
    private static String CONTROLLER_LISTENER = "CONTROLLER";

    @Value("${server.address:}")
    private String serverAddress;

    @Bean
    // switched on unless told otherwise, either by 'kafka.embedded' or by naming an external broker
    @ConditionalOnExpression("${kafka.embedded:true} and '${spring.kafka.bootstrap-servers:}'.empty")
    public EmbeddedKafkaBroker broker() throws Exception {

        String bindAddress;
        if (!StringUtils.hasText(serverAddress)) {
            bindAddress = getDefaultAddress();
        } else {
            bindAddress = serverAddress;
        }

        LoggerFactory
                .getLogger(EmbeddedKafkaConfiguration.class)
                .info("Broker URNs: 'localhost:{}', '{}:{}'",
                        LOCAL_BROKER_PORT,
                        bindAddress,
                        EXTERNAL_BROKER_PORT);

        // EmbeddedKafkaBroker became an interface in spring-kafka 3.x. Of the two implementations, KRaft
        // is the one to pick: ZooKeeper support disappears with Kafka 4, so EmbeddedKafkaZKBroker would
        // only have to be replaced again in T19. Arguments are (brokerCount, partitionsPerTopic).
        //
        // The calls cannot be chained any more either: brokerProperty(..) returns the interface, which
        // does not declare it, so the concrete instance is kept in a local variable.
        //
        // kafkaPorts(..) is not called: it is documented as being out of use for KRaft. The ports are
        // pinned by the listeners below instead, which is what makes the broker reachable from another
        // process - the business cockpit consuming from it.
        final var broker = new EmbeddedKafkaKraftBroker(1, 1);

        // the controller listener is required by a KRaft node in combined mode and must not be
        // advertised, so both properties are built separately
        final var clientListeners = CLIENT_LISTENER + "://localhost:" + LOCAL_BROKER_PORT
                + "," + REMOTE_LISTENER + "://" + bindAddress + ":" + EXTERNAL_BROKER_PORT;
        broker.brokerProperty("listeners",
                clientListeners + "," + CONTROLLER_LISTENER + "://localhost:" + CONTROLLER_PORT);
        broker.brokerProperty("advertised.listeners", clientListeners);
        broker.brokerProperty("listener.security.protocol.map",
                CLIENT_LISTENER + ":PLAINTEXT,"
                + REMOTE_LISTENER + ":PLAINTEXT,"
                + CONTROLLER_LISTENER + ":PLAINTEXT");
        broker.brokerProperty("controller.listener.names", CONTROLLER_LISTENER);
        broker.brokerProperty("inter.broker.listener.name", CLIENT_LISTENER);

        return broker;

    }

    private static String getDefaultAddress() throws Exception {

        final var result = new HashSet<String>();

        final var en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            final var nint = en.nextElement();
            if (nint.isLoopback()) {
                continue;
            }
            nint
                    .inetAddresses()
                    .filter(address -> address instanceof Inet4Address)
                    .map(InetAddress::getHostAddress)
                    .forEach(address -> result.add(address));
            if (!result.isEmpty()) {
                return result.iterator().next();
            }
        }

        return "0.0.0.0";

    }

}