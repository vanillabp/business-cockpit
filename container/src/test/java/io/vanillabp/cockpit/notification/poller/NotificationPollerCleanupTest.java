package io.vanillabp.cockpit.notification.poller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.DeleteResult;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import io.vanillabp.cockpit.notification.model.NotificationOutboxRepository;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.UserRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;

class NotificationPollerCleanupTest {

    @Test
    void runCleanup_removesSentEntriesOlderThanRetention() {
        final var mongoTemplate = mock(ReactiveMongoTemplate.class);
        final var deleteResult = mock(DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(3L);
        when(mongoTemplate.remove(any(Query.class), eq(NotificationOutboxEntry.class)))
                .thenReturn(Mono.just(deleteResult));

        final var poller = new NotificationPoller(
                mongoTemplate,
                mock(UserRepository.class),
                mock(UserTaskRepository.class),
                mock(NotificationOutboxRepository.class),
                List.of(mock(NotificationService.class)),
                mock(UserDetailsProvider.class),
                120,
                Duration.ofHours(48));

        poller.runCleanup();

        final ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(query.capture(), eq(NotificationOutboxEntry.class));
        // only sent entries (sentAt set) older than the threshold are removed
        final var queryObject = query.getValue().getQueryObject();
        assertTrue(queryObject.containsKey("sentAt"), queryObject.toString());
        final var sentAtCriteria = (org.bson.Document) queryObject.get("sentAt");
        assertTrue(sentAtCriteria.containsKey("$lt"), queryObject.toString());
    }

}
