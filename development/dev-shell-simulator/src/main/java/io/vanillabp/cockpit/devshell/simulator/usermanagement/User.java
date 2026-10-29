package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * A made-up user of the dev shell simulator, as listed in the configuration.
 */
@Data
@Builder
public class User {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> groups;
    private Map<String, List<String>> attributes = null;

    /**
     * Creates a user. Use {@link #builder()} instead.
     *
     * @param id The id the user logs in with
     * @param email The email address
     * @param firstName The first name
     * @param lastName The last name
     * @param groups The groups, which become the user's authorities
     * @param attributes More attributes, each with a list of values
     */
    User(
            final String id,
            final String email,
            final String firstName,
            final String lastName,
            final List<String> groups,
            final Map<String, List<String>> attributes) {

        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.groups = groups;
        this.attributes = attributes;

    }

}
