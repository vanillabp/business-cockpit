package io.vanillabp.cockpit.devshell.simulator.config;

import io.vanillabp.cockpit.devshell.simulator.usermanagement.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ConfigurationProperties(prefix = "dev-shell-simulator")
public class Properties {

    private List<User> users;

    public User getUser(String Id) {
        if (Id == null) {
            return null;
        }
        return users
                .stream()
                .filter(user -> user.getId().equals(Id))
                .findFirst()
                .orElse(null);
    }
}
