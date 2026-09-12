package io.vanillabp.cockpit.workflowmodules;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowmodules.model.GroupHierarchy;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModuleRepository;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class WorkflowModuleService {

    @Autowired
    private WorkflowModuleRepository workflowModules;

    @Autowired
    private MicroserviceProxyRegistry microserviceProxyRegistry;

    @PostConstruct
    public void registerProxiesForWorkflowModules() {

        // prefill cache
        final var all = workflowModules.findAll();

        all.forEach(workflowModule -> {
            final var groupHierarchy = Optional
                    .ofNullable(workflowModule.getGroupHierarchy())
                    .stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toMap(GroupHierarchy::group, hierarchy -> (Collection<String>) hierarchy.targets()));
            GroupHierarchyService.putGroupHierarchy(workflowModule.getId(), groupHierarchy);
        });

        microserviceProxyRegistry.registerMicroservices(all
                .stream()
                .collect(Collectors.toMap(
                        WorkflowModule::getId,
                        WorkflowModule::getUri)));

    }

    public boolean registerOrUpdateWorkflowModule(
            final String id,
            final String uri,
            final String taskProviderApiUriPath,
            final String workflowProviderApiUriPath,
            final List<String> accessibleToGroups,
            final Map<String, Collection<String>> groupHierarchy) {

        try {
            return updateWorkflowModule(id, uri, taskProviderApiUriPath, workflowProviderApiUriPath,
                    accessibleToGroups, groupHierarchy);
        } catch (OptimisticLockingFailureException e) {
            // another node registered the same module concurrently: read the now current state and
            // apply the registration once more
            return updateWorkflowModule(id, uri, taskProviderApiUriPath, workflowProviderApiUriPath,
                    accessibleToGroups, groupHierarchy);
        }

    }

    /**
     * @return whether anything had to be written; a registration repeating what is already stored
     *         leaves the document untouched
     */
    private boolean updateWorkflowModule(
            final String id,
            final String uri,
            final String taskProviderApiUriPath,
            final String workflowProviderApiUriPath,
            final List<String> accessibleToGroups,
            final Map<String, Collection<String>> groupHierarchy) {

        final var workflowModule = workflowModules
                .findById(id)
                .orElseGet(() -> WorkflowModule.withId(id));

        if (!hasChanged(workflowModule, uri, taskProviderApiUriPath, workflowProviderApiUriPath,
                accessibleToGroups, groupHierarchy)) {
            return false;
        }

        workflowModule.setUri(uri);
        workflowModule.setTaskProviderApiUriPath(taskProviderApiUriPath);
        workflowModule.setWorkflowProviderApiUriPath(workflowProviderApiUriPath);
        workflowModule.setAccessibleToGroups(accessibleToGroups);
        final var modelGroupHierarchy = Optional
                .ofNullable(groupHierarchy)
                .map(hierarchy -> hierarchy
                        .entrySet()
                        .stream()
                        .map(entry -> new GroupHierarchy(entry.getKey(), new LinkedList<>(entry.getValue())))
                        .toList())
                .orElse(null);
        workflowModule.setGroupHierarchy(modelGroupHierarchy);
        GroupHierarchyService.putGroupHierarchy(workflowModule.getId(), groupHierarchy);

        final var saved = workflowModules.save(workflowModule);

        microserviceProxyRegistry.registerMicroservice(
                saved.getId(),
                saved.getUri());

        return true;

    }

    private boolean hasChanged(
            final WorkflowModule workflowModule,
            final String uri,
            final String taskProviderApiUriPath,
            final String workflowProviderApiUriPath,
            final List<String> accessibleToGroups,
            final Map<String, Collection<String>> groupHierarchy) {

        if ((uri == null) && (workflowModule.getUri() != null)) return true;
        if ((uri != null) && (workflowModule.getUri() == null)) return true;
        if ((uri != null) && !uri.equals(workflowModule.getUri())) return true;
        if ((taskProviderApiUriPath == null) && (workflowModule.getTaskProviderApiUriPath() != null)) return true;
        if ((taskProviderApiUriPath != null) && (workflowModule.getTaskProviderApiUriPath() == null)) return true;
        if ((taskProviderApiUriPath != null) && !taskProviderApiUriPath.equals(workflowModule.getTaskProviderApiUriPath())) return true;
        if ((workflowProviderApiUriPath == null) && (workflowModule.getWorkflowProviderApiUriPath() != null)) return true;
        if ((workflowProviderApiUriPath != null) && (workflowModule.getWorkflowProviderApiUriPath() == null)) return true;
        if ((workflowProviderApiUriPath != null) && !workflowProviderApiUriPath.equals(workflowModule.getWorkflowProviderApiUriPath())) return true;
        if ((accessibleToGroups == null) && (workflowModule.getAccessibleToGroups() != null)) return true;
        if ((accessibleToGroups != null) && (workflowModule.getAccessibleToGroups() == null)) return true;
        if ((accessibleToGroups != null) && !accessibleToGroups.equals(workflowModule.getAccessibleToGroups())) return true;
        if (!Objects.equals(
                asComparableHierarchy(groupHierarchy),
                asComparableHierarchy(workflowModule.getGroupHierarchy()))) return true;
        return false;

    }

    /**
     * A registration brings the hierarchy as a map, the document keeps it as a list of entries, so
     * both have to be brought into one shape before they can be compared. Otherwise every
     * re-registration of a module having a hierarchy would look like a change and rewrite the
     * document.
     */
    private static Map<String, List<String>> asComparableHierarchy(
            final Map<String, Collection<String>> groupHierarchy) {

        if (groupHierarchy == null) {
            return null;
        }
        return groupHierarchy
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));

    }

    private static Map<String, List<String>> asComparableHierarchy(
            final List<GroupHierarchy> groupHierarchy) {

        if (groupHierarchy == null) {
            return null;
        }
        return groupHierarchy
                .stream()
                .collect(Collectors.toMap(GroupHierarchy::group, entry -> List.copyOf(entry.targets())));

    }

    /**
     * One workflow module by its id, with nobody asked whether the caller may see it. Used where
     * the cockpit needs the registration itself, for instance to route a proxied request.
     * Everything answering a person goes through the overload taking a
     * {@link WorkflowModuleVisibility}.
     */
    public WorkflowModule getWorkflowModule(
            final String id) {

        if (id == null) {
            return null;
        }
        return workflowModules
                .findById(id)
                .orElse(null);

    }

    /**
     * The module of that id which the given visibility lets through, or {@code null} when there is
     * none. The visibility goes into the query the same way it goes into the query behind the list,
     * so the two cannot drift apart.
     */
    public WorkflowModule getWorkflowModule(
            final WorkflowModuleVisibility visibility,
            final String id) {

        if (id == null) {
            return null;
        }
        if (visibility.accessibleToGroups() == null) {
            return getWorkflowModule(id);
        }
        return workflowModules
                .findByIdAndAccessibleToGroups(id, List.copyOf(visibility.accessibleToGroups()))
                .orElse(null);

    }

    public List<WorkflowModule> getWorkflowModules(
            final WorkflowModuleVisibility visibility) {

        if (visibility.accessibleToGroups() == null) {
            return workflowModules.findAll();
        }
        return workflowModules.findByAccessibleToGroups(
                List.copyOf(visibility.accessibleToGroups()));

    }

}
