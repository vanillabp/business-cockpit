package io.vanillabp.cockpit.workflowmodules;

import io.vanillabp.cockpit.commons.security.usercontext.GroupHierarchyResolver;
import io.vanillabp.cockpit.commons.security.usercontext.WorkflowModuleGroupHierarchy;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.stereotype.Service;

@Service
public class GroupHierarchyService implements WorkflowModuleGroupHierarchy {

    private static final Map<String, Map<String, Collection<String>>> cachedGroupHierarchies = new HashMap<>();

    private static final Lock readLock;

    private static final Lock writeLock;

    static {

        final var readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();

    }

    public static void putGroupHierarchy(
            final String workflowModuleId,
            final Map<String, Collection<String>> groupHierarchy) {

        try {
            writeLock.lock();

            cachedGroupHierarchies.put(workflowModuleId, groupHierarchy);

        } finally {
            writeLock.unlock();
        }

    }

    public Collection<String> resolveGroups(
            final Collection<String> assignedGroups) {

        try {
            readLock.lock();

            return GroupHierarchyResolver.resolveGroups(
                    cachedGroupHierarchies.values(),
                    assignedGroups);

        } finally {
            readLock.unlock();
        }

    }

    /**
     * @return Merged hierarchy for general purpose usage
     */
    @Override
    public Map<String, Collection<String>> getGroupHierarchy() {

        try {
            readLock.lock();

            final Map<String, Collection<String>> merged = new HashMap<>();
            cachedGroupHierarchies
                    .values()
                    .forEach(hierarchy -> {
                        hierarchy.forEach((role, targets) ->
                                merged.computeIfAbsent(role, k -> new HashSet<>()).addAll(targets));
                    });
            return merged;

        } finally {
            readLock.unlock();
        }

    }

}
