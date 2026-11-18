package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModuleRepository;
import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.gui.api.v1.User;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * A workflow module asks the cockpit who is signed in, with the user's cookie and the generated
 * client of the official GUI API, for example to tell whether another user claimed a task. The
 * simulator answers {@code GET /official-api/gui/api/v1/app/current-user} the way the cockpit answers it:
 * the user with all groups, completed by the group hierarchies of the workflow modules, and 200
 * without a body if nobody is signed in.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:the-simulator-answers-the-user-signed-in-like-the-cockpit", "dev-shell-simulator.users[0].id=john", "dev-shell-simulator.users[0].email=john@doe.com", "dev-shell-simulator.users[0].first-name=John", "dev-shell-simulator.users[0].last-name=Doe", "dev-shell-simulator.users[0].groups=RISK_ASSESSMENT"
})
class TheSimulatorAnswersTheUserSignedInLikeTheCockpitTest {

  private static final String CURRENT_USER = "/official-api/gui/api/v1/app/current-user";

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private WorkflowModuleRepository workflowModules;

  @AfterEach
  void removeTheWorkflowModules() {

    workflowModules.deleteAll();

  }

  @Test
  void withoutALoginTheAnswerHasNoBody() throws Exception {

    final var answer = new Browser(port).get(CURRENT_USER);

    assertThat(answer.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(answer.body()).isEmpty();

  }

  @Test
  void theUserSignedInComesWithNameAndGroups() throws Exception {

    final var answer = new Browser(port).signIn("john").get(CURRENT_USER);

    assertThat(answer.statusCode()).isEqualTo(HttpStatus.OK.value());
    final var user = jsonMapper.readValue(answer.body(), User.class);
    assertThat(user.getId()).isEqualTo("john");
    assertThat(user.getEmail()).isEqualTo("john@doe.com");
    assertThat(user.getDisplay()).isEqualTo("Doe, John");
    assertThat(user.getDisplayShort()).isEqualTo("Doe, J.");
    assertThat(user.getGroups())
        .extracting(Group::getId, Group::getDisplay)
        .containsExactly(tuple("RISK_ASSESSMENT", "RISK_ASSESSMENT"));

  }

  @Test
  void theGroupHierarchiesOfTheWorkflowModulesAddGroups() throws Exception {

    final var browser = new Browser(port).signIn("john");
    final var registration = browser.send("POST", "/bpms/api/v1_1/workflow-module/loan-approval", """
        {
          "id": "loan-approval",
          "uri": "http://localhost:8080/wm/loan-approval",
          "groupHierarchy": [
            { "group": "RISK_ASSESSMENT", "targets": [ "CLERK" ] },
            { "group": "CLERK", "targets": [ "EMPLOYEE" ] }
          ]
        }
        """);
    assertThat(registration.statusCode()).isEqualTo(HttpStatus.OK.value());

    final var user = jsonMapper.readValue(browser.get(CURRENT_USER).body(), User.class);

    assertThat(user.getGroups())
        .extracting(Group::getId)
        .containsExactlyInAnyOrder("RISK_ASSESSMENT", "CLERK", "EMPLOYEE");

  }

}
