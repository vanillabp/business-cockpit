package io.vanillabp.cockpit.extension.springboot.deployedversion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.springboot.deployedversionparts.CalledProcessInTheSameFile;
import io.vanillabp.cockpit.extension.springboot.deployedversionparts.CoveringProvidersService;
import io.vanillabp.cockpit.extension.springboot.deployedversionparts.NoProviderService;
import io.vanillabp.cockpit.extension.springboot.deployedversionparts.ProviderOfATaskTheModelLacksService;
import io.vanillabp.cockpit.extension.springboot.deployedversionparts.SecondaryProcessService;
import io.vanillabp.cockpit.extension.springboot.deployedversionparts.SecondaryProcessWithAnUncoveredTaskService;
import io.vanillabp.cockpit.extension.springboot.deployedversionparts.UncoveredUserTaskProviderService;
import io.vanillabp.cockpit.extension.springboot.deployedversionparts.UncoveredWorkflowProviderService;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The version deployed in this boot needs a details provider which covers it, wherever an
 * element of that model has providers at all. VanillaBP follows the same rule for a
 * <code>&#64;WorkflowTask</code> method, see decision 64 in the repository's DECISIONS.md.
 * <p>
 * The BPMS double deployed version 2 of the process. Each test boots the application with one
 * workflow service.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class TheDeployedVersionNeedsACoveringProviderTest {

  private static void boot(
      final String databaseName,
      final Class<?>... sources) {

    final var all = new Class<?>[sources.length + 1];
    all[0] = DeployedVersionApplication.class;
    System.arraycopy(sources, 0, all, 1, sources.length);
    new SpringApplicationBuilder(all)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1".formatted(databaseName),
            "vanillabp.cockpit.rest.base-url=http://localhost:1")
        .run()
        .close();

  }

  private static String failureOfBooting(
      final String databaseName,
      final Class<?>... sources) {

    final var failure = assertThrows(Exception.class, () -> boot(databaseName, sources));
    final var messages = new StringBuilder();
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      messages.append(cause.getMessage()).append('\n');
    }
    return messages.toString();

  }

  @Test
  @DisplayName("Providers which cover the deployed version let the start run")
  public void coveringProvidersLetTheStartRun() {

    assertDoesNotThrow(() -> boot("cockpit-deployed-covering", CoveringProvidersService.class));

  }

  @Test
  @DisplayName("A user task whose providers miss the deployed version ends the start")
  public void aUserTaskWhoseProvidersMissTheDeployedVersionEndsTheStart() {

    final var message = failureOfBooting(
        "cockpit-deployed-user-task", UncoveredUserTaskProviderService.class);

    assertTrue(message.contains("Version '2' of BPMN process 'TestProcess'"), message);
    assertTrue(message.contains("element '%s' (also matched by '%s')"
        .formatted(DeployedVersionApplication.APPROVE_ELEMENT, DeployedVersionApplication.APPROVE)), message);
    assertTrue(message.contains("approveOfTheFirstVersion"), message);
    assertTrue(message.contains(ConfigurationKeys.EXTENSION_ID), message);
    assertTrue(message.contains("which does not cover version '2'"), message);
    assertTrue(message.contains("For @UserTaskDetailsProvider of extension '%s': Every report about such a user task "
        .formatted(ConfigurationKeys.EXTENSION_ID)), message);

  }

  @Test
  @DisplayName("A workflow whose providers miss the deployed version ends the start")
  public void aWorkflowWhoseProvidersMissTheDeployedVersionEndsTheStart() {

    final var message = failureOfBooting(
        "cockpit-deployed-workflow", UncoveredWorkflowProviderService.class);

    assertTrue(message.contains("the process itself"), message);
    assertTrue(message.contains("workflowOfTheFirstVersion"), message);
    assertTrue(message.contains("For @WorkflowDetailsProvider of extension '%s': Every report about such a workflow "
        .formatted(ConfigurationKeys.EXTENSION_ID)), message);

  }

  @Test
  @DisplayName("A process without any provider is not asked about")
  public void aProcessWithoutAProviderIsNotAsked() {

    assertDoesNotThrow(() -> boot("cockpit-deployed-no-provider", NoProviderService.class));

  }

  @Test
  @DisplayName("A provider of a version nobody holds stays a warning")
  public void aProviderOfAVersionNobodyHoldsStaysAWarning(
      final CapturedOutput output) {

    assertDoesNotThrow(() -> boot("cockpit-deployed-nobody-holds", ProviderOfATaskTheModelLacksService.class));

    final var reported = output.getAll();
    assertTrue(reported.contains("auditOfAVersionNobodyDeploys"), reported);
    assertTrue(reported.contains("the method never runs"), reported);

  }

  @Test
  @DisplayName("A called process is held against its user task providers only, in its own version")
  public void aSecondaryProcessIsNotHeldAgainstTheWorkflowProvider() {

    assertDoesNotThrow(() -> boot(
        "cockpit-deployed-secondary", SecondaryProcessService.class, CalledProcessInTheSameFile.class));

  }

  @Test
  @DisplayName("A user task of a secondary process is held against the version of that process")
  public void aUserTaskOfASecondaryProcessIsHeldAgainstItsOwnVersion() {

    final var message = failureOfBooting(
        "cockpit-deployed-secondary-task", SecondaryProcessWithAnUncoveredTaskService.class,
        CalledProcessInTheSameFile.class);

    assertTrue(message.contains("Version '1' of BPMN process '%s'"
        .formatted(DeployedVersionApplication.CALLED_PROCESS)), message);
    assertTrue(message.contains("element '%s' (also matched by '%s')"
        .formatted(DeployedVersionApplication.CHECK_ELEMENT, DeployedVersionApplication.CHECK)), message);
    assertTrue(message.contains("checkOfTheSecondVersion"), message);
    assertTrue(message.contains("which does not cover version '1'"), message);
    // the process at the top was deployed as version 2, which the provider covers, and the
    // called process gets no workflow report of its own
    assertFalse(message.contains("TestProcess"), message);
    assertFalse(message.contains("the process itself"), message);

  }

}
