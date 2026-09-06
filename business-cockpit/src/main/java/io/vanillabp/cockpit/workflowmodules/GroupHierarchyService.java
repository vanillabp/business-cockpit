package io.vanillabp.cockpit.workflowmodules;

import io.vanillabp.cockpit.commons.security.usercontext.GroupHierarchyResolver;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.stereotype.Service;

@Service
public class GroupHierarchyService {

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

}
