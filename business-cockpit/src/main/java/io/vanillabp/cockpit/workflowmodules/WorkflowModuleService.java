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
        if ((groupHierarchy != null) && (workflowModule.getGroupHierarchy() == null)) return true;
        if ((groupHierarchy == null) && (workflowModule.getGroupHierarchy() != null)) return true;
        if ((groupHierarchy != null) && !groupHierarchy.equals(workflowModule.getGroupHierarchy())) return true;
        return false;

    }

    public WorkflowModule getWorkflowModule(
            final String id) {

        if (id == null) {
            return null;
        }
        return workflowModules
                .findById(id)
                .orElse(null);

    }

    public List<WorkflowModule> getWorkflowModules(List<String> userRoles) {

        return workflowModules.findByAccessibleToGroups(userRoles);

    }

}
