package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.vanillabp.cockpit.devshell.simulator.api.client.DevShellUserApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The dev shell lists the made-up users and switches between them through
 * {@code /dev-shell/user}. The test calls these paths through the client generated from the
 * simulator's OpenAPI file, so it proves that the simulator answers what the file describes. The
 * client keeps the cookies, like a browser does, so a switch of the user holds for the next call.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:the-dev-shell-switches-users-as-the-file-describes", "dev-shell-simulator.users[0].id=john", "dev-shell-simulator.users[0].email=john@doe.com", "dev-shell-simulator.users[0].first-name=John", "dev-shell-simulator.users[0].last-name=Doe", "dev-shell-simulator.users[0].groups=ADMIN, RISK_ASSESSMENT", "dev-shell-simulator.users[0].attributes.office365-uuid=7bda0fe7", "dev-shell-simulator.users[1].id=jane", "dev-shell-simulator.users[1].groups=ADMIN"
})
class TheDevShellSwitchesUsersAsTheFileDescribesTest {

  @Value("${local.server.port}")
  private int port;

  private DevShellUserApi users;

  @BeforeEach
  void createTheClient() {

    final var browser = HttpClient
        .newBuilder()
        .cookieHandler(new CookieManager())
        .build();
    final var restClient = RestClient
        .builder()
        .baseUrl("http://localhost:"
            + port)
        .requestFactory(new JdkClientHttpRequestFactory(browser))
        .build();
    users = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(DevShellUserApi.class);

  }

  @Test
  void theShellListsAllUsers() {

    assertThat(users.allUsersIds().getBody()).containsExactly("john", "jane");

    final var all = users.allUsers().getBody();
    assertThat(all).hasSize(2);
    final var john = all.get(0);
    assertThat(john.getId()).isEqualTo("john");
    assertThat(john.getEmail()).isEqualTo("john@doe.com");
    assertThat(john.getFirstName()).isEqualTo("John");
    assertThat(john.getLastName()).isEqualTo("Doe");
    assertThat(john.getGroups()).containsExactly("ADMIN", "RISK_ASSESSMENT");
    assertThat(john.getAttributes()).isEqualTo(Map.of("office365-uuid", List.of("7bda0fe7")));

  }

  @Test
  void anApplicationReadsOneUserById() {

    assertThat(users.getUserById("jane").getBody().getGroups()).containsExactly("ADMIN");

    assertThatThrownBy(() -> users.getUserById("nobody"))
        .isInstanceOfSatisfying(HttpClientErrorException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

  }

  @Test
  void withoutASwitchNobodyIsSignedIn() {

    assertThatThrownBy(() -> users.getUser())
        .isInstanceOfSatisfying(HttpClientErrorException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    assertThatThrownBy(() -> users.getUserDetails())
        .isInstanceOfSatisfying(HttpClientErrorException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

  }

  @Test
  void aSwitchHoldsUntilTheUserSignsOut() {

    assertThat(users.changeUser("jane").getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(users.getUser().getBody()).isEqualTo("jane");
    assertThat(users.getUserDetails().getBody().getId()).isEqualTo("jane");

    assertThat(users.changeUser("john").getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(users.getUser().getBody()).isEqualTo("john");

    assertThat(users.changeUser("---").getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThatThrownBy(() -> users.getUser())
        .isInstanceOfSatisfying(HttpClientErrorException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

  }

  @Test
  void aSwitchToAnUnknownUserIsRefused() {

    assertThatThrownBy(() -> users.changeUser("nobody"))
        .isInstanceOfSatisfying(HttpClientErrorException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    assertThatThrownBy(() -> users.getUser())
        .isInstanceOfSatisfying(HttpClientErrorException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

  }

}
