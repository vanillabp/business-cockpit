package io.vanillabp.cockpit.users;

import java.util.List;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

/**
 * A plain holder of the details of a user, for a {@link UserDetailsProvider} which builds them
 * itself.
 */
public class UserDetailsImpl implements UserDetails {
  private String id;
  private String email;
  private String display;
  private String displayShort;
  private List<String> authorities;

  /**
   * Creates the details of one user.
   *
   * @param id The id of the user
   * @param email The e-mail address of the user
   * @param display The name of the user as it is shown
   * @param displayShort A short form of the name, for example the initials
   * @param authorities The groups and roles of the user
   */
  public UserDetailsImpl(
      String id,
      String email,
      String display,
      String displayShort,
      List<String> authorities) {
    this.id = id;
    this.email = email;
    this.display = display;
    this.displayShort = displayShort;
    this.authorities = authorities;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public String getDisplay() {
    return display;
  }

  @Override
  public String getDisplayShort() {
    return displayShort;
  }

  @Override
  public List<String> getAuthorities() {
    return authorities;
  }
};
