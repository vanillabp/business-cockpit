package io.vanillabp.cockpit.users.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.notification.model.WorkflowNotificationConfiguration;
import io.vanillabp.cockpit.users.UserSettings;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The notification configuration and the recipient configuration of a medium are written one
 * field at a time, next to the settings of the user interface. Measured against a real MongoDB,
 * through the repository Spring Data builds.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class OnePartOfAUserDocumentTest {

  private static final String MARTIN = "martin";

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private UserRepository users;

  private UserSettings settings;

  @BeforeAll
  static void startMongoDb() {

    MONGODB.start();
    mongoClient = MongoClients.create(MONGODB.getConnectionString());

  }

  @AfterAll
  static void stopMongoDb() {

    if (mongoClient != null) {
      mongoClient.close();
    }
    MONGODB.stop();

  }

  @BeforeEach
  void anEmptyDatabase() {

    final var factory = new SimpleMongoClientDatabaseFactory(mongoClient, "cockpit-"
        + UUID.randomUUID());
    final var conversions = new MongoCustomConversions(List.of(
        new OffsetDateTimeReadConverter(),
        new OffsetDateTimeWriteConverter()));
    final var mappingContext = new MongoMappingContext();
    mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
    mappingContext.afterPropertiesSet();
    final var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mappingContext);
    converter.setCustomConversions(conversions);
    converter.afterPropertiesSet();
    final var mongoTemplate = new MongoTemplate(factory, converter);

    users = new MongoRepositoryFactory(mongoTemplate).getRepository(
        UserRepository.class,
        RepositoryFragments.just(new UserDocumentWritesImpl(mongoTemplate)));
    settings = new UserSettings(mongoTemplate);

  }

  @Test
  void eachPartLeavesTheOthersAsTheyWere() {

    final var configuration = new NotificationConfiguration(
        Map.of("email", true), Map.of(NotificationConfiguration.workflowKey("taxi-ride", "ride"),
            new WorkflowNotificationConfiguration(false, Map.of("email", false))));

    users.writeNotificationConfiguration(MARTIN, configuration);
    settings.set(MARTIN, "language", "de");
    users.writeRecipientConfiguration(MARTIN, "email", Map.of("emailAddress", "martin@example.org"));
    users.writeRecipientConfiguration(MARTIN, "sms", Map.of("phone", "123"));
    settings.set(MARTIN, "columns", List.of("title", "dueDate"));

    final var stored = users.findById(MARTIN).orElseThrow();
    assertThat(stored.getNotificationConfiguration()).isEqualTo(configuration);
    assertThat(stored.getRecipientConfigurations()).isEqualTo(Map.of(
        "email", Map.of("emailAddress", "martin@example.org"),
        "sms", Map.of("phone", "123")));
    assertThat(stored.getCustomSettings()).isEqualTo(Map.of(
        "language", "de",
        "columns", List.of("title", "dueDate")));

  }

  /**
   * Every write raises the version, so a save of the whole document which read it before still
   * fails instead of wiping the write out.
   */
  @Test
  void eachWriteRaisesTheVersion() {

    users.writeNotificationConfiguration(MARTIN, new NotificationConfiguration(Map.of(), Map.of()));
    final var first = users.findById(MARTIN).orElseThrow().getVersion();

    users.writeRecipientConfiguration(MARTIN, "email", Map.of("emailAddress", "martin@example.org"));
    users.writeNotificationConfiguration(MARTIN, new NotificationConfiguration(Map.of("email", true), Map.of()));

    assertThat(users.findById(MARTIN).orElseThrow().getVersion()).isEqualTo(first + 2);

  }

  @Test
  void aMediumWhoseTypeMongoDbWouldReadAsAPathIsRefused() {

    for (final var type : List.of("e.mail", "$email", "")) {
      assertThatThrownBy(() -> users.writeRecipientConfiguration(MARTIN, type, Map.of()))
          .as(type)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be the type of a medium");
    }
    assertThat(users.findById(MARTIN)).isEmpty();

  }

}
