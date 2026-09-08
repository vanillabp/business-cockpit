package io.vanillabp.cockpit.extension.transport;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vanillabp.cockpit.extension.config.KafkaTransportConfiguration;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;

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
          "Interrupted while sending a Business Cockpit event to the topic '%s'".formatted(topic), e);
    } catch (final ExecutionException e) {
      throw new IllegalStateException(
          "Could not send a Business Cockpit event to the topic '%s'".formatted(topic), e.getCause());
    }

  }

}
