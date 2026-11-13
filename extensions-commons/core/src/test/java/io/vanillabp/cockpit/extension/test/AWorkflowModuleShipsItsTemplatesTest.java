package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.templating.EventTitles;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module keeps all of its resources in a directory of its own name, and its templates in
 * <code>&lt;module id&gt;/business-cockpit-templates</code> of the classpath. Found there, they are
 * rendered with nothing configured. A module may name a directory of its own instead, and the
 * directory of the application still works for every module which names none.
 * <p>
 * Each kind of template is tested: the title of a workflow, of a user task and of a kind of user
 * task, and the two texts a search matches against. The ones which belong to no single task lie
 * right in the module's directory.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AWorkflowModuleShipsItsTemplatesTest {

  private static final String LOAN_APPROVAL = "loan-approval";

  private static final Map<String, Object> LOAN = Map.of("loan", Map.of("number", "L-1"));

  private static ConfigurationFixture theLoanApprovalModule() {

    return ConfigurationFixture
        .aConfiguredApplication()
        .withWorkflowModuleId(LOAN_APPROVAL)
        // with templates the module does not need to say the language of its BPMN names
        .withoutWorkflowModule(ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE)
        .withWorkflowModule(ConfigurationKeys.I18N_LANGUAGES, "en");

  }

  private static BusinessCockpitConfiguration read(
      final ConfigurationFixture fixture) {

    return BusinessCockpitConfiguration
        .readAndValidate(fixture.properties(), fixture.settings(), true, false);

  }

  private static UserTaskEvent userTaskOf(
      final BusinessCockpitConfiguration configuration,
      final String taskDefinition) {

    final var event = new UserTaskEvent(UserTaskEventKind.CREATED);
    event.setWorkflowModuleId(LOAN_APPROVAL);
    event.setBpmnProcessId("LoanApproval");
    event.setTaskDefinition(taskDefinition);
    event.setTemplateContext(LOAN);
    EventTitles
        .fill(
            event, configuration.workflowModule(LOAN_APPROVAL),
            BusinessCockpitAssembly.templatingOf(configuration), "BPMN task", "BPMN process");
    return event;

  }

  private static WorkflowEvent workflowOf(
      final BusinessCockpitConfiguration configuration) {

    final var event = new WorkflowEvent(WorkflowEventKind.CREATED);
    event.setWorkflowModuleId(LOAN_APPROVAL);
    event.setBpmnProcessId("LoanApproval");
    event.setTemplateContext(LOAN);
    EventTitles
        .fill(
            event, configuration.workflowModule(LOAN_APPROVAL),
            BusinessCockpitAssembly.templatingOf(configuration), "BPMN process");
    return event;

  }

  @Test
  @DisplayName("Without any key the templates of a user task come from the module's default directory")
  public void theTemplatesOfAUserTaskComeFromTheDefaultDirectory() {

    final var configuration = read(theLoanApprovalModule());

    final var directory = configuration.workflowModule(LOAN_APPROVAL).templateDirectory();
    assertEquals("classpath:", directory.loaderPath());
    assertEquals("loan-approval/business-cockpit-templates", directory.below());

    final var task = userTaskOf(configuration, "assessRisk");
    assertEquals("Assess the risk of loan L-1", task.getTitle().get("en"), "the task's own template");
    assertEquals("Assessment", task.getTaskDefinitionTitle().get("en"));
    assertEquals("Loan L-1", task.getWorkflowTitle().get("en"));
    assertEquals("Assess the risk of loan L-1 | Loan L-1 | Assessment", task.getDetailsFulltextSearch());

  }

  @Test
  @DisplayName("A task without a template of its own takes the one in the module's directory")
  public void aTaskWithoutATemplateOfItsOwnTakesTheModulesOne() {

    final var task = userTaskOf(read(theLoanApprovalModule()), "decide");

    assertEquals("Loan task L-1", task.getTitle().get("en"));

  }

  @Test
  @DisplayName("Without any key the templates of a workflow come from the module's default directory")
  public void theTemplatesOfAWorkflowComeFromTheDefaultDirectory() {

    final var workflow = workflowOf(read(theLoanApprovalModule()));

    assertEquals("Loan L-1", workflow.getTitle().get("en"));
    assertEquals("Loan L-1 | searchable", workflow.getDetailsFulltextSearch());

  }

  @Test
  @DisplayName("A module's own template-loader-path wins over the application's and the default")
  public void theModulesOwnDirectoryWins() {

    final var configuration = read(
        theLoanApprovalModule()
            .with(ConfigurationKeys.TEMPLATE_LOADER_PATH, "classpath:global-templates")
            .withWorkflowModule(ConfigurationKeys.TEMPLATE_LOADER_PATH, "classpath:elsewhere/loan-templates"));

    assertEquals("Loan L-1 from elsewhere", workflowOf(configuration).getTitle().get("en"));

  }

  @Test
  @DisplayName("The application's template-loader-path still works for a module which names none")
  public void theApplicationsDirectoryStillWorks() {

    final var configuration = read(
        theLoanApprovalModule().with(ConfigurationKeys.TEMPLATE_LOADER_PATH, "classpath:global-templates"));

    final var directory = configuration.workflowModule(LOAN_APPROVAL).templateDirectory();
    assertEquals("classpath:global-templates", directory.loaderPath());
    assertEquals(LOAN_APPROVAL, directory.below());
    assertEquals("Loan L-1 from the application", workflowOf(configuration).getTitle().get("en"));

  }

  @Test
  @DisplayName("A module without templates is no defect, it only has to say the language of its BPMN")
  public void aModuleWithoutTemplatesReportsTheBpmnNames() {

    final var configuration = read(ConfigurationFixture.aConfiguredApplication());

    assertNull(configuration.workflowModule(ConfigurationFixture.WORKFLOW_MODULE).templateDirectory());
    assertFalse(configuration.isTemplating());

    final var defects = assertThrows(
        IllegalStateException.class,
        () -> read(ConfigurationFixture.aConfiguredApplication()
            .withoutWorkflowModule(ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE)))
        .getMessage();
    assertTrue(defects.contains("'test-module/business-cockpit-templates'"), defects);
    assertTrue(defects.contains("vanillabp.workflow-modules.test-module.cockpit.template-loader-path"), defects);

  }

  @Test
  @DisplayName("A module's template-loader-path without its place is refused, naming the module's key")
  public void aModulesDirectoryWithoutItsPlaceIsRefused() {

    final var defects = assertThrows(
        IllegalStateException.class,
        () -> read(theLoanApprovalModule()
            .withWorkflowModule(ConfigurationKeys.TEMPLATE_LOADER_PATH, "loan-templates")))
        .getMessage();

    assertTrue(defects.contains("vanillabp.workflow-modules.loan-approval.cockpit.template-loader-path"), defects);
    assertTrue(defects.contains("'classpath:loan-templates'"), defects);

  }

  @Test
  @DisplayName("A module's template-loader-path without a template engine is refused")
  public void aModulesDirectoryWithoutAnEngineIsRefused() {

    final var fixture = theLoanApprovalModule()
        .withWorkflowModule(ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE, "en")
        .withWorkflowModule(ConfigurationKeys.TEMPLATE_LOADER_PATH, "classpath:elsewhere/loan-templates");
    final var defects = assertThrows(
        IllegalStateException.class,
        () -> BusinessCockpitConfiguration.readAndValidate(fixture.properties(), fixture.settings(), false, false))
        .getMessage();

    assertTrue(defects.contains("vanillabp.workflow-modules.loan-approval.cockpit.template-loader-path"), defects);
    assertTrue(defects.contains("org.freemarker:freemarker"), defects);

  }

  @Test
  @DisplayName("A module's directory of the file system which cannot be read stops the start")
  public void aModulesFileDirectoryWhichCannotBeReadStopsTheStart() {

    final var configuration = read(
        theLoanApprovalModule()
            .withWorkflowModule(ConfigurationKeys.TEMPLATE_LOADER_PATH, "file:/no/such/directory"));

    final var failure = assertThrows(
        IllegalStateException.class, () -> BusinessCockpitAssembly.templatingOf(configuration));
    assertTrue(failure.getMessage().contains("vanillabp.workflow-modules.loan-approval.cockpit.template-loader-path"),
        failure.getMessage());

  }

}
