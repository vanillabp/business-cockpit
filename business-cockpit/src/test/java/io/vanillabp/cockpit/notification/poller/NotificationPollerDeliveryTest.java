package io.vanillabp.cockpit.notification.poller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.notification.NotificationDeliveryException;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import io.vanillabp.cockpit.notification.model.NotificationOutboxRepository;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.UserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Delivery of a bulk is accounted for per recipient: a recipient already reached must not receive
 * the notification again just because another recipient of the same bulk failed.
 */
class NotificationPollerDeliveryTest {

    private static final String EMAIL = "email";

    private NotificationPoller poller;
    private NotificationOutboxRepository outbox;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        outbox = mock(NotificationOutboxRepository.class);
        service = mock(NotificationService.class);
        when(service.getType()).thenReturn(EMAIL);

        final var userTasks = mock(UserTaskRepository.class);
        final var task = new UserTask();
        task.setId("task-1");
        when(userTasks.findById(anyString())).thenReturn(java.util.Optional.of(task));
        when(outbox.saveAll(anyIterable()))
                .thenAnswer(invocation -> java.util.List.copyOf((java.util.Collection<NotificationOutboxEntry>) invocation.getArgument(0)));

        poller = new NotificationPoller(
                mock(MongoTemplate.class),
                mock(UserRepository.class),
                userTasks,
                outbox,
                List.of(service),
                mock(UserDetailsProvider.class),
                120,
                Duration.ofHours(48));
    }

    private NotificationOutboxEntry entry(final String recipient) {
        final var entry = new NotificationOutboxEntry();
        entry.setUserTaskId("task-1");
        entry.setNotificationType(NotificationType.CREATED);
        entry.setMedium(EMAIL);
        entry.setRecipientUserId(recipient);
        entry.setCreatedAt(OffsetDateTime.parse("2026-07-07T10:00:00Z"));
        return entry;
    }

    private void pending(final NotificationOutboxEntry... entries) {
        when(outbox.findBySentAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(120))
                .thenReturn(List.of(entries));
    }

    /** All entries saved by the poller, keyed by recipient. */
    private Map<String, NotificationOutboxEntry> saved(final int expectedCalls) {
        final ArgumentCaptor<Iterable<NotificationOutboxEntry>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(outbox, org.mockito.Mockito.times(expectedCalls)).saveAll(captor.capture());
        final var result = new java.util.HashMap<String, NotificationOutboxEntry>();
        captor.getAllValues().forEach(entries -> entries.forEach(
                entry -> result.put(entry.getRecipientUserId(), entry)));
        return result;
    }

    @Test
    void oneFailedRecipient_leavesOnlyThatOnePending() {
        final var reached = entry("u1");
        final var failed = entry("u2");
        pending(reached, failed);
        doThrow(new NotificationDeliveryException(List.of("u2"), null))
                .when(service).sendNotification(any(), any());

        poller.drainOutbox(OffsetDateTime.parse("2026-07-07T10:01:00Z"));

        final var saved = saved(2);
        assertNotNull(saved.get("u1").getSentAt(), "the recipient reached is done");
        assertEquals(0, saved.get("u1").getAttempts());
        assertNull(saved.get("u2").getSentAt(), "the failed recipient stays pending");
        assertEquals(1, saved.get("u2").getAttempts());
    }

    @Test
    void aFailureWithoutRecipientInformation_keepsTheWholeBulkPending() {
        final var first = entry("u1");
        final var second = entry("u2");
        pending(first, second);
        doThrow(new IllegalStateException("smtp down"))
                .when(service).sendNotification(any(), any());

        poller.drainOutbox(OffsetDateTime.parse("2026-07-07T10:01:00Z"));

        final var saved = saved(1);
        assertNull(saved.get("u1").getSentAt());
        assertNull(saved.get("u2").getSentAt());
        assertEquals(1, saved.get("u1").getAttempts());
        assertEquals(1, saved.get("u2").getAttempts());
    }

    @Test
    void successfulDelivery_marksEveryEntrySent() {
        pending(entry("u1"), entry("u2"));

        poller.drainOutbox(OffsetDateTime.parse("2026-07-07T10:01:00Z"));

        final var saved = saved(1);
        assertNotNull(saved.get("u1").getSentAt());
        assertNotNull(saved.get("u2").getSentAt());
        assertEquals(new ArrayList<>(List.of(0, 0)),
                new ArrayList<>(List.of(saved.get("u1").getAttempts(), saved.get("u2").getAttempts())));
    }

}
