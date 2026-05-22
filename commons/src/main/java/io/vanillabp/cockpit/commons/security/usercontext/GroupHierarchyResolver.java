package io.vanillabp.cockpit.commons.security.usercontext;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupHierarchyResolver {

    /**
     * Resolves effective groups based on a list of hierarchies and assigned groups.
     *
     * @param hierarchies List of group hierarchies
     * @param assignedGroups Groups a user is assigned to
     * @return All effective groups (assigned plus transitive resolved)
     */
    public static Collection<String> resolveGroups(
            final Collection<Map<String, Collection<String>>> hierarchies,
            final Collection<String> assignedGroups) {

        if ((hierarchies == null) || hierarchies.isEmpty()) {
            return assignedGroups;
        }
        if ((assignedGroups == null) || assignedGroups.isEmpty()) {
            return List.of();
        }

        Map<String, Set<String>> merged = new HashMap<>();
        for (Map<String, Collection<String>> hierarchy : hierarchies) {
            if (hierarchy == null) {
                continue;
            }
            hierarchy.forEach((role, targets) ->
                    merged.computeIfAbsent(role, k -> new HashSet<>()).addAll(targets)
            );
        }

        Set<String> resolved = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>(assignedGroups);

        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (resolved.add(current)) {
                Set<String> targets = merged.get(current);
                if (targets != null) {
                    stack.addAll(targets);
                }
            }
        }

        return resolved;
    }

}
