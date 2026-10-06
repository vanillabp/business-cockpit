package io.vanillabp.cockpit.workflowlist;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Which workflows one view of the cockpit lets a user reach. The list of that view is built from
 * it, and so are the requests naming a single workflow, so an id the list would not have shown is
 * answered as unknown rather than opening a way past the list.
 *
 * <p>A workflow is visible once it names one of the {@code accessibleToUsers} or one of the
 * {@code accessibleToGroups}. A collection left null or empty drops its reason instead of widening
 * the view. A visibility which names nobody restricts nothing, and {@link #everyWorkflow()} is the
 * name for that case. {@code includeDanglingWorkflows} decides what happens to a workflow which
 * names neither users nor groups. Once it is set, such a workflow counts as open to everybody.
 *
 * <p>The values a workflow is matched against are what its {@code @WorkflowDetailsProvider}
 * reported, so an application built on the cockpit answers here with whatever it told the cockpit
 * back then.
 *
 * <p>The rule exists twice, as the query of {@code WorkflowlistService.buildWorkflowlistCriteria}
 * and as {@link #letsThrough(WorkflowFacts)} for the update streams.
 * {@code TheVisibilityInMemoryAgreesWithTheQueryTest} holds the two together, the same way it does
 * for {@code UserTaskVisibility}.
 */
public record WorkflowVisibility(
        boolean includeDanglingWorkflows,
        Collection<String> accessibleToUsers,
        Collection<String> accessibleToGroups) {

    /**
     * The workflows addressed to the user, either by name or through one of their groups, plus the
     * ones which address nobody and are therefore open to everybody.
     */
    public static WorkflowVisibility workflowsAddressedTo(
            final UserDetails user) {

        return new WorkflowVisibility(true, List.of(user.getId()), user.getAuthorities());

    }

    /** Every workflow, whoever it addresses. */
    public static WorkflowVisibility everyWorkflow() {

        return new WorkflowVisibility(false, null, null);

    }

    /**
     * Whether this visibility lets the workflow through. It is the rule of
     * {@code WorkflowlistService.buildWorkflowlistCriteria}, for a list of every workflow, ended or
     * not, and it has to give the same answer.
     */
    public boolean letsThrough(
            final WorkflowFacts workflow) {

        final var reasons = new java.util.ArrayList<Boolean>();
        if ((accessibleToUsers != null) && !accessibleToUsers.isEmpty()) {
            reasons.add(idsOf(workflow.accessibleToUsers(), Person::getId)
                    .anyMatch(accessibleToUsers::contains));
        }
        if ((accessibleToGroups != null) && !accessibleToGroups.isEmpty()) {
            reasons.add(idsOf(workflow.accessibleToGroups(), Group::getId)
                    .anyMatch(accessibleToGroups::contains));
        }
        if (reasons.isEmpty()) {
            return true;
        }
        if (includeDanglingWorkflows) {
            reasons.add(Boolean.TRUE.equals(workflow.dangling()));
        }
        return reasons.stream().anyMatch(Boolean::booleanValue);

    }

    private static <T> Stream<String> idsOf(
            final List<T> entries,
            final java.util.function.Function<T, String> id) {

        return entries == null
                ? Stream.empty()
                : entries.stream().filter(Objects::nonNull).map(id);

    }

    /**
     * What {@link #letsThrough(WorkflowFacts)} reads of a workflow, and nothing else. The update
     * streams read exactly these fields of a changed workflow from the database. {@code dangling}
     * is the value stored in the document, which is what the query compares.
     */
    public record WorkflowFacts(
            String id,
            List<Person> accessibleToUsers,
            List<Group> accessibleToGroups,
            Boolean dangling) {

        /** The names of the fields, which are the fields the update streams read. */
        public static List<String> fieldNames() {

            return Stream
                    .of(WorkflowFacts.class.getRecordComponents())
                    .map(java.lang.reflect.RecordComponent::getName)
                    .toList();

        }

    }

}
