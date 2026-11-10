package io.vanillabp.cockpit.devshell.simulator.config;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.devshell.simulator.usermanagement.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The properties of the dev shell simulator, read from the prefix {@code dev-shell-simulator}.
 * They list the made-up users the developer can switch between.
 */
@AllArgsConstructor
@Data
@ConfigurationProperties(prefix = "dev-shell-simulator")
public class Properties implements InitializingBean {

  /**
   * What the simulator says when it starts without users. It names the key and shows a user,
   * so the developer can fix the configuration from the log alone.
   */
  static final String NO_USERS_MESSAGE = """
      The dev shell simulator has no users, so nobody can log in to the dev shell.
      Add at least one user under the property key 'dev-shell-simulator.users', for example
      in a file users.yaml:

        dev-shell-simulator:
          users:
            - id: john
              first-name: John
              last-name: Doe
              groups: ADMIN

      and start the simulator with '--spring.config.additional-location=./users.yaml'.""";

  private List<User> users;

  /**
   * Creates empty properties. Spring Boot fills them by the setters.
   */
  public Properties() {
  }

  /**
   * Stops the start when no user is configured. Without a user, the dev shell is of no use,
   * and the simulator would otherwise fail later with a message which does not say why.
   */
  @Override
  public void afterPropertiesSet() {

    if ((users == null) || users.isEmpty()) {
      throw new IllegalStateException(NO_USERS_MESSAGE);
    }

  }

  /**
   * Finds a configured user by id.
   *
   * @param id The id of the user
   * @return The user, or {@code null} if the id is {@code null} or unknown
   */
  public User getUser(
      final String id) {

    if (id == null) {
      return null;
    }
    return users
        .stream()
        .filter(user -> user.getId().equals(id))
        .findFirst()
        .orElse(null);

  }

}
