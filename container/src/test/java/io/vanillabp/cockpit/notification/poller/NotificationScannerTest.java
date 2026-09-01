package io.vanillabp.cockpit.notification.poller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.spi.cockpit.usertask.NotificationDelivery;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotificationScannerTest {

    private static final String EMAIL = "email";
    private static final OffsetDateTime CURSOR = OffsetDateTime.parse("2026-07-07T10:00:00Z");
    private static final OffsetDateTime AFTER = CURSOR.plusMinutes(1);

    private final NotificationScanner scanner = new NotificationScanner();

    /** Configurable in-memory directory. */
    private static final class FakeDirectory implements RecipientDirectory {
        final Map<String, List<String>> authorities = new HashMap<>();
        final Map<String, NotificationConfiguration> configs = new HashMap<>();
        final List<String> media = new ArrayList<>(List.of(EMAIL));

        FakeDirectory loggedIn(String userId, List<String> auth, boolean notifyAll) {
            authorities.put(userId, auth);
            configs.put(userId, notifyAll ? new NotificationConfiguration(Map.of(EMAIL, true), Map.of()) : null);
            return this;
        }

        public Collection<String> mediaTypes() {
            return media;
        }

        public boolean isLoggedIn(String userId) {
            return authorities.containsKey(userId);
        }

        public Collection<String> loggedInUserIds() {
            return authorities.keySet();
        }

        public List<String> authoritiesOf(String userId) {
            return authorities.get(userId);
        }

        public NotificationConfiguration configOf(String userId) {
            return configs.get(userId);
        }
    }

    private static Person person(String id) {
        final var p = new Person();
        p.setId(id);
        return p;
    }

    private static Group group(String id) {
        final var g = new Group();
        g.setId(id);
        return g;
    }

    private static UserTask openTask() {
        final var task = new UserTask();
        task.setId("task-1");
        task.setWorkflowModuleId("wfm");
        task.setBpmnProcessId("proc");
        task.setCreatedAt(AFTER);
        task.setUpdatedAt(AFTER);
        task.setReportedAt(AFTER);
        return task;
    }

    /** Personal candidates the cockpit learned about at the given point in time. */
    private static void candidates(
            final UserTask task,
            final OffsetDateTime since,
            final String... userIds) {
        task.setCandidateUsers(new ArrayList<>(
                java.util.Arrays.stream(userIds).map(NotificationScannerTest::person).toList()));
        task.stampCandidatesSince(since);
    }

    @Test
    void created_notifiesVisibleLoggedInUser_viaGroup() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        final var dir = new FakeDirectory().loggedIn("u1", List.of("g1"), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals(NotificationType.CREATED, planned.get(0).notificationType());
        assertEquals("u1", planned.get(0).recipientUserId());
    }

    @Test
    void created_skippedWhenUserConfigIsNone() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        final var dir = new FakeDirectory().loggedIn("u1", List.of("g1"), false);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void created_skippedWhenNotVisible() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        final var dir = new FakeDirectory().loggedIn("u1", List.of("otherGroup"), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void created_notOlderThanCursor_notReported() {
        final var task = openTask();
        task.setCreatedAt(CURSOR.minusMinutes(5));
        task.setReportedAt(CURSOR.minusMinutes(5)); // reported before the window
        task.setCandidateGroups(List.of(group("g1")));
        final var dir = new FakeDirectory().loggedIn("u1", List.of("g1"), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void danglingTask_visibleToEveryone() {
        final var task = openTask(); // no candidates/groups/assignee -> targetGroups null
        final var dir = new FakeDirectory()
                .loggedIn("u1", List.of(), true)
                .loggedIn("u2", List.of(), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(2, planned.size());
        assertTrue(planned.stream().allMatch(p -> p.notificationType() == NotificationType.CREATED));
    }

    @Test
    void candidateUser_personallyNotified_notAlsoCreated() {
        final var task = openTask();
        candidates(task, AFTER, "u1");
        task.setCandidateGroups(List.of(group("g1")));
        task.setInitiator("someoneElse");
        final var dir = new FakeDirectory()
                .loggedIn("u1", List.of("USER_u1"), true) // personal candidate
                .loggedIn("u2", List.of("g1"), true);      // visible via group

        final var planned = scanner.scan(task, CURSOR, dir);
        final var byUser = planned.stream().collect(
                java.util.stream.Collectors.toMap(PlannedNotification::recipientUserId, p -> p.notificationType()));
        assertEquals(NotificationType.CANDIDATE_USER, byUser.get("u1"));
        assertEquals(NotificationType.CREATED, byUser.get("u2"));
        assertEquals(2, planned.size());
    }

    @Test
    void candidateUser_selfCaused_excluded() {
        final var task = openTask();
        candidates(task, AFTER, "u1");
        task.setInitiator("u1"); // the user caused it himself (AC func 2b)
        final var dir = new FakeDirectory().loggedIn("u1", List.of("USER_u1"), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void completed_byAnotherUser_notifiesAssignee() {
        final var task = openTask();
        task.setEndedAt(AFTER);
        task.setEndReason(UserTaskEndReason.COMPLETED);
        task.setAssignee(person("u1"));
        task.setInitiator("u2"); // completed by someone else
        final var dir = new FakeDirectory().loggedIn("u1", List.of(), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals(NotificationType.COMPLETED, planned.get(0).notificationType());
        assertEquals("u1", planned.get(0).recipientUserId());
    }

    @Test
    void completed_bySelf_notNotified() {
        final var task = openTask();
        task.setEndedAt(AFTER);
        task.setEndReason(UserTaskEndReason.COMPLETED);
        task.setAssignee(person("u1"));
        task.setInitiator("u1"); // assignee completed their own task
        final var dir = new FakeDirectory().loggedIn("u1", List.of(), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void cancelled_notifiesAssignee() {
        final var task = openTask();
        task.setEndedAt(AFTER);
        task.setEndReason(UserTaskEndReason.CANCELLED);
        task.setAssignee(person("u1"));
        task.setInitiator("theProcess");
        final var dir = new FakeDirectory().loggedIn("u1", List.of(), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals(NotificationType.CANCELED, planned.get(0).notificationType());
    }

    @Test
    void delivery_force_overridesUserConfigNone() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        task.setNotificationDelivery(NotificationDelivery.FORCE);
        final var dir = new FakeDirectory().loggedIn("u1", List.of("g1"), false); // config none

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertTrue(planned.get(0).forced());
    }

    @Test
    void delivery_suppress_skipsEvenIfConfigured() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        task.setNotificationDelivery(NotificationDelivery.SUPPRESS);
        final var dir = new FakeDirectory().loggedIn("u1", List.of("g1"), true); // config notify

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void created_skippedWhenAuthoritiesUnknown() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        final var dir = new FakeDirectory();
        dir.authorities.put("u1", null); // logged in but no directory authorities
        dir.configs.put("u1", new NotificationConfiguration(Map.of(EMAIL, true), Map.of()));

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void nonLoggedInCandidate_notNotified() {
        final var task = openTask();
        candidates(task, AFTER, "ghost");
        task.setInitiator("x");
        final var dir = new FakeDirectory(); // nobody logged in

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void multipleMedia_planPerMedium() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        final var dir = new FakeDirectory().loggedIn("u1", List.of("g1"), true);
        dir.media.add("sms");
        // u1 config only enables email globally -> only email planned
        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(Set.of(EMAIL), planned.stream().map(PlannedNotification::medium).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void completed_updatedByIsIgnored_onlyInitiatorDecides() {
        final var task = openTask();
        task.setEndedAt(AFTER);
        task.setEndReason(UserTaskEndReason.COMPLETED);
        task.setAssignee(person("u1"));
        task.setInitiator("u1");         // the assignee completed their own task
        task.setUpdatedBy("system");     // audit field written by the save callback
        final var dir = new FakeDirectory().loggedIn("u1", List.of(), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void completed_withoutInitiator_notifiesAssignee() {
        final var task = openTask();
        task.setEndedAt(AFTER);
        task.setEndReason(UserTaskEndReason.COMPLETED);
        task.setAssignee(person("u1"));
        // no initiator reported: the application did not tell who completed the task
        task.setUpdatedBy("u1");
        final var dir = new FakeDirectory().loggedIn("u1", List.of(), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals(NotificationType.COMPLETED, planned.get(0).notificationType());
    }

    @Test
    void candidateUser_updatedByIsIgnored_onlyInitiatorDecides() {
        final var task = openTask();
        candidates(task, AFTER, "u1");
        task.setInitiator("someoneElse");
        task.setUpdatedBy("u1");         // audit field of a cockpit-side save
        final var dir = new FakeDirectory().loggedIn("u1", List.of("USER_u1"), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals(NotificationType.CANDIDATE_USER, planned.get(0).notificationType());
    }

    @Test
    void created_reportedAtDecides_notTheReportingSystemsClock() {
        final var task = openTask();
        // the workflow system's clock lags behind (or the event was delivered late), so 'createdAt'
        // is older than the cursor - yet the cockpit reported the task within this window
        task.setCreatedAt(CURSOR.minusHours(3));
        task.setReportedAt(AFTER);
        task.setCandidateGroups(List.of(group("g1")));
        final var dir = new FakeDirectory().loggedIn("u1", List.of("g1"), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals(NotificationType.CREATED, planned.get(0).notificationType());
    }

    @Test
    void created_withoutReportedAt_notReported() {
        final var task = openTask();
        task.setReportedAt(null); // reported before the property existed and not migrated
        task.setCandidateGroups(List.of(group("g1")));
        final var dir = new FakeDirectory().loggedIn("u1", List.of("g1"), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void candidateUser_knownBeforeTheCursor_notNotifiedAgain() {
        final var task = openTask();
        // the task was reported days ago and is only being changed now
        task.setReportedAt(CURSOR.minusDays(3));
        candidates(task, CURSOR.minusDays(3), "u1");
        task.setInitiator("someoneElse");
        final var dir = new FakeDirectory().loggedIn("u1", List.of("USER_u1"), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void candidateUser_addedWithinTheWindow_notified() {
        final var task = openTask();
        task.setReportedAt(CURSOR.minusDays(3));  // task itself is old ...
        candidates(task, AFTER, "u1");            // ... but u1 was just assigned
        task.setInitiator("someoneElse");
        final var dir = new FakeDirectory().loggedIn("u1", List.of("USER_u1"), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals(NotificationType.CANDIDATE_USER, planned.get(0).notificationType());
    }

    @Test
    void candidateUser_onlyTheNewCandidateIsNotified() {
        final var task = openTask();
        task.setReportedAt(CURSOR.minusDays(3));
        candidates(task, CURSOR.minusDays(3), "old");
        task.addCandidatePerson(person("fresh")); // stamped with 'now', i.e. after the cursor
        task.setInitiator("someoneElse");
        final var dir = new FakeDirectory()
                .loggedIn("old", List.of("USER_old"), true)
                .loggedIn("fresh", List.of("USER_fresh"), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals("fresh", planned.get(0).recipientUserId());
    }

    @Test
    void candidateUser_withoutTimestamp_notNotified() {
        final var task = openTask();
        task.setCandidateUsers(List.of(person("u1"))); // no 'candidateUsersSince' recorded
        task.setInitiator("someoneElse");
        final var dir = new FakeDirectory().loggedIn("u1", List.of("USER_u1"), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void excludedCandidate_notNotified() {
        final var task = openTask();
        candidates(task, AFTER, "u1");
        task.setExcludedCandidateUsers(List.of(person("u1"))); // hidden from u1's task list
        task.setInitiator("someoneElse");
        final var dir = new FakeDirectory().loggedIn("u1", List.of("USER_u1"), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void excludedUser_notNotifiedAboutACreatedTaskOfHisGroup() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        task.setExcludedCandidateUsers(List.of(person("u2")));
        final var dir = new FakeDirectory()
                .loggedIn("u1", List.of("g1"), true)
                .loggedIn("u2", List.of("g1"), true); // in the group, but excluded

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals("u1", planned.get(0).recipientUserId());
    }

    @Test
    void takenOverTaskWithoutCandidates_notifiesItsAssigneeOnly() {
        final var task = openTask();
        task.setAssignee(person("u1")); // no candidates at all: only the assignee sees this task
        final var dir = new FakeDirectory()
                .loggedIn("u1", List.of(), true)
                .loggedIn("u2", List.of(), true)
                .loggedIn("u3", List.of(), true);

        // not dangling (it has an assignee), so 'no target groups' must not mean 'everybody'
        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals("u1", planned.get(0).recipientUserId());
        assertEquals(NotificationType.CREATED, planned.get(0).notificationType());
    }

    @Test
    void assignee_isNotifiedWithoutAPersonalAuthority() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        task.setAssignee(person("u2"));
        // a user directory reporting group memberships only, i.e. no 'USER_u2' authority
        final var dir = new FakeDirectory().loggedIn("u2", List.of("g2"), true);

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals("u2", planned.get(0).recipientUserId());
    }

    @Test
    void assignee_whoCausedTheChangeHimself_notNotified() {
        final var task = openTask();
        task.setAssignee(person("u1"));
        task.setInitiator("u1");
        final var dir = new FakeDirectory().loggedIn("u1", List.of(), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void assignee_whoNeverLoggedIn_notNotified() {
        final var task = openTask();
        task.setAssignee(person("ghost"));
        final var dir = new FakeDirectory(); // nobody logged in

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void excludedAssignee_notNotified() {
        final var task = openTask();
        task.setAssignee(person("u1"));
        task.setExcludedCandidateUsers(List.of(person("u1")));
        final var dir = new FakeDirectory().loggedIn("u1", List.of(), true);

        assertTrue(scanner.scan(task, CURSOR, dir).isEmpty());
    }

    @Test
    void assigneeOfATaskWithCandidateGroups_isNotifiedPersonally() {
        final var task = openTask();
        task.setCandidateGroups(List.of(group("g1")));
        task.setAssignee(person("u2"));
        final var dir = new FakeDirectory()
                .loggedIn("u2", List.of("USER_u2"), true); // authority of the assignee himself

        final var planned = scanner.scan(task, CURSOR, dir);
        assertEquals(1, planned.size());
        assertEquals("u2", planned.get(0).recipientUserId());
    }

}
