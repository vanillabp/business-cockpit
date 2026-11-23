package io.vanillabp.cockpit.notification.poller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
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
import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import io.vanillabp.cockpit.notification.model.NotificationOutboxRepository;
import io.vanillabp.cockpit.notification.model.NotificationPollState;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.UserDocumentsWithATypeHintKey;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The poller reads the documents of all users in each cycle. One document which Spring Data cannot
 * read, like one which holds a key {@code _class} that names no type the cockpit stores, must not
 * stop the notifications of all other users. The poller leaves that one user out, plans the
 * notifications of the others, moves its cursor on and sends what waits in the outbox.
 * <p>
 * The test runs against a real MongoDB with the converters of the cockpit, because the failure
 * comes from reading the stored document.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class OneUserDocumentWhichCannotBeReadStopsNoNotificationTest {

  private static final String EMAIL = "email";

  private static final String CLERK = "CLERK";

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private MongoTemplate mongoTemplate;

  private NotificationOutboxRepository outbox;

  private NotificationService email;

  private NotificationPoller poller;

  private OffsetDateTime cursor;

  private UserDirectoryService directory;

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
  void aTaskForClerksAndTwoUsers() {

    mongoTemplate = cockpitLikeTemplate();
    final var repositories = new MongoRepositoryFactory(mongoTemplate);
    outbox = repositories.getRepository(NotificationOutboxRepository.class);
    final var userTasks = repositories.getRepository(UserTaskRepository.class);
    cursor = OffsetDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS);

    final var task = new UserTask();
    task.setId("task-1");
    task.setWorkflowModuleId("module");
    task.setBpmnProcessId("process");
    task.setCreatedAt(cursor.plusSeconds(10));
    task.setReportedAt(cursor.plusSeconds(10));
    task.setUpdatedAt(cursor.plusSeconds(10));
    final var clerks = new Group();
    clerks.setId(CLERK);
    task.setCandidateGroups(List.of(clerks));
    mongoTemplate.save(task);

    final var state = new NotificationPollState();
    state.setId(NotificationPollState.SINGLETON_ID);
    state.setCursor(cursor);
    mongoTemplate.save(state);

    final var alice = new User();
    alice.setId("alice");
    alice.setNotificationConfiguration(new NotificationConfiguration(Map.of(EMAIL, true), Map.of()));
    mongoTemplate.save(alice);

    // written as it was stored by a setting before such a key was refused
    mongoTemplate.getCollection(User.COLLECTION_NAME).insertOne(new Document("_id", "mallory")
        .append("customSettings", new Document("x", new Document("_class", "java.lang.Thread"))));

    // how the poller read the users before: the repository maps every whole document
    final var users = mock(UserRepository.class);
    when(users.findAll()).thenAnswer(invocation -> mongoTemplate.findAll(User.class));

    directory = mock(UserDirectoryService.class);
    when(directory.getUser(any())).thenAnswer(invocation -> Optional.of(
        new UserDetailsImpl(invocation.getArgument(0), null, null, null, List.of(CLERK))));

    email = mock(NotificationService.class);
    when(email.getType()).thenReturn(EMAIL);

    poller = new NotificationPoller(
        mongoTemplate, users, userTasks, outbox, List.of(email), directory, new GroupHierarchyService(), 3, Duration
            .ofHours(48));

  }

  @Test
  void theOtherUsersAreNotifiedAndTheCursorMovesOn() {

    poller.runCycle();

    assertThat(outbox.findAll())
        .extracting(NotificationOutboxEntry::getRecipientUserId)
        .containsExactly("alice");
    verify(email).sendNotification(eq(List.of("alice")), any());
    assertThat(mongoTemplate.findById(NotificationPollState.SINGLETON_ID, NotificationPollState.class).getCursor())
        .isAfter(cursor);

  }

  /**
   * What waits in the outbox was planned in an earlier cycle. It is sent even when planning the
   * changes of this cycle fails, here because the user directory cannot be reached.
   */
  @Test
  void theOutboxIsSentEvenWhenPlanningFails() {

    final var waiting = new NotificationOutboxEntry();
    waiting.setUserTaskId("task-1");
    waiting.setNotificationType(NotificationType.CREATED);
    waiting.setMedium(EMAIL);
    waiting.setRecipientUserId("alice");
    waiting.setCreatedAt(cursor);
    outbox.insert(waiting);
    when(directory.getUser(any())).thenThrow(new IllegalStateException("the directory cannot be reached"));

    poller.runCycle();

    verify(email).sendNotification(eq(List.of("alice")), any());

  }

  /** The check at startup names the user whose document holds the key, and nobody else. */
  @Test
  void theCheckAtStartupNamesTheDocumentWhichHoldsTheKey() {

    assertThat(new UserDocumentsWithATypeHintKey(mongoTemplate).find()).containsExactly("mallory (customSettings)");

  }

}
