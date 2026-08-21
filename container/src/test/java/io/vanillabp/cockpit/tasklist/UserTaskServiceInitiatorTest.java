package io.vanillabp.cockpit.tasklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import java.util.ArrayList;
import java.util.List;
import io.vanillabp.cockpit.users.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * A cockpit-side change has to record who caused it in {@code initiator}: it is the only field
 * carrying that information ({@code updatedBy} is audit information overwritten on every save), and
 * the notification poller reads it to skip notifications a user triggered himself.
 */
class UserTaskServiceInitiatorTest {

    private UserTaskService service;
    private UserTaskRepository userTasks;
    private ReactiveUserContext currentUserContext;

    @BeforeEach
    void setUp() throws Exception {
        service = new UserTaskService();
        userTasks = mock(UserTaskRepository.class);
        currentUserContext = mock(ReactiveUserContext.class);
        inject("userTasks", userTasks);
        inject("currentUserContext", currentUserContext);

        final var task = new UserTask();
        task.setId("task-1");
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));
        when(userTasks.save(any(UserTask.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    private void inject(final String field, final Object value) throws Exception {
        final var declaredField = UserTaskService.class.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(service, value);
    }

    private static Person person(final String id) {
        final var person = new Person();
        person.setId(id);
        return person;
    }

    @Test
    void assignTask_recordsTheActingUserAsInitiator() {
        when(currentUserContext.getUserLoggedInAsMono()).thenReturn(Mono.just("actingUser"));

        final var result = service.assignTask("task-1", person("targetUser")).block();

        assertEquals("actingUser", result.getInitiator());
    }

    @Test
    void claimTask_recordsTheActingUserAsInitiator() {
        when(currentUserContext.getUserLoggedInAsMono()).thenReturn(Mono.just("actingUser"));

        final var result = service.claimTask("task-1", person("actingUser")).block();

        assertEquals("actingUser", result.getInitiator());
    }

    @Test
    void withoutLoggedInUser_theCockpitItselfIsTheInitiator() {
        when(currentUserContext.getUserLoggedInAsMono()).thenReturn(Mono.empty());

        final var result = service.assignTask("task-1", person("targetUser")).block();

        assertEquals(UpdateInformationAware.COCKPIT_USER, result.getInitiator());
    }

    @Test
    void unauthorizedContext_doesNotFailTheAction() {
        when(currentUserContext.getUserLoggedInAsMono())
                .thenReturn(Mono.error(new io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException("no context")));

        final var result = service.setFollowUpDate("task-1", null).block();

        assertEquals(UpdateInformationAware.COCKPIT_USER, result.getInitiator());
    }

    @Test
    void createUserTask_stampsTheCockpitsOwnReportingTime() {
        final var task = new UserTask();
        task.setId("task-2");

        service.createUserTask(task).block();

        // 'createdAt' stays the reporting system's timestamp, 'reportedAt' is the cockpit's own
        assertNotNull(task.getReportedAt());
    }

    @Test
    void createUserTask_recordsSinceWhenTheReportedCandidatesAreCandidates() {
        final var task = new UserTask();
        task.setId("task-2");
        task.setCandidateUsers(new ArrayList<>(List.of(person("u1"), person("u2"))));

        service.createUserTask(task).block();

        assertEquals(task.getReportedAt(), task.getCandidateSince("u1"));
        assertEquals(task.getReportedAt(), task.getCandidateSince("u2"));
    }

}
