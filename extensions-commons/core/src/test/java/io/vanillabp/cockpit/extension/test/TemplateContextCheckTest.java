package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.templating.TemplateContextCheck;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module checks in a test that its template contexts have every value its templates
 * read. The module {@value #LOAN_APPROVAL} keeps its templates in the default directory of the test
 * classpath, and every one of them reads <code>loan.number</code>.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TemplateContextCheckTest {

  private static final String LOAN_APPROVAL = "loan-approval";

  /** The sentence each message about a missing value has. */
  private static final String DOES_NOT_HAVE = "which its template context does not have";

  private static final Map<String, Object> A_COMPLETE_LOAN = Map.of("loan", Map.of("number", "L-1"));

  /** A context in which the loan is there, but not its number. */
  private static final Map<String, Object> A_LOAN_WITHOUT_ITS_NUMBER = Map.of("loan", Map.of("amount", 1000));

  @Test
  @DisplayName("A template context with every value misses nothing")
  public void aCompleteTemplateContextMissesNothing() {

    final var check = TemplateContextCheck.ofWorkflowModule(LOAN_APPROVAL, "en", "de");

    final var missingOfTheTask = check.missingValuesOfUserTask("LoanApproval", "assessRisk", A_COMPLETE_LOAN);
    final var missingOfTheWorkflow = check.missingValuesOfWorkflow("LoanApproval", A_COMPLETE_LOAN);

    assertEquals(List.of(), missingOfTheTask);
    assertEquals(List.of(), missingOfTheWorkflow);
    assertTrue(missingOfTheTask.stream().noneMatch(message -> message.contains(DOES_NOT_HAVE)));
    assertDoesNotThrow(() -> check.assertUserTask("LoanApproval", "assessRisk", A_COMPLETE_LOAN));
    assertDoesNotThrow(() -> check.assertWorkflow("LoanApproval", A_COMPLETE_LOAN));

  }

  @Test
  @DisplayName("A user task renders the title of its workflow with its own template context")
  public void aUserTaskNeedsTheValuesOfTheWorkflowTitle() {

    final var missing = TemplateContextCheck
        .ofWorkflowModule(LOAN_APPROVAL, "en", "de")
        .missingValuesOfUserTask("LoanApproval", "assessRisk", A_LOAN_WITHOUT_ITS_NUMBER);

    final var task = "the user task 'assessRisk' of the workflow 'LoanApproval' in the workflow module 'loan-approval'";
    assertTrue(
        missing
            .contains(
                "The template 'classpath:loan-approval/business-cockpit-templates/workflow-title.ftl' for "
                    + task
                    + " reads 'loan.number', "
                    + DOES_NOT_HAVE
                    + ". Put it into the template context the details provider sets with setTemplateContext, "
                    + "or write ${loan.number!} in the template where the value may be missing."),
        String.join("\n", missing));
    assertTrue(
        missing
            .stream()
            .anyMatch(message -> message
                .startsWith(
                    "The template 'classpath:loan-approval/business-cockpit-templates/LoanApproval/assessRisk/task-title.ftl' for "
                        + task)),
        String.join("\n", missing));
    // rendered once per language, reported once
    assertEquals(missing.stream().distinct().count(), missing.size());

  }

  @Test
  @DisplayName("The assertion names every template which misses a value")
  public void theAssertionNamesEveryTemplate() {

    final var check = TemplateContextCheck.ofWorkflowModule(LOAN_APPROVAL, "en");

    final var failure = assertThrows(
        AssertionError.class,
        () -> check.assertUserTask("LoanApproval", "assessRisk", A_LOAN_WITHOUT_ITS_NUMBER));

    assertTrue(failure.getMessage().contains("workflow-title.ftl"), failure.getMessage());
    assertTrue(failure.getMessage().contains("assessRisk/task-title.ftl"), failure.getMessage());

  }

  @Test
  @DisplayName("The templates of a workflow are checked with the template context of the workflow")
  public void theTemplatesOfAWorkflowAreChecked() {

    final var check = TemplateContextCheck.ofWorkflowModule(LOAN_APPROVAL, "en");

    final var failure = assertThrows(
        AssertionError.class,
        () -> check.assertWorkflow("LoanApproval", A_LOAN_WITHOUT_ITS_NUMBER));

    assertTrue(
        failure
            .getMessage()
            .contains(
                "'classpath:loan-approval/business-cockpit-templates/workflow-title.ftl' for the workflow "
                    + "'LoanApproval' in the workflow module 'loan-approval' reads 'loan.number'"),
        failure.getMessage());

  }

  @Test
  @DisplayName("A directory of its own and renamed segments are looked up like the cockpit adapter does")
  public void aDirectoryOfItsOwnAndRenamedSegments() {

    // the templates of 'test-module' lie in TestProcess/approve, and the module calls the workflow
    // 'Orders' and its task 'check'
    final var check = TemplateContextCheck
        .ofWorkflowModule("test-module", "en")
        .withTemplateLoaderPath("classpath:templates/test-module")
        .withTemplatePathOfWorkflow("Orders", "TestProcess")
        .withTemplatePathOfUserTask("Orders", "check", "approve");

    final var missing = check.missingValuesOfUserTask("Orders", "check", Map.of("order", Map.of("amount", 250)));

    assertTrue(
        missing
            .stream()
            .anyMatch(message -> message
                .startsWith(
                    "The template 'classpath:templates/test-module/TestProcess/approve/task-title.ftl' for the "
                        + "user task 'check' of the workflow 'Orders'")),
        String.join("\n", missing));

  }

  @Test
  @DisplayName("A template which fails for another reason is named as well")
  public void aTemplateWhichFailsOtherwiseIsNamed() {

    // the title of the module 'broken-titles' divides by the count, and the count is zero
    final var missing = TemplateContextCheck
        .ofWorkflowModule("broken-titles", "en")
        .missingValuesOfWorkflow("LoanApproval", Map.of("loan", Map.of("count", 0)));

    assertTrue(
        missing
            .stream()
            .anyMatch(message -> message
                .startsWith(
                    "The template 'classpath:broken-titles/business-cockpit-templates/workflow-title.ftl' for the "
                        + "workflow 'LoanApproval' in the workflow module 'broken-titles' cannot be rendered: ")),
        String.join("\n", missing));

  }

  @Test
  @DisplayName("The check needs the languages and a directory which says where it is")
  public void theCheckNeedsLanguagesAndAPlace() {

    final var withoutLanguages = assertThrows(
        IllegalArgumentException.class,
        () -> TemplateContextCheck.ofWorkflowModule(LOAN_APPROVAL));
    assertTrue(withoutLanguages.getMessage().contains("i18n-languages"), withoutLanguages.getMessage());

    final var withoutPlace = assertThrows(
        IllegalArgumentException.class,
        () -> TemplateContextCheck.ofWorkflowModule(LOAN_APPROVAL, "en").withTemplateLoaderPath("templates"));
    assertTrue(withoutPlace.getMessage().contains("'classpath:' or 'file:'"), withoutPlace.getMessage());

  }

}
