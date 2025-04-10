package io.vanillabp.cockpit.devshell.simulator.config;

import io.vanillabp.cockpit.devshell.simulator.usermanagement.User;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ConfigurationProperties(prefix = "dev-shell-simulator")
public class Properties {

    private List<User> users;

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
