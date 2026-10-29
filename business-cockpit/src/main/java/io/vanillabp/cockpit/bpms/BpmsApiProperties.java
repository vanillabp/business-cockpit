package io.vanillabp.cockpit.bpms;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.commons.kafka.KafkaProperties;

/**
 * How the BPMS adapters of the workflow modules reach the cockpit to report user tasks, cases and
 * workflow modules: by REST with HTTP basic, or by Kafka. The properties start with
 * {@code bpms-api}.
 */
@ConfigurationProperties(prefix = BpmsApiProperties.PREFIX, ignoreUnknownFields = false)
public class BpmsApiProperties {

  /**
   * The prefix of all properties of this class.
   */
  public static final String PREFIX = "bpms-api";

  /**
   * Creates empty properties for Spring Boot to fill.
   */
  public BpmsApiProperties() {
  }

  private KafkaProperties kafka = new KafkaProperties();

  private String realmName;

  private String username;

  private String password;

  /**
   * The realm the cockpit names when it asks a BPMS adapter for HTTP basic credentials.
   *
   * @return The realm name
   */
  public String getRealmName() {
    return realmName;
  }

  /**
   * Sets the realm of HTTP basic for the BPMS API.
   *
   * @param realmName The realm name
   */
  public void setRealmName(
      String realmName) {
    this.realmName = realmName;
  }

  /**
   * The user name the BPMS adapters log in with on the BPMS API.
   *
   * @return The user name
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the user name the BPMS adapters log in with.
   *
   * @param username The user name
   */
  public void setUsername(
      String username) {
    this.username = username;
  }

  /**
   * The password the BPMS adapters log in with on the BPMS API.
   *
   * @return The password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password the BPMS adapters log in with.
   *
   * @param password The password
   */
  public void setPassword(
      String password) {
    this.password = password;
  }

  /**
   * The settings for reports which come by Kafka instead of REST: the topics and the suffix of the consumer group id.
   *
   * @return The Kafka settings
   */
  public KafkaProperties getKafka() {
    return kafka;
  }

  /**
   * Sets the settings for reports which come by Kafka.
   *
   * @param kafka The Kafka settings
   */
  public void setKafka(
      KafkaProperties kafka) {
    this.kafka = kafka;
  }

}
