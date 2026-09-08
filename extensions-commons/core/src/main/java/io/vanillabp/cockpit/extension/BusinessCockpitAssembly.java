package io.vanillabp.cockpit.extension;

import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.templating.FreemarkerTemplating;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.cockpit.extension.transport.BusinessCockpitTransport;
import io.vanillabp.cockpit.extension.transport.KafkaTransport;
import io.vanillabp.cockpit.extension.transport.RestTransport;

/**
 * How the pieces of the extension are put together from what was configured. Both platform
 * modules do the same thing here, so they do it through this class rather than each in its own
 * way.
 */
public final class BusinessCockpitAssembly {

  private BusinessCockpitAssembly() {
  }

  /**
   * @param configuration The validated configuration
   * @return The transport it chose
   */
  public static BusinessCockpitTransport transportOf(
      final BusinessCockpitConfiguration configuration) {

    if (configuration.getRest() != null) {
      return new RestTransport(configuration.getRest());
    }
    requireKafkaClient();
    return kafkaTransport(configuration);

  }

  /**
   * The Kafka transport is built in a method of its own so that the classes of the Kafka client
   * are loaded when Kafka was chosen and never otherwise - the client is an optional dependency
   * of this module.
   */
  private static BusinessCockpitTransport kafkaTransport(
      final BusinessCockpitConfiguration configuration) {

    return new KafkaTransport(configuration.getKafka(), objectMapper());

  }

  private static void requireKafkaClient() {

    try {
      Class
          .forName(
              "org.apache.kafka.clients.producer.KafkaProducer", false,
              BusinessCockpitAssembly.class.getClassLoader());
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException(
          """
              The Business Cockpit's Kafka transport was chosen by '%s' but no Kafka client is on \
              the classpath. Add the dependencies 'org.apache.kafka:kafka-clients' and \
              'io.vanillabp.businesscockpit:bpms-protobuf-api' to your workflow module, or report \
              the events over REST by setting '%s' instead."""
              .formatted(
                  ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS),
                  ConfigurationKeys.globalKey(ConfigurationKeys.REST_BASE_URL)), e);
    }

  }

  /**
   * @param configuration The validated configuration
   * @return The renderer of the titles, {@link Templating#none()} where the application
   *         configured no template directory
   */
  public static Templating templatingOf(
      final BusinessCockpitConfiguration configuration) {

    if (!configuration.isTemplating()) {
      return Templating.none();
    }
    return freemarker(configuration.getTemplateLoaderPath());

  }

  /**
   * Loaded only where templates are configured, for the same reason the Kafka transport is.
   */
  private static Templating freemarker(
      final String templateLoaderPath) {

    return new FreemarkerTemplating(templateLoaderPath);

  }

  /**
   * The mapper turning a workflow module's business data into the tree both transports send.
   * <p>
   * Its settings are part of what the cockpit server receives, so they are pinned here rather
   * than taken from whatever mapper an application happens to have: dates as ISO-8601 rather
   * than as numbers, in UTC, and nothing absent written out as null.
   *
   * @return The mapper
   */
  public static ObjectMapper objectMapper() {

    return com.fasterxml.jackson.databind.json.JsonMapper
        .builder()
        .addModule(new JavaTimeModule())
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
        .defaultTimeZone(TimeZone.getTimeZone("UTC"))
        // for values as well as for what a map or a list holds: a detail nobody set is
        // absent rather than null, which is how the cockpit reads it either way and what
        // keeps a message from carrying what it does not say
        .defaultPropertyInclusion(
            JsonInclude.Value
                .construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
        .build();

  }

}
