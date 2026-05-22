package io.vanillabp.cockpit.commons.security.usercontext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GroupHierarchyResolverTest {

    @Test
    public void testTransitiveGroupInWrongOrder() {

        final Map<String, Collection<String>> groupHierarchy =
                Map.of("Business", List.of("User"),
                    "Admin", List.of("Business"));

        final var resolvedGroups = GroupHierarchyResolver.resolveGroups(
                List.of(groupHierarchy), List.of("Admin"));

        Assertions.assertTrue(resolvedGroups.contains("Admin"));
        Assertions.assertTrue(resolvedGroups.contains("Business"));
        Assertions.assertTrue(resolvedGroups.contains("User"));

    }

}
