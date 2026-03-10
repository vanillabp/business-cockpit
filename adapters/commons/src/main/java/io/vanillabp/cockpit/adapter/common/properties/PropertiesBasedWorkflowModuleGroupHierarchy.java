package io.vanillabp.cockpit.adapter.common.properties;

import io.vanillabp.cockpit.commons.security.usercontext.WorkflowModuleGroupHierarchy;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public class PropertiesBasedWorkflowModuleGroupHierarchy
        implements WorkflowModuleGroupHierarchy {

    final Map<String, Collection<String>> mergedGroupHierarchy;

    public PropertiesBasedWorkflowModuleGroupHierarchy(
            final VanillaBpCockpitProperties properties) {

        final Map<String, Set<String>> merged = new HashMap<>();
        properties
                .getWorkflowModules()
                .values()
                .stream()
                .map(VanillaBpCockpitProperties.WorkflowModuleAdapterProperties::getCockpit)
                .filter(props -> !StringUtils.hasText(props.getGroupHierarchyBeanName()))
                .map(VanillaBpCockpitProperties.WorkflowModuleCockpitAdapterProperties::getGroupHierarchy)
                .forEach(hierarchy -> {
                    if (hierarchy == null) {
                        return;
                    }
                    hierarchy.forEach((role, targets) ->
                            merged.computeIfAbsent(role, k -> new HashSet<>()).addAll(targets)
                    );
                });

        mergedGroupHierarchy = merged
                .entrySet()
                .stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), new LinkedList<>(e.getValue())), HashMap::putAll);

    }

    @Override
    public Map<String, Collection<String>> getGroupHierarchy() {

        return mergedGroupHierarchy;

    }

}
