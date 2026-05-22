package io.vanillabp.cockpit.workflowmodules.model;

import java.util.List;

public record GroupHierarchy(
        String group,
        List<String> targets) {
}
