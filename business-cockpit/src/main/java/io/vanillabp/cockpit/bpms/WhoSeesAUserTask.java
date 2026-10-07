package io.vanillabp.cockpit.bpms;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Which report says who sees a user task.
 * <p>
 * Four fields say who sees a task: the assignee, the candidate users, the candidate groups and the
 * excluded candidate users. The report which creates the task in the cockpit sets them. After that
 * no report changes them. Only the cockpit itself still changes the assignee and the candidate
 * users, when somebody claims the task or assigns it to somebody.
 * <p>
 * Who sees a task is part of what the task is. If it changed while the task is open, nobody could
 * tell later why a person worked on it. So a workflow module which wants other people to work on a
 * task ends it, for example with a boundary event, and enters it again. The new task brings the new
 * people along.
 * <p>
 * The admitted users are not part of this. Every report may set them, because they only add
 * readers and take the task away from nobody.
 * <p>
 * The report which creates the task is usually the creation, but not always. The outbox of a
 * workflow module puts its entries in no order, so a change or an end can arrive first and create
 * the task (see decision 36 and decision 45 in the repository's DECISIONS.md). That is why a change
 * and an end carry the four fields as well. The cockpit uses them only where the report creates
 * the task.
 * <p>
 * There is one exception. A task which names no assignee, no candidate user and no candidate group
 * is dangling ({@link UserTask#isDangling()}), and the cockpit shows it to everybody. Such a task
 * takes who sees it from the first later report which names somebody. Version 1.1 of the API
 * carries all values in every report, so this happens with reports of version 1, which carry none of
 * the four fields in an end, and with ends of a BPMS which could no longer describe the task. The
 * task stops being dangling then, because the cockpit reads that flag from the fields whenever it
 * stores the task.
 * <p>
 * The cockpit does not warn about a report which names other people. A workflow module cannot
 * know whether the cockpit already holds the task, so such a report is no mistake. A DEBUG line
 * says that it happened.
 */
public final class WhoSeesAUserTask {

    private static final Logger logger = LoggerFactory.getLogger(WhoSeesAUserTask.class);

    private WhoSeesAUserTask() {
    }

    /**
     * The four fields which say who sees a task, copied, so that a mapper which fills a list in
     * place does not change them.
     */
    record Visibility(
            Person assignee,
            List<Person> candidateUsers,
            List<UserTask.CandidateSince> candidateUsersSince,
            List<Group> candidateGroups,
            List<Person> excludedCandidateUsers) {

        static Visibility of(
                final UserTask task) {

            return new Visibility(
                    task.getAssignee(),
                    copy(task.getCandidateUsers()),
                    copy(task.getCandidateUsersSince()),
                    copy(task.getCandidateGroups()),
                    copy(task.getExcludedCandidateUsers()));

        }

        void putBackOnto(
                final UserTask task) {

            task.setAssignee(assignee);
            task.setCandidateUsers(copy(candidateUsers));
            task.setCandidateUsersSince(copy(candidateUsersSince));
            task.setCandidateGroups(copy(candidateGroups));
            task.setExcludedCandidateUsers(copy(excludedCandidateUsers));

        }

        /**
         * Compares the fields a report fills with the stored ones. A field the report leaves
         * empty is not compared: a claim in the cockpit sets an assignee which no workflow module
         * reports back, and that is no difference worth a line in the log.
         */
        boolean namesOtherPeopleThan(
                final Visibility stored) {

            return differs(idOf(assignee), idOf(stored.assignee))
                    || differs(idsOf(candidateUsers, Person::getId), idsOf(stored.candidateUsers, Person::getId))
                    || differs(idsOf(candidateGroups, Group::getId), idsOf(stored.candidateGroups, Group::getId))
                    || differs(
                            idsOf(excludedCandidateUsers, Person::getId),
                            idsOf(stored.excludedCandidateUsers, Person::getId));

        }

        private static boolean differs(
                final String reported,
                final String stored) {

            return (reported != null)
                    && !reported.equals(stored);

        }

        private static boolean differs(
                final List<String> reported,
                final List<String> stored) {

            return !reported.isEmpty()
                    && !reported.equals(stored);

        }

    }

    /**
     * Lays a change or an end onto a task the cockpit already holds, and keeps who sees the task as
     * it is. Whatever else the report says is stored as the mapping stores it. A dangling task takes
     * who sees it from the report, where the report names somebody.
     *
     * @param stored The task the cockpit holds, changed in place
     * @param kindOfReport What the report is, for the log, like "change" or "end"
     * @param report Lays the report onto the stored task
     */
    public static void keepWhoSeesTheStoredTask(
            final UserTask stored,
            final String kindOfReport,
            final Consumer<UserTask> report) {

        final var wasDangling = stored.isDangling();
        final var asStored = Visibility.of(stored);
        report.accept(stored);
        if (wasDangling && !stored.isDangling()) {
            tookWhoSeesTheTask(stored, kindOfReport);
            return;
        }
        final var asReported = Visibility.of(stored);
        asStored.putBackOnto(stored);
        if (asReported.namesOtherPeopleThan(asStored)) {
            logDifference(stored.getId(), kindOfReport);
        }

    }

    /**
     * Decides who sees a task once its creation arrives late, after a change or an end created the
     * task. The task keeps who sees it, unless it is dangling and the creation names somebody.
     *
     * @param stored The task the cockpit holds, changed in place
     * @param asReported Builds the task as the creation describes it
     * @return Whether the stored task took who sees it from the creation, and has to be saved
     */
    public static boolean takeFromALateCreation(
            final UserTask stored,
            final Supplier<UserTask> asReported) {

        if (!stored.isDangling()) {
            if (logger.isDebugEnabled()
                    && Visibility.of(asReported.get()).namesOtherPeopleThan(Visibility.of(stored))) {
                logDifference(stored.getId(), "creation");
            }
            return false;
        }
        final var creation = asReported.get();
        if (creation.isDangling()) {
            return false;
        }
        Visibility.of(creation).putBackOnto(stored);
        tookWhoSeesTheTask(stored, "creation");
        return true;

    }

    /**
     * Decides who sees a task the cockpit knew from its end alone, once the creation arrives. The
     * end created the task, so who sees it stays as the end said. A dangling task is the exception
     * here as well: then the creation says who sees it, as it fills in everything else the end
     * could not report.
     *
     * @param knownFromItsEnd The task as the end stored it
     * @param creation The task as the creation reports it, changed in place
     */
    public static void keepWhatTheEndSaid(
            final UserTask knownFromItsEnd,
            final UserTask creation) {

        if (knownFromItsEnd.isDangling()) {
            return;
        }
        if (Visibility.of(creation).namesOtherPeopleThan(Visibility.of(knownFromItsEnd))) {
            logDifference(creation.getId(), "creation");
        }
        Visibility.of(knownFromItsEnd).putBackOnto(creation);

    }

    /**
     * The candidate users of a dangling task are new to the cockpit, so they are stamped as known
     * from now on. That is what a notification about a new candidate reads.
     */
    private static void tookWhoSeesTheTask(
            final UserTask task,
            final String kindOfReport) {

        task.stampCandidatesSince(OffsetDateTime.now());
        logger.debug(
                "User task '{}' named nobody, so the {} says who sees it",
                task.getId(),
                kindOfReport);

    }

    private static void logDifference(
            final String userTaskId,
            final String kindOfReport) {

        logger.debug(
                "The {} of user task '{}' names other people than the cockpit holds. They are not "
                        + "used: the report which created the task decides who sees it",
                kindOfReport,
                userTaskId);

    }

    private static <T> List<T> copy(
            final List<T> list) {

        return list == null ? null : new ArrayList<>(list);

    }

    private static boolean isFilled(
            final List<?> list) {

        return (list != null)
                && !list.isEmpty();

    }

    private static String idOf(
            final Person person) {

        return person == null ? null : person.getId();

    }

    private static <T> List<String> idsOf(
            final List<T> list,
            final Function<T, String> id) {

        return Optional
                .ofNullable(list)
                .orElse(List.of())
                .stream()
                .map(id)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

    }

}
