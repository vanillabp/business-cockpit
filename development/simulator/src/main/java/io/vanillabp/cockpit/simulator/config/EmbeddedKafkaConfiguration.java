package io.vanillabp.cockpit.simulator.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashSet;

/**
 * Use localhost:9092 to connect to.
 * <p>
 * For external addresses you can use Spring Boot property
 * &quote;server.address&quot; to bind to at port 9093.
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

    /**
     * "EXTERNAL" is the listener name KafkaClusterTestKit pre-binds for itself, so it cannot serve a
     * fixed port (see the reasoning in {@link #broker()}). The listeners we do promise get names of
     * our own, and "CONTROLLER" is the name a KRaft node in combined mode requires to appear in
     * {@code listeners}.
     */
    private static String TESTKIT_LISTENER = "EXTERNAL";
    private static String LOCAL_LISTENER = "LOCAL";
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

        /*
         * EmbeddedKafkaBroker became an interface in spring-kafka 3.x. Of the two implementations, KRaft is
         * the one to pick: ZooKeeper support disappeared with Kafka 4, and kafka-clients is at 4.x here.
         * Arguments are (brokerCount, partitionsPerTopic). The calls cannot be chained, because
         * brokerProperty(..) returns the interface, which does not declare the concrete methods.
         *
         * kafkaPorts(..) is deliberately not used: EmbeddedKafkaKraftBroker stores the array and never
         * reads it again - start() hands the broker properties to KafkaClusterTestKit, which assigns random
         * ports. Overriding "listeners" is the only remaining way to pin the port down, which is what
         * happens below.
         */
        final var broker = new EmbeddedKafkaKraftBroker(1, 1);

        /*
         * Three listener-related traps, all of them measured against spring-kafka 4.1 / kafka-clients 4.2:
         *
         * 1. KafkaClusterTestKit pre-binds a socket for its own broker listener name, EXTERNAL, and that
         *    prebound port wins over whatever "listeners" asks for. Naming our fixed-port listener EXTERNAL
         *    therefore lands it on a random port - the broker comes up and localhost:9092 stays dead. So
         *    EXTERNAL keeps port 0 and stays the testkit's own channel (it is also what the testkit uses to
         *    talk to the cluster while starting up), while the listeners we promise get names of their own.
         * 2. This node runs in combined mode - broker and KRaft controller in one process - so CONTROLLER
         *    has to appear in "listeners", otherwise Kafka refuses to start with "controller.listener.names
         *    must contain at least one value appearing in the listeners configuration". Port 0 again,
         *    because the testkit derives the quorum voter address from its prebound controller socket.
         * 3. The inter-broker listener has to be one that is advertised. EXTERNAL is not advertised - its
         *    port is unknown at configuration time - so inter.broker.listener.name is pointed at LOCAL.
         */
        final var advertisedListeners = LOCAL_LISTENER + "://localhost:"
                + LOCAL_BROKER_PORT
                + "," + REMOTE_LISTENER + "://"
                + bindAddress + ":"
                + EXTERNAL_BROKER_PORT;
        broker.brokerProperty("listeners",
                TESTKIT_LISTENER + "://localhost:0,"
                + advertisedListeners
                + "," + CONTROLLER_LISTENER + "://localhost:0");
        broker.brokerProperty("advertised.listeners", advertisedListeners);
        broker.brokerProperty("listener.security.protocol.map",
                TESTKIT_LISTENER + ":PLAINTEXT,"
                + LOCAL_LISTENER + ":PLAINTEXT,"
                + REMOTE_LISTENER + ":PLAINTEXT,"
                + CONTROLLER_LISTENER + ":PLAINTEXT");
        broker.brokerProperty("inter.broker.listener.name", LOCAL_LISTENER);

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
