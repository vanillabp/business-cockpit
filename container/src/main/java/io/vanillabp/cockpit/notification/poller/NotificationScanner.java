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
 * Pure classification of a changed user task into the notifications to send (AC func 2).
 * <p>
 * Given one changed user task and a {@link RecipientDirectory}, decides the notification type and
 * recipients per medium. Interpretation of overlapping conditions: a personal candidate gets a
 * CANDIDATE_USER notification (not also a CREATED one); other users who can merely see a newly
 * created task get CREATED. Delivery precedence FORCE &gt; SUPPRESS &gt; user-config is applied per
 * (recipient, medium). Deliberately side-effect free so it is unit-testable in isolation.
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
            planEndOfTask(task, dir, planned);
            return planned;
        }

        planCandidateUsers(task, dir, planned);
        if (task.getCreatedAt() != null && task.getCreatedAt().isAfter(cursor)) {
            planCreated(task, dir, planned);
        }
        return planned;

    }

    private void planEndOfTask(
            final UserTask task,
            final RecipientDirectory dir,
            final List<PlannedNotification> planned) {

        final var assignee = task.getAssignee() == null ? null : task.getAssignee().getId();
        if (assignee == null || !dir.isLoggedIn(assignee)) {
            return;
        }
        if (task.getEndReason() == UserTaskEndReason.COMPLETED) {
            // notify only if completed by someone else (AC func 2c)
            if (!Objects.equals(assignee, task.getUpdatedBy())) {
                planForRecipient(task, assignee, NotificationType.COMPLETED, dir, planned);
            }
        } else if (task.getEndReason() == UserTaskEndReason.CANCELLED) {
            // cancelled by the process (AC func 2d)
            planForRecipient(task, assignee, NotificationType.CANCELED, dir, planned);
        }

    }

    private void planCandidateUsers(
            final UserTask task,
            final RecipientDirectory dir,
            final List<PlannedNotification> planned) {

        for (final var candidate : candidateUserIds(task)) {
            // exclude the user who caused the change (AC func 2b: "unless caused by the user himself")
            if (dir.isLoggedIn(candidate) && !Objects.equals(candidate, task.getUpdatedBy())) {
                planForRecipient(task, candidate, NotificationType.CANDIDATE_USER, dir, planned);
            }
        }

    }

    private void planCreated(
            final UserTask task,
            final RecipientDirectory dir,
            final List<PlannedNotification> planned) {

        final var personalCandidates = candidateUserIds(task);
        final var targetGroups = task.getTargetGroups(); // null == visible to everyone
        for (final var userId : dir.loggedInUserIds()) {
            if (personalCandidates.contains(userId)) {
                continue; // already handled as CANDIDATE_USER
            }
            if (isVisibleTo(targetGroups, dir.authoritiesOf(userId))) {
                planForRecipient(task, userId, NotificationType.CREATED, dir, planned);
            }
        }

    }

    private static boolean isVisibleTo(
            final java.util.Collection<String> targetGroups,
            final List<String> authorities) {

        if (targetGroups == null) {
            return true; // dangling task: visible to everyone
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

    private static Set<String> candidateUserIds(
            final UserTask task) {

        return Optional.ofNullable(task.getCandidateUsers())
                .orElse(List.of())
                .stream()
                .map(Person::getId)
                .collect(Collectors.toSet());

    }

}
