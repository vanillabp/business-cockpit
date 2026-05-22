package io.vanillabp.cockpit.commons.security.usercontext;

import java.util.Collection;
import java.util.Map;

public interface WorkflowModuleGroupHierarchy {

    Map<String, Collection<String>> getGroupHierarchy();

}
