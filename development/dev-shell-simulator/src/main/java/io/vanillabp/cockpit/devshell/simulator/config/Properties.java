package io.vanillabp.cockpit.devshell.simulator.config;

import java.util.List;

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
public class Properties {

  private List<User> users;

  /**
   * Creates empty properties. Spring Boot fills them by the setters.
   */
  public Properties() {
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
