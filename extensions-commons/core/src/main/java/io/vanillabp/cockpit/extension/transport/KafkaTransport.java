package io.vanillabp.cockpit.extension.transport;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vanillabp.cockpit.extension.config.KafkaTransportConfiguration;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;

/**
 * Reports events to the cockpit server over Kafka, as protobuf.
 * <p>
 * The key of every record is the entity the event is about - the user task, the workflow, the
 * workflow module - so that everything about one entity lands in one partition and the cockpit
 * sees it in the order it happened.
 * <p>
 * A send is awaited rather than fired and forgotten. What the outbox guarantees is that an
 * event reaches the cockpit or is retried, and a producer whose queue swallowed the record
 * without the broker acknowledging it would break that promise while looking green.
 */
public class KafkaTransport implements BusinessCockpitTransport {

  private final KafkaTransportConfiguration configuration;

  private final ProtobufMapper mapper;

  private final Producer<String, byte[]> producer;

  /**
   * @param configuration The brokers and topics
   * @param objectMapper The mapper turning business data into a tree
   */
  public KafkaTransport(
      final KafkaTransportConfiguration configuration,
      final ObjectMapper objectMapper) {

    this(configuration, objectMapper, new KafkaProducer<>(producerProperties(configuration)));

  }

  /**
   * @param configuration The brokers and topics
   * @param objectMapper The mapper turning business data into a tree
   * @param producer A producer built elsewhere - what a test hands in
   */
  public KafkaTransport(
      final KafkaTransportConfiguration configuration,
      final ObjectMapper objectMapper,
      final Producer<String, byte[]> producer) {

    this.configuration = configuration;
    this.mapper = new ProtobufMapper(objectMapper);
    this.producer = producer;

  }

  private static Properties producerProperties(
      final KafkaTransportConfiguration configuration) {

    final var properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.bootstrapServers());
    properties
        .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties
        .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    configuration.producerProperties().forEach(properties::put);
    return properties;

  }

  @Override
  public String describe() {

    return "the cockpit server's Kafka topics at %s".formatted(configuration.bootstrapServers());

  }

  @Override
  public void publishUserTaskEvent(
      final UserTaskEvent event) {

    send(configuration.userTaskTopic(), event.getUserTaskId(), mapper.map(event).toByteArray());

  }

  @Override
  public void publishWorkflowEvent(
      final WorkflowEvent event) {

    send(configuration.workflowTopic(), event.getWorkflowId(), mapper.map(event).toByteArray());

  }

  @Override
  public void registerWorkflowModule(
      final RegisterWorkflowModuleEvent event) {

    send(
        configuration.workflowModuleTopic(), event.workflowModuleId(),
        mapper.map(event).toByteArray());

  }

  @Override
  public void close() {

    producer.close();

  }

  private void send(
      final String topic,
      final String key,
      final byte[] value) {

    try {
      producer.send(new ProducerRecord<>(topic, key, value)).get();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while sending a Business Cockpit event to the topic '%s' of %s"
              .formatted(topic, describe()), e);
    } catch (final ExecutionException e) {
      throw failureOf(topic, e.getCause());
    } catch (final RuntimeException e) {
      // what the client refuses before it ever reaches a broker - a record it cannot serialize,
      // one larger than the configured maximum - arrives here instead of in the future
      throw failureOf(topic, e);
    }

  }

  /**
   * What a failed send means for the outbox entry - see decision 11 in the repository's
   * DECISIONS.md.
   * <p>
   * The client's own verdict is taken: it knows which of its failures pass, and repeating a
   * send it marks as retriable is the whole point of the outbox. Anything else is about the
   * record rather than about the moment, so the same bytes would be refused again.
   *
   * @param topic Where the record was to go
   * @param cause What the send failed with
   * @return The exception ending this dispatch
   */
  private RuntimeException failureOf(
      final String topic,
      final Throwable cause) {

    final var message = "Could not send a Business Cockpit event to the topic '%s' of %s"
        .formatted(topic, describe());
    return cause instanceof RetriableException
        ? new IllegalStateException(message, cause)
        : new PhaseTwoPermanentFailure(
            """
                %s. The broker will refuse the same record again, so the report is given up. \
                Check that the topic exists and that this application may write to it."""
                .formatted(message), cause);

  }

}
