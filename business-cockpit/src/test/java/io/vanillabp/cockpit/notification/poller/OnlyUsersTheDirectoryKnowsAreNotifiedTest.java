package io.vanillabp.cockpit.notification.poller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import io.vanillabp.cockpit.notification.model.NotificationOutboxRepository;
import io.vanillabp.cockpit.notification.model.NotificationPollState;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A notification goes to a user who used the cockpit within
 * {@code business-cockpit.notification.recipients-active-within} and whom the user directory
 * still knows. Every user in the test switched notifications on for all workflows. The test runs
 * against a real MongoDB, because the poller asks MongoDB for the users who were active recently.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class OnlyUsersTheDirectoryKnowsAreNotifiedTest {

  private static final String EMAIL = "email";

  private static final Duration ACTIVE_WITHIN = Duration.ofDays(90);

  /** Known to the directory and active yesterday. */
  private static final String ALICE = "alice";

  /** Active yesterday, but the directory does not know her any more. */
  private static final String BEA = "bea";

  /** Known to the directory, but last active longer ago than the limit. */
  private static final String CARL = "carl";

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private MongoTemplate mongoTemplate;

  private NotificationOutboxRepository outbox;

  private NotificationPoller poller;

  private OffsetDateTime cursor;

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

  /** A template with the converters of the cockpit, in a database of its own. */
  private static MongoTemplate cockpitLikeTemplate() {

    final var factory = new SimpleMongoClientDatabaseFactory(mongoClient, "cockpit-"
        + UUID.randomUUID());
    final var conversions = new MongoCustomConversions(List.of(
        new OffsetDateTimeReadConverter(),
        new OffsetDateTimeWriteConverter(),
        new BigDecimalReadConverter(),
        new BigDecimalWriteConverter()));
    final var mappingContext = new MongoMappingContext();
    mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
    mappingContext.afterPropertiesSet();
    final var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mappingContext);
    converter.setCustomConversions(conversions);
    converter.afterPropertiesSet();
    return new MongoTemplate(factory, converter);

  }

  @BeforeEach
  void threeUsersWhoSwitchedNotificationsOn() {

    mongoTemplate = cockpitLikeTemplate();
    final var repositories = new MongoRepositoryFactory(mongoTemplate);
    outbox = repositories.getRepository(NotificationOutboxRepository.class);
    cursor = OffsetDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS);

    final var state = new NotificationPollState();
    state.setId(NotificationPollState.SINGLETON_ID);
    state.setCursor(cursor);
    mongoTemplate.save(state);

    final var yesterday = OffsetDateTime.now().minusDays(1);
    user(ALICE, yesterday);
    user(BEA, yesterday);
    user(CARL, OffsetDateTime.now().minus(ACTIVE_WITHIN).minusDays(1));

    final var directory = mock(UserDirectoryService.class);
    when(directory.getUser(any())).thenReturn(Optional.empty());
    for (final var known : Set.of(ALICE, CARL)) {
      when(directory.getUser(known)).thenReturn(Optional.of(new UserDetailsImpl(known, null, null, null, List.of())));
    }

    final var email = mock(NotificationService.class);
    when(email.getType()).thenReturn(EMAIL);

    poller = new NotificationPoller(
        mongoTemplate, repositories
            .getRepository(UserTaskRepository.class), outbox, List
                .of(email), directory, new GroupHierarchyService(), 3, Duration.ofHours(48), ACTIVE_WITHIN);

  }

  private void user(
      final String id,
      final OffsetDateTime lastLoggedIn) {

    final var user = new User();
    user.setId(id);
    user.setLastLoggedIn(lastLoggedIn);
    user.setNotificationConfiguration(new NotificationConfiguration(Map.of(EMAIL, true), Map.of()));
    mongoTemplate.save(user);

  }

  private UserTask reportedTask() {

    final var task = new UserTask();
    task.setId("task-1");
    task.setWorkflowModuleId("module");
    task.setBpmnProcessId("process");
    task.setCreatedAt(cursor.plusSeconds(10));
    task.setReportedAt(cursor.plusSeconds(10));
    task.setUpdatedAt(cursor.plusSeconds(10));
    return task;

  }

  private List<String> recipients() {

    return outbox.findAll().stream().map(NotificationOutboxEntry::getRecipientUserId).sorted().toList();

  }

  /** A task which addresses nobody is seen by every user, but mailed to known and active users only. */
  @Test
  void aTaskWhichAddressesNobodyIsMailedToKnownAndActiveUsersOnly() {

    mongoTemplate.save(reportedTask());

    poller.runCycle();

    assertThat(recipients()).containsExactly(ALICE);

  }

  /** A task which names its candidates by id is mailed only to those of them who are known and active. */
  @Test
  void aTaskWhichNamesItsCandidatesIsMailedToKnownAndActiveCandidatesOnly() {

    final var task = reportedTask();
    task.setCandidateUsers(List.of(person(ALICE), person(BEA), person(CARL)));
    task.stampCandidatesSince(cursor.plusSeconds(10));
    mongoTemplate.save(task);

    poller.runCycle();

    assertThat(recipients()).containsExactly(ALICE);

  }

  private static Person person(
      final String id) {

    final var person = new Person();
    person.setId(id);
    return person;

  }

}
