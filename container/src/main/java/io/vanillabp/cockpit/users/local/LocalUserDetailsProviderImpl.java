package io.vanillabp.cockpit.users.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.UserDetailsImpl;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import jakarta.annotation.PostConstruct;

/**
 * The user directory of the profile {@code local}. It loads the demo users once, at startup, from
 * the simulator of the development tools, and it answers every later question from that copy. A
 * user added to the simulator later is seen only after a restart.
 * <p>
 * If the simulator cannot be reached, the cockpit does not start, and the message says which
 * property points to it.
 */
public class LocalUserDetailsProviderImpl implements UserDetailsProvider {

  private static final Logger log = LoggerFactory.getLogger(LocalUserDetailsProviderImpl.class);

  private final List<UserDetails> users = new LinkedList<>();

  private final RestTemplate restTemplate;

  private final String devShellSimulatorUsersUri;

  /**
   * Creates the directory. The users are loaded later, by
   * {@link #loadAllUsersFromDevShellSimulator()}.
   *
   * @param devShellSimulatorUsersUri The address the simulator serves its users at. The users
   *          are read from <code>/all</code> below it
   */
  public LocalUserDetailsProviderImpl(
      final String devShellSimulatorUsersUri) {

    this.devShellSimulatorUsersUri = devShellSimulatorUsersUri;
    this.restTemplate = new RestTemplateBuilder()
        .baseUri(devShellSimulatorUsersUri)
        .build();

  }

  /**
   * One user the way the simulator sends it as JSON.
   */
  public static class UserRepresentation {

    /**
     * Called by the JSON reader, which creates one object per user it reads.
     */
    public UserRepresentation() {
    }

    /** The id of the user, the one tasks and workflows name. */
    public String id;
    /** The email address of the user. */
    public String email;
    /** The first name, shortened to its first letter in the short display name. */
    public String firstName;
    /** The last name. Where it is empty, the user is shown by the id. */
    public String lastName;
    /** The ids of the groups the user belongs to. */
    public List<String> groups;
    /** Further attributes of the user. The cockpit does not read them. */
    public Map<String, List<String>> attributes = null;
  }

  /**
   * Loads all users from the simulator. Spring calls it once, after it created this bean.
   *
   * @throws RuntimeException If the simulator cannot be reached or sends no users. The message
   *          names the address used and the property which changes it
   */
  @PostConstruct
  public void loadAllUsersFromDevShellSimulator() {

    try {
      log.info("Loading users from {}", devShellSimulatorUsersUri);
      Arrays
          .stream(
              Optional.ofNullable(
                  this.restTemplate.getForEntity("/all", UserRepresentation[].class).getBody())
                  .orElseThrow(() -> new RuntimeException("Got null-response when fetching users")))
          .map(this::toUser)
          .forEach(users::add);
    } catch (Exception e) {
      throw new RuntimeException(
          ("Unable to load users from Simulator. Is it up an running at %s?\n"
              +
              "If URI is another than this, use Spring Boot property '%s' to change. One can also point to the "
              +
              "DevShell-Simulator instead. For details see: %s").formatted(
                  devShellSimulatorUsersUri,
                  "dev-shell-simulator.users-uri",
                  "https://github.com/vanillabp/business-cockpit/tree/main/development/dev-shell-simulator"), e);
    }

  }

  private UserDetailsImpl toUser(
      UserRepresentation user) {

    if (user == null) {
      return null;
    }

    final var display = !StringUtils.hasText(user.lastName)
        ? user.id
        : !StringUtils.hasText(user.firstName)
            ? user.lastName
            : user.lastName
                + ", "
                + user.firstName;

    final var displayShort = !StringUtils.hasText(user.lastName)
        ? user.id
        : !StringUtils.hasText(user.firstName)
            ? user.lastName
            : user.lastName
                + ", "
                + user.firstName.charAt(0)
                + ".";

    return new UserDetailsImpl(
        user.id, user.email, display, displayShort, user.groups != null ? user.groups
            : new ArrayList<>()
    );

  }

  @Override
  public Optional<UserDetails> getUser(
      final String id) {

    return users
        .stream()
        .filter(user -> user.getId().equals(id))
        .findFirst();

  }

  @Override
  public List<UserDetails> findUsers(
      String query) {

    return users
        .stream()
        .filter(user -> {
          if (user.getDisplay().toLowerCase().contains(query)) return true;
          if (user.getEmail().toLowerCase().contains(query)) return true;
          return false;
        })
        .toList();

  }

  @Override
  public List<UserDetails> findUsers(
      String query,
      List<String> excludeUsersIds) {

    return users
        .stream()
        .filter(user -> !excludeUsersIds.contains(user.getId()))
        .filter(user -> {
          if (user.getDisplay().toLowerCase().contains(query)) return true;
          if (user.getEmail().toLowerCase().contains(query)) return true;
          return false;
        })
        .toList();

  }

  @Override
  public List<UserDetails> getAllUsers() {

    return users;

  }

  @Override
  public List<UserDetails> getAllUsers(
      List<String> excludeUsersIds) {

    return users
        .stream()
        .filter(user -> !excludeUsersIds.contains(user.getId()))
        .toList();

  }

}
