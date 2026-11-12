package io.vanillabp.cockpit.workflowmodule.onsupportonly;

import java.util.List;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;

/**
 * A user of the workflow module, written by the workflow module itself. Most workflow modules use
 * {@link io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl} instead. This class
 * shows that implementing {@link UserDetails} needs nothing but the module {@code support}.
 */
public class Clerk implements UserDetails {

  private final String id;

  private final String name;

  private final List<String> groups;

  /**
   * Creates a clerk.
   *
   * @param id The id of the clerk
   * @param name The name of the clerk as it is shown
   * @param groups The groups of the clerk
   */
  public Clerk(
      final String id,
      final String name,
      final List<String> groups) {
    this.id = id;
    this.name = name;
    this.groups = List.copyOf(groups);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getEmail() {
    return null;
  }

  @Override
  public String getDisplay() {
    return name;
  }

  @Override
  public String getDisplayShort() {
    return name;
  }

  @Override
  public List<String> getAuthorities() {
    return groups;
  }

}
