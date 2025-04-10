package io.vanillabp.cockpit.users.local;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.UserDetailsImpl;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

public class LocalUserDetailsProviderImpl implements UserDetailsProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalUserDetailsProviderImpl.class);

    private final List<UserDetails> users = new LinkedList<>();

    private final RestTemplate restTemplate;

    private final String devShellSimulatorUsersUri;

    public LocalUserDetailsProviderImpl(
            final String devShellSimulatorUsersUri) {

        this.devShellSimulatorUsersUri = devShellSimulatorUsersUri;
        this.restTemplate = new RestTemplateBuilder()
                .rootUri(devShellSimulatorUsersUri)
                .build();

    }

    public static class UserRepresentation {
        public String id;
        public String email;
        public String firstName;
        public String lastName;
        public List<String> groups;
        public Map<String, List<String>> attributes = null;
    }

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
                    ("Unable to load users from Simulator. Is it up an running at %s?\n" +
                    "If URI is another than this, use Spring Boot property '%s' to change. One can also point to the " +
                    "DevShell-Simulator instead. For details see: %s").formatted(
                            devShellSimulatorUsersUri,
                            "dev-shell-simulator.users-uri",
                            "https://github.com/vanillabp/business-cockpit/tree/main/development/dev-shell-simulator"),
                    e);
        }

    }

    private UserDetailsImpl toUser(
            UserRepresentation user) {
        
        if (user == null) {
            return null;
        }

        // Format user details to display
        final var display = !StringUtils.hasText(user.lastName)
                ? user.id
                : !StringUtils.hasText(user.firstName)
                ? user.lastName
                : user.lastName + ", " + user.firstName;

        // Format user details to displayShort
        final var displayShort = !StringUtils.hasText(user.lastName)
                ? user.id
                : !StringUtils.hasText(user.firstName)
                ? user.lastName
                : user.lastName + ", " + user.firstName.charAt(0) + ".";

        // Create and return the user details implementation
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
    public List<UserDetails> findUsers(String query) {

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
    public List<UserDetails> findUsers(String query, List<String> excludeUsersIds) {

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
    public List<UserDetails> getAllUsers(List<String> excludeUsersIds) {

        return users
                .stream()
                .filter(user -> !excludeUsersIds.contains(user.getId()))
                .toList();

    }

}
