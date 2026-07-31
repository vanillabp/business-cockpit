package io.vanillabp.cockpit.simulator.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Hint: Embedded Kafka is only active if property
 * &quot;spring.kafka.bootstrap-servers&quot; is not set which means to use
 * external Kafka.
 *
 * @author https://stackoverflow.com/questions/63812994/how-do-i-implement-in-memory-or-embedded-kafka-not-for-testing-purposes
 */
@Configuration
public class EmbeddedKafkaConfiguration {

    private static String LOCAL_BROKER_PORT = "9092";
    private static String EXTERNAL_BROKER_PORT = "9093";

    @Value("${server.address:}")
    private String serverAddress;

    @Bean
    @ConditionalOnExpression("'${spring.kafka.bootstrap-servers:}'.empty and ${kafka.embedded:false}")
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
        final var broker = new EmbeddedKafkaKraftBroker(1, 1).kafkaPorts(9092);

        final var listeners = "PLAINTEXT://localhost:"
                + LOCAL_BROKER_PORT
                + ",REMOTE://"
                + bindAddress + ":"
                + EXTERNAL_BROKER_PORT;
        broker.brokerProperty("listeners", listeners);
        broker.brokerProperty("advertised.listeners", listeners);
        broker.brokerProperty("listener.security.protocol.map", "PLAINTEXT:PLAINTEXT,REMOTE:PLAINTEXT");

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