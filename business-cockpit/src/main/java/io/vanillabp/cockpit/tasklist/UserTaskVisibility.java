package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.util.Collection;
import java.util.List;

/**
 * Which user tasks one view of the cockpit lets a user reach. The list of that view is built from
 * it, and so is every request naming a single task, so an id the list would not have shown is
 * answered as unknown rather than opening a way past the list.
 *
 * <p>A task is visible once one of the reasons named here holds: it is assigned to one of the
 * {@code assignees}, it names one of the {@code candidateUsers}, it names one of the
 * {@code candidateGroups}, one of the {@code openedBy} users opened it before, or it addresses
 * nobody at all and {@code includeDanglingTasks} is set. A collection left null or empty drops its
 * reason instead of widening the view. Once no reason is left the view is unrestricted, which is
 * what {@link #everyUserTask()} says out loud.
 *
 * <p>{@code openedBy} is the reason which does not follow from what the task says today. A person
 * who opened a task keeps it whatever happened to their roles since, which is how somebody can
 * still read what they entered after the group which brought them the task was taken away. The
 * cockpit notes the opening itself, and {@code business-cockpit.opened-tasks-stay-visible} says
 * where that is wanted.
 *
 * <p>Of the visibilities below only {@link #everythingTheUserMayWorkOn(UserDetails)} carries that
 * reason. It is the view of everything a person has to do with, and the narrower ones stay what
 * their names say: what is addressed to somebody today, and what their groups may take.
 *
 * <p>Two of the values take tasks away again. {@code notInAssignees} turns the assignee reason
 * around, so the view holds what those users have not taken yet, which is how work still up for
 * grabs is described. {@code excludedCandidates} names users a task can keep out through its own
 * {@code excludedCandidateUsers}, which is how a workflow module enforces four eyes. Both of them
 * hold against every reason, {@code openedBy} included, so an earlier view never carries a user
 * past the exclusion a workflow module wrote.
 *
 * <p>An application built on the cockpit picks one of the visibilities below or writes its own, and
 * the controller carrying it needs nothing else.
 */
public record UserTaskVisibility(
        boolean includeDanglingTasks,
        boolean notInAssignees,
        Collection<String> assignees,
        Collection<String> candidateUsers,
        Collection<String> candidateGroups,
        Collection<String> excludedCandidates,
        Collection<String> openedBy) {

    /**
     * Everything the user could take a hand in: their own tasks, the ones naming them or one of
     * their groups, the ones they opened before, and the ones addressed to nobody. A task naming
     * the user as excluded is left out whatever else it says.
     */
    public static UserTaskVisibility everythingTheUserMayWorkOn(
            final UserDetails user) {

        return everythingTheUserMayWorkOn(user.getId(), user.getAuthorities());

    }

    /**
     * The same view as {@link #everythingTheUserMayWorkOn(UserDetails)} for a caller which holds
     * the user's id and groups instead of the user.
     */
    public static UserTaskVisibility everythingTheUserMayWorkOn(
            final String userId,
            final Collection<String> usersGroups) {

        return new UserTaskVisibility(
                true, false, List.of(userId), List.of(userId), usersGroups, List.of(userId),
                List.of(userId));

    }

    /**
     * What is the user's own: assigned to them, or naming them personally. Their groups do not
     * count here, and neither do the tasks addressed to nobody, and neither does a task they once
     * opened: this view says what is addressed to the person today.
     */
    public static UserTaskVisibility onlyWhatIsTheUsersOwn(
            final UserDetails user) {

        return new UserTaskVisibility(
                false, false, List.of(user.getId()), List.of(user.getId()), null,
                List.of(user.getId()), null);

    }

    /**
     * What the user's groups may take and the user has not taken yet, together with the tasks
     * addressed to nobody. A task the user has already claimed is gone from this view, which is
     * what makes it read as a pile of open work.
     *
     * <p>What the user opened before does not count here. This view says which work is theirs to
     * take, and a group they are no longer in is not work offered to them.
     */
    public static UserTaskVisibility whatTheUsersGroupsMayTake(
            final UserDetails user) {

        return new UserTaskVisibility(
                true, true, List.of(user.getId()), null, user.getAuthorities(),
                List.of(user.getId()), null);

    }

    /**
     * Every user task, whoever it addresses. Meant for the places which show what a workflow is up
     * to rather than what the reader has to do.
     */
    public static UserTaskVisibility everyUserTask() {

        return new UserTaskVisibility(false, false, null, null, null, null, null);

    }

}
