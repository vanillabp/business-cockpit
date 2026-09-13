package io.vanillabp.cockpit.users.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.integration.test.utils.FreePortUtil;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The users of the Spring profile 'local' come from a running DevShell-Simulator, so this test
 * serves them from a local HTTP server and asks the provider what it found.
 * <p>
 * The path the provider requests is asserted as well. The base URI is configured once and every
 * call is written relative to it, which only works as long as the two are joined the way the
 * configured URI reads.
 */
@ExtendWith(SuppressOutputExtension.class)
class LocalUserDetailsProviderImplTest {

    private static final String USERS = """
            [
              {
                "id": "willi",
                "email": "Willi.Wichtig@vanillabp.io",
                "firstName": "Willi",
                "lastName": "Wichtig",
                "groups": [ "TREASURER" ]
              },
              {
                "id": "testuser",
                "email": "test.user@vanillabp.io",
                "firstName": "Test",
                "lastName": "User"
              },
              {
                "id": "nameless",
                "email": "nameless@vanillabp.io"
              }
            ]
            """;

    private HttpServer simulator;

    private String usersUri;

    private final AtomicReference<String> requestedPath = new AtomicReference<>();

    @BeforeEach
    void startSimulator() throws IOException {

        final var port = FreePortUtil.getFreePort();
        simulator = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        simulator.createContext("/dev-shell/user/all", this::serveUsers);
        simulator.start();
        usersUri = "http://localhost:%d/dev-shell/user".formatted(port);

    }

    @AfterEach
    void stopSimulator() {

        simulator.stop(0);

    }

    private void serveUsers(
            final HttpExchange exchange) throws IOException {

        requestedPath.set(exchange.getRequestURI().getPath());
        final var body = USERS.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var response = exchange.getResponseBody()) {
            response.write(body);
        }

    }

    private LocalUserDetailsProviderImpl loadedProvider() {

        final var provider = new LocalUserDetailsProviderImpl(usersUri);
        provider.loadAllUsersFromDevShellSimulator();
        return provider;

    }

    private static List<String> idsOf(
            final List<UserDetails> users) {

        return users
                .stream()
                .map(UserDetails::getId)
                .toList();

    }

    @Test
    void theUsersAreFetchedBelowTheConfiguredUri() {

        final var users = loadedProvider().getAllUsers();

        assertEquals("/dev-shell/user/all", requestedPath.get());
        assertEquals(List.of("willi", "testuser", "nameless"), idsOf(users));

    }

    @Test
    void aUserIsFoundById() {

        final var user = loadedProvider().getUser("testuser");

        assertTrue(user.isPresent());
        assertEquals("test.user@vanillabp.io", user.get().getEmail());
        assertEquals("User, Test", user.get().getDisplay());
        assertEquals("User, T.", user.get().getDisplayShort());
        assertEquals(List.of(), user.get().getAuthorities());

    }

    @Test
    void anUnknownIdYieldsNothing() {

        assertTrue(loadedProvider().getUser("nobody").isEmpty());

    }

    @Test
    void aUserWithoutANameIsDisplayedByItsId() {

        final var user = loadedProvider().getUser("nameless");

        assertTrue(user.isPresent());
        assertEquals("nameless", user.get().getDisplay());
        assertEquals("nameless", user.get().getDisplayShort());

    }

    @Test
    void theGroupsOfTheSimulatorBecomeAuthorities() {

        final var user = loadedProvider().getUser("willi");

        assertTrue(user.isPresent());
        assertEquals(List.of("TREASURER"), user.get().getAuthorities());

    }

    @Test
    void theQueryMatchesDisplayAndEmail() {

        final var provider = loadedProvider();

        assertEquals(List.of("willi"), idsOf(provider.findUsers("wichtig")));
        assertEquals(List.of("testuser"), idsOf(provider.findUsers("test.user")));
        assertEquals(List.of(), idsOf(provider.findUsers("nobody")));

    }

    @Test
    void excludedUsersAreLeftOutOfBothLists() {

        final var provider = loadedProvider();
        final var excluded = List.of("willi");

        assertEquals(List.of("testuser", "nameless"), idsOf(provider.getAllUsers(excluded)));
        assertEquals(List.of(), idsOf(provider.findUsers("wichtig", excluded)));
        assertEquals(List.of("testuser"), idsOf(provider.findUsers("test.user", excluded)));

    }

    @Test
    void aSimulatorWhichIsDownIsReportedWithTheProperty() {

        simulator.stop(0);

        final var failure = assertThrows(
                RuntimeException.class,
                () -> new LocalUserDetailsProviderImpl(usersUri).loadAllUsersFromDevShellSimulator());

        assertTrue(failure.getMessage().contains("dev-shell-simulator.users-uri"));
        assertTrue(failure.getMessage().contains(usersUri));

    }

    @Test
    void anEmptySimulatorLeavesNoUsers() throws IOException {

        simulator.removeContext("/dev-shell/user/all");
        simulator.createContext("/dev-shell/user/all", exchange -> {
            final var body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var response = exchange.getResponseBody()) {
                response.write(body);
            }
        });

        final var provider = loadedProvider();

        assertTrue(provider.getAllUsers().isEmpty());
        assertFalse(provider.getUser("willi").isPresent());

    }

}
