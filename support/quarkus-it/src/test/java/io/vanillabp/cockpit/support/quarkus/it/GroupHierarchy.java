package io.vanillabp.cockpit.support.quarkus.it;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.support.security.usercontext.GroupHierarchyResolver;
import io.vanillabp.cockpit.support.security.usercontext.GroupResolver;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The group hierarchy of the application: a loan manager is a clerk as well.
 */
@ApplicationScoped
public class GroupHierarchy implements GroupResolver {

  private static final Map<String, Collection<String>> HIERARCHY = Map.of("LOAN_MANAGER", List.of("CLERK"));

  @Override
  public Collection<String> resolve(
      final Collection<String> groups) {

    return GroupHierarchyResolver.resolveGroups(List.of(HIERARCHY), groups);

  }

}
