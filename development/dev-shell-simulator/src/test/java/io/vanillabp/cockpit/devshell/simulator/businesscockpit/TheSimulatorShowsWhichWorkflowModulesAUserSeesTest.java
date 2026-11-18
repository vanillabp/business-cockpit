package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.vanillabp.cockpit.devshell.simulator.api.client.SimulatorApi;
import io.vanillabp.cockpit.devshell.simulator.api.client.model.VisibleWorkflowModule;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModuleRepository;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModules;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * The cockpit lists a workflow module only for the users it is accessible to, and a cockpit loads
 * the parts of a module which belong to no case, for example a button in its header, from this
 * list. The simulator answers the same list: for the user signed in at
 * {@code /official-api/gui/api/v1/workflow-module}, which a dev shell offers on its start page, and
 * for a named user at {@code /simulator-api/workflow-module?user=<id>}, which a test of a workflow
 * module reads without a login.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:the-simulator-shows-which-workflow-modules-a-user-sees", "dev-shell-simulator.users[0].id=john", "dev-shell-simulator.users[0].groups=LOAN_OFFICERS", "dev-shell-simulator.users[1].id=jane", "dev-shell-simulator.users[1].groups=CLERKS"
})
class TheSimulatorShowsWhichWorkflowModulesAUserSeesTest {

  private static final String WORKFLOW_MODULES = "/official-api/gui/api/v1/workflow-module";

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private WorkflowModuleRepository workflowModules;

  private SimulatorApi simulatorApi;

  @BeforeEach
  void registerTheWorkflowModules() throws Exception {

    simulatorApi = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(RestClient.create("http://localhost:"
            + port)))
        .build()
        .createClient(SimulatorApi.class);

    register("loan-approval", """
        {
          "id": "loan-approval",
          "uri": "http://localhost:8080/wm/loan-approval",
          "accessibleToGroups": [ "LOAN_OFFICERS" ]
        }
        """);
    register("address-change", """
        {
          "id": "address-change",
          "uri": "http://localhost:8080/wm/address-change"
        }
        """);

  }

  @AfterEach
  void removeTheWorkflowModules() {

    workflowModules.deleteAll();

  }

  private void register(
      final String workflowModuleId,
      final String registration) throws Exception {

    final var answer = new Browser(port).send("POST", "/bpms/api/v1_1/workflow-module/"
        + workflowModuleId, registration);
    assertThat(answer.statusCode()).isEqualTo(HttpStatus.OK.value());

  }

  @Test
  void aModuleWithGroupsIsListedOnlyForItsMembers() {

    assertThat(simulatorApi.getWorkflowModulesOfUser("john").getBody().getModules())
        .extracting(VisibleWorkflowModule::getId)
        .containsExactly("address-change", "loan-approval");
    assertThat(simulatorApi.getWorkflowModulesOfUser("jane").getBody().getModules())
        .extracting(VisibleWorkflowModule::getId)
        .containsExactly("address-change");

  }

  @Test
  void aModuleIsListedWithItsVersionAndThePathOfTheProxy() throws Exception {

    register("loan-approval", """
        {
          "id": "loan-approval",
          "uri": "http://localhost:8080/wm/loan-approval",
          "accessibleToGroups": [ "LOAN_OFFICERS" ]
        }
        """);

    assertThat(simulatorApi.getWorkflowModulesOfUser("john").getBody().getModules())
        .extracting(VisibleWorkflowModule::getId, VisibleWorkflowModule::getVersion, VisibleWorkflowModule::getUri)
        .containsExactly(
            tuple("address-change", 0L, "/wm/address-change"),
            tuple("loan-approval", 1L, "/wm/loan-approval"));

  }

  @Test
  void theGroupHierarchiesOfTheWorkflowModulesCount() throws Exception {

    register("risk-assessment", """
        {
          "id": "risk-assessment",
          "uri": "http://localhost:8080/wm/risk-assessment",
          "accessibleToGroups": [ "EMPLOYEES" ],
          "groupHierarchy": [
            { "group": "CLERKS", "targets": [ "EMPLOYEES" ] }
          ]
        }
        """);

    assertThat(simulatorApi.getWorkflowModulesOfUser("jane").getBody().getModules())
        .extracting(VisibleWorkflowModule::getId)
        .containsExactly("address-change", "risk-assessment");

  }

  @Test
  void anUnknownUserIsAnswered404() {

    assertThatThrownBy(() -> simulatorApi.getWorkflowModulesOfUser("nobody"))
        .isInstanceOfSatisfying(HttpClientErrorException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

  }

  @Test
  void theOfficialApiListsTheModulesOfTheUserSignedIn() throws Exception {

    final var answer = new Browser(port).signIn("jane").get(WORKFLOW_MODULES);

    assertThat(answer.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(jsonMapper.readValue(answer.body(), WorkflowModules.class).getModules())
        .extracting(WorkflowModule::getId, WorkflowModule::getVersion, WorkflowModule::getUri)
        .containsExactly(tuple("address-change", 0L, "/wm/address-change"));

  }

  @Test
  void withoutALoginTheOfficialApiListsTheModulesOpenToEverybody() throws Exception {

    final var answer = new Browser(port).get(WORKFLOW_MODULES);

    assertThat(answer.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(jsonMapper.readValue(answer.body(), WorkflowModules.class).getModules())
        .extracting(WorkflowModule::getId)
        .containsExactly("address-change");

  }

  @Test
  void aModuleTheUserDoesNotSeeIsUnknownByItsId() throws Exception {

    final var browser = new Browser(port).signIn("jane");

    assertThat(browser.get(WORKFLOW_MODULES
        + "/loan-approval").statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    final var seen = browser.get(WORKFLOW_MODULES
        + "/address-change");
    assertThat(seen.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(jsonMapper.readValue(seen.body(), WorkflowModule.class).getId()).isEqualTo("address-change");

  }

}
