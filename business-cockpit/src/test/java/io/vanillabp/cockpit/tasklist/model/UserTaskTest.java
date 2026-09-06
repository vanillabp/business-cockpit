package io.vanillabp.cockpit.tasklist.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers the bookkeeping a user task does for the cockpit itself: the authorities a task is visible
 * to, and since when a user is a personal candidate.
 */
class UserTaskTest {

    private static Person person(final String id) {
        final var person = new Person();
        person.setId(id);
        return person;
    }

    private static Group group(final String id) {
        final var group = new Group();
        group.setId(id);
        return group;
    }

    @Test
    void targetGroups_containThePrefixedIdOfTheAssignee() {
        final var task = new UserTask();
        task.setCandidateGroups(new ArrayList<>(List.of(group("g1"))));
        task.setAssignee(person("u1"));

        // the id, not the Person object: 'USER_' + a Person renders its class name and hash code,
        // which matches no authority at all
        assertTrue(task.getTargetGroups().contains("USER_u1"), task.getTargetGroups().toString());
        assertTrue(task.getTargetGroups().contains("g1"));
    }

    @Test
    void targetGroups_ofATaskWithoutCandidatesAndAssignee_areUnrestricted() {
        assertNull(new UserTask().getTargetGroups());
        assertTrue(new UserTask().isDangling());
    }

    @Test
    void aTakenOverTaskIsNotDangling() {
        final var task = new UserTask();
        task.setAssignee(person("u1"));

        assertTrue(!task.isDangling());
    }

    @Test
    void addCandidatePerson_worksOnATaskWithoutCandidatesAndStampsTheTime() {
        final var task = new UserTask();

        task.addCandidatePerson(person("u1"));
        task.addCandidatePerson(person("u2")); // must not fail on an immutable list

        assertEquals(List.of("u1", "u2"),
                task.getCandidateUsers().stream().map(Person::getId).toList());
        assertNotNull(task.getCandidateSince("u1"));
        assertNotNull(task.getCandidateSince("u2"));
    }

    @Test
    void addCandidatePerson_twice_keepsTheCandidateOnce() {
        final var task = new UserTask();
        task.addCandidatePerson(person("u1"));
        task.addCandidatePerson(person("u1"));

        assertEquals(1, task.getCandidateUsers().size());
        assertEquals(1, task.getCandidateUsersSince().size());
    }

    @Test
    void removeCandidatePerson_dropsTheTimestampAsWell() {
        final var task = new UserTask();
        task.addCandidatePerson(person("u1"));

        task.removeCandidatePerson("u1");

        assertTrue(task.getCandidateUsers().isEmpty());
        assertNull(task.getCandidateSince("u1"));
    }

    @Test
    void stampCandidatesSince_onlyFillsWhatIsNotKnownYet() {
        final var task = new UserTask();
        task.setCandidateUsers(new ArrayList<>(List.of(person("reported"))));
        task.addCandidatePerson(person("assigned"));
        final var assignedSince = task.getCandidateSince("assigned");

        final var reportedAt = OffsetDateTime.parse("2026-07-07T10:00:00Z");
        task.stampCandidatesSince(reportedAt);

        assertEquals(reportedAt, task.getCandidateSince("reported"));
        assertEquals(assignedSince, task.getCandidateSince("assigned"));
    }

    @Test
    void setReadAt_worksForASecondReader() {
        final var task = new UserTask();

        task.setReadAt("u1");
        task.setReadAt("u2"); // must not fail on an immutable list

        assertNotNull(task.getReadAt("u1"));
        assertNotNull(task.getReadAt("u2"));
    }

}
