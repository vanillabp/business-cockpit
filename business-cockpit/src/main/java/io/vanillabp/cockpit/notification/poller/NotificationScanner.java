package io.vanillabp.cockpit.notification.poller;

import io.vanillabp.cockpit.notification.NotificationConfigurationResolver;
import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.spi.cockpit.usertask.NotificationDelivery;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns a changed user task into the notifications to send. It reads and changes nothing.
 * <p>
 * Given one changed user task and a {@link RecipientDirectory}, it decides the type of the
 * notification and the recipients per medium. Where two reasons overlap, it reads them like this.
 * A personal candidate gets a CANDIDATE_USER notification and no CREATED one. Other users who can
 * merely see a newly created task get CREATED, and so does the assignee of a task which was
 * reported as taken over already. The order FORCE, then SUPPRESS, then what the user configured
 * holds per recipient and medium. The class has no side effects on purpose, so it can be
 * unit-tested on its own.
 * <p>
 * Whether a user caused the change themselves is read from {@link UserTask#getInitiator()}. That
 * is the only field which says who triggered the last change. The workflow application reports it
 * per event, because it alone knows the acting user, and the cockpit sets it on its own actions.
 * {@code updatedBy} must not be used for this. It is audit information, and
 * {@code UpdateInformationEventListener} overwrites it on every save with the security context of
 * the writing request.
 */
public class NotificationScanner {

    /**
     * @param task   a user task changed since the cursor
     * @param cursor the previous scan timestamp (to tell a newly created task from an updated one)
     * @param dir    the recipient directory for this cycle
     * @return the notifications to enqueue (idempotency is handled downstream by the outbox)
     */
    public List<PlannedNotification> scan(
            final UserTask task,
            final OffsetDateTime cursor,
            final RecipientDirectory dir) {

        final var planned = new ArrayList<PlannedNotification>();

        if (task.getEndedAt() != null) {
            // deliberately no CREATED for a task reported and ended within the same cycle: there is
            // nothing left to work on, so only the end of the task is worth a notification
            planEndOfTask(task, dir, planned);
            return planned;
        }

        planCandidateUsers(task, cursor, dir, planned);
        // 'reportedAt' and not 'createdAt'. The cursor moves with the cockpit's clock, while
        // 'createdAt' is the timestamp of the reporting workflow system. Comparing the two drops
        // the notification of a task whose event arrived late, or was stamped by a clock which
        // runs behind
        if (task.getReportedAt() != null && task.getReportedAt().isAfter(cursor)) {
            planCreated(task, dir, planned);
        }
        return planned;

    }

    private void planEndOfTask(
            final UserTask task,
            final RecipientDirectory dir,
            final List<PlannedNotification> planned) {

        final var assignee = assigneeId(task);
        if (assignee == null || !dir.isLoggedIn(assignee)) {
            return;
        }
        if (task.getEndReason() == UserTaskEndReason.COMPLETED) {
            // notify only where somebody else completed it
            if (!Objects.equals(assignee, task.getInitiator())) {
                planForRecipient(task, assignee, NotificationType.COMPLETED, dir, planned);
            }
        } else if (task.getEndReason() == UserTaskEndReason.CANCELLED) {
            // the process cancelled it
            planForRecipient(task, assignee, NotificationType.CANCELED, dir, planned);
        }

    }

    private void planCandidateUsers(
            final UserTask task,
            final OffsetDateTime cursor,
            final RecipientDirectory dir,
            final List<PlannedNotification> planned) {

        final var excluded = excludedCandidateUserIds(task);
        for (final var candidate : candidateUserIds(task)) {
            if (excluded.contains(candidate)) {
                continue; // excluded candidates do not see the task in their task list
            }
            // a candidate is notified when they become one, and not on every later change of
            // the task. A timestamp nobody knows counts as known before, so a task changed long
            // after the candidate was notified does not produce a second message
            final var candidateSince = task.getCandidateSince(candidate);
            if (candidateSince == null || !candidateSince.isAfter(cursor)) {
                continue;
            }
            // leave out the user who caused the change
            if (dir.isLoggedIn(candidate) && !Objects.equals(candidate, task.getInitiator())) {
                planForRecipient(task, candidate, NotificationType.CANDIDATE_USER, dir, planned);
            }
        }

    }

    private void planCreated(
            final UserTask task,
            final RecipientDirectory dir,
            final List<PlannedNotification> planned) {

        final var personalCandidates = candidateUserIds(task);
        final var excluded = excludedCandidateUserIds(task);
        final var assignee = assigneeId(task);
        final var targetGroups = task.getTargetGroups();
        for (final var userId : dir.loggedInUserIds()) {
            if (personalCandidates.contains(userId)) {
                continue; // already handled as CANDIDATE_USER
            }
            if (excluded.contains(userId)) {
                continue; // excluded candidates do not see the task in their task list
            }
            if (isAddressedPersonally(task, userId, assignee)
                    || isVisibleTo(task, targetGroups, dir.authoritiesOf(userId))) {
                planForRecipient(task, userId, NotificationType.CREATED, dir, planned);
            }
        }

    }

    /**
     * Whether a newly reported task is addressed to that very user, which holds for its assignee.
     * The task was reported as taken over by them, so they see it whatever authorities the user
     * directory reports for them. Only some directories report a personal {@code USER_<id>}
     * authority. As with a personal candidate, no notification is due where the user caused the
     * change themselves.
     */
    private static boolean isAddressedPersonally(
            final UserTask task,
            final String userId,
            final String assignee) {

        return Objects.equals(userId, assignee)
                && !Objects.equals(userId, task.getInitiator());

    }

    private static boolean isVisibleTo(
            final UserTask task,
            final java.util.Collection<String> targetGroups,
            final List<String> authorities) {

        if (task.isDangling()) {
            return true; // nobody is candidate of or has taken over the task: visible to everyone
        }
        if (targetGroups == null) {
            // not dangling, yet no target groups: the task was taken over and has no candidates, so
            // only its assignee sees it. 'dangling' and 'no target groups' are not the same thing.
            return false;
        }
        if (authorities == null) {
            return false; // cannot determine visibility (no user directory)
        }
        return authorities.stream().anyMatch(targetGroups::contains);

    }

    private void planForRecipient(
            final UserTask task,
            final String userId,
            final NotificationType type,
            final RecipientDirectory dir,
            final List<PlannedNotification> planned) {

        final var delivery = task.getNotificationDelivery(); // null == USER_CONFIG
        if (delivery == NotificationDelivery.SUPPRESS) {
            return; // force > suppress > user-config
        }
        final var forced = delivery == NotificationDelivery.FORCE;
        for (final var medium : dir.mediaTypes()) {
            if (forced
                    || NotificationConfigurationResolver.shouldNotify(
                            dir.configOf(userId),
                            task.getWorkflowModuleId(),
                            task.getBpmnProcessId(),
                            medium)) {
                planned.add(new PlannedNotification(task.getId(), type, medium, userId, forced));
            }
        }

    }

    private static String assigneeId(
            final UserTask task) {

        return task.getAssignee() == null ? null : task.getAssignee().getId();

    }

    private static Set<String> candidateUserIds(
            final UserTask task) {

        return personIds(task.getCandidateUsers());

    }

    /**
     * The users a task keeps out of its candidates by name. No work is waiting for them, so they
     * must not be notified about it, and that stays true where the same task admits them as a
     * reader.
     */
    private static Set<String> excludedCandidateUserIds(
            final UserTask task) {

        return personIds(task.getExcludedCandidateUsers());

    }

    private static Set<String> personIds(
            final List<Person> persons) {

        return Optional.ofNullable(persons)
                .orElse(List.of())
                .stream()
                .map(Person::getId)
                .collect(Collectors.toSet());

    }

}
