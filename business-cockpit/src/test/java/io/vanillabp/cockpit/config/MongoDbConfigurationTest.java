package io.vanillabp.cockpit.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import com.mongodb.MongoClientSettings;

import io.vanillabp.cockpit.mongo.MongoDbProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The key {@code mongodb.use-tls} reaches the settings of the MongoDB client.
 */
@ExtendWith(SuppressOutputExtension.class)
class MongoDbConfigurationTest {

  private static MongoClientSettings settingsWith(
      final boolean useTls) {

    final var properties = new MongoDbProperties();
    properties.setUseTls(useTls);
    final var configuration = new MongoDbConfiguration();
    ReflectionTestUtils.setField(configuration, "properties", properties);

    final var builder = MongoClientSettings.builder();
    configuration.settings().customize(builder);
    return builder.build();

  }

  @Test
  void useTlsSwitchesTlsOn() {

    assertThat(settingsWith(true).getSslSettings().isEnabled()).isTrue();

  }

  @Test
  void withoutUseTlsTheConnectionHasNoTls() {

    assertThat(settingsWith(false).getSslSettings().isEnabled()).isFalse();

  }

}
