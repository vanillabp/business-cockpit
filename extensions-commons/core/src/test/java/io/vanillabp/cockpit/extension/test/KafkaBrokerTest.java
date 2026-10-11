package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.config.KafkaTransportConfiguration;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.transport.KafkaTransport;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * One report through a real broker, from the producer the extension builds itself to the bytes
 * a consumer reads off the topic.
 * <p>
 * The other Kafka tests use the client's own test double, which is the right place to check the
 * content of a message. Only a broker can show the rest: that the producer this extension
 * configures connects and serializes at all, and that somebody else can read a message this
 * extension sent. One test is enough for that.
 * <p>
 * Testcontainers starts the broker, so the test needs a Docker daemon and is skipped where there
 * is none. A build machine without Docker cannot run this test, and that is no defect of the
 * extension.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
@Testcontainers(disabledWithoutDocker = true)
public class KafkaBrokerTest {

  private static final String USER_TASK_TOPIC = "user-task";

  @Container
  private final KafkaContainer broker = new KafkaContainer("apache/kafka:3.8.0");

  private KafkaTransport transportTo(
      final String bootstrapServers) {

    return new KafkaTransport(
        new KafkaTransportConfiguration(
            bootstrapServers, USER_TASK_TOPIC, "workflow", "workflow-module", Map.of()), BusinessCockpitAssembly
                .objectMapper());

  }

  private KafkaConsumer<String, byte[]> consumerOf(
      final String bootstrapServers) {

    final var properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "the-cockpit-server");
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties
        .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties
        .put(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    final var consumer = new KafkaConsumer<String, byte[]>(properties);
    consumer.subscribe(List.of(USER_TASK_TOPIC));
    return consumer;

  }

  @Test
  @DisplayName("A user task sent through a broker arrives as the message the cockpit reads")
  public void aUserTaskTravelsThroughTheBroker() throws Exception {

    final var bootstrapServers = broker.getBootstrapServers();
    final var transport = transportTo(bootstrapServers);

    try (var consumer = consumerOf(bootstrapServers)) {

      transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED));

      // the first polls of a new subscription return nothing while the consumer gets its
      // partitions, so the test waits for the message instead of polling once
      final var deadline = System.currentTimeMillis() + 60000;
      var records = consumer.poll(Duration.ofSeconds(5));
      while (records.isEmpty() && (System.currentTimeMillis() < deadline)) {
        records = consumer.poll(Duration.ofSeconds(5));
      }
      assertEquals(1, records.count(), "the broker handed on no message");
      final var record = records.iterator().next();
      assertEquals("task-1", record.key());
      final var envelope = BcEvent.parseFrom(record.value());
      assertTrue(envelope.hasUserTaskCreatedV11());
      assertEquals("task-1", envelope.getUserTaskCreatedV11().getUserTaskId());

    } finally {
      transport.close();
    }

  }

}
