package io.vanillabp.cockpit.extension.config;

import java.util.Map;

/**
 * Which brokers the events go to and under which topics.
 *
 * @param bootstrapServers The brokers, in the form the Kafka client expects
 * @param userTaskTopic The topic user-task events are sent to
 * @param workflowTopic The topic workflow events are sent to
 * @param workflowModuleTopic The topic a workflow module's registration is sent to
 * @param producerProperties Everything else configured below
 *          <code>kafka.properties.*</code>, handed to the producer unchanged - a security
 *          protocol, a truststore, a client id
 */
public record KafkaTransportConfiguration(
                                          String bootstrapServers,
                                          String userTaskTopic,
                                          String workflowTopic,
                                          String workflowModuleTopic,
                                          Map<String, String> producerProperties) {

  public KafkaTransportConfiguration {
    producerProperties = producerProperties == null ? Map.of() : Map.copyOf(producerProperties);
  }

}
