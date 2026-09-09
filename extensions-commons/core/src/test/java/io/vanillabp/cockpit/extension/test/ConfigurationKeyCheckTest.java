package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.ConfigurationKeyCheck;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A key which looks like a setting of the Business Cockpit and is read by nobody.
 * <p>
 * What is asserted here is the message rather than the refusal: which key was meant is the part a
 * framework cannot say, and it is the reason this check exists next to the one Quarkus does.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ConfigurationKeyCheckTest {

  private static String refusalOf(
      final String... propertyNames) {

    return assertThrows(
        IllegalStateException.class,
        () -> ConfigurationKeyCheck.refuseKeysNobodyReads(List.of(propertyNames))).getMessage();

  }

  @Test
  @DisplayName("A misspelled key names the one it was nearest to, at every level")
  public void aMisspelledKeyNamesTheOneMeant() {

    final var global = refusalOf("vanillabp.cockpit.rest.base-rul");
    assertTrue(global.contains("vanillabp.cockpit.rest.base-rul"), global);
    assertTrue(global.contains("vanillabp.cockpit.rest.base-url"), global);

    final var ofAModule = refusalOf("vanillabp.workflow-modules.taxi.cockpit.ui-uri-pth");
    assertTrue(ofAModule.contains("vanillabp.workflow-modules.taxi.cockpit.ui-uri-path"), ofAModule);

    final var ofAWorkflow = refusalOf(
        "vanillabp.workflow-modules.taxi.workflows.TaxiRide.cockpit.template-paths");
    assertTrue(
        ofAWorkflow
            .contains("vanillabp.workflow-modules.taxi.workflows.TaxiRide.cockpit.template-path"),
        ofAWorkflow);

    final var ofAUserTask = refusalOf(
        "vanillabp.workflow-modules.taxi.workflows.TaxiRide.user-tasks.approve.cockpit.template-pth");
    assertTrue(
        ofAUserTask
            .contains(
                "vanillabp.workflow-modules.taxi.workflows.TaxiRide.user-tasks.approve.cockpit.template-path"),
        ofAUserTask);

  }

  @Test
  @DisplayName("A key of version 1 which is gone says what became of it")
  public void aKeyOfVersionOneSaysWhatBecameOfIt() {

    final var log = refusalOf("vanillabp.cockpit.rest.log");
    assertTrue(log.contains("version 1"), log);
    assertTrue(log.contains("BpmsApi"), log);

    final var retry = refusalOf("vanillabp.cockpit.rest.retry.max-attempts");
    assertTrue(retry.contains("vanillabp.outbox.*"), retry);

    final var bean = refusalOf(
        "vanillabp.workflow-modules.taxi.cockpit.group-hierarchy-bean-name");
    assertTrue(bean.contains("WorkflowModuleDetailsProvider"), bean);

  }

  @Test
  @DisplayName("A key nothing is near to lists what that level has")
  public void aKeyNothingIsNearToListsTheLevel() {

    final var refusal = refusalOf(
        "vanillabp.workflow-modules.taxi.workflows.TaxiRide.cockpit.workflow-module-uri");

    assertTrue(refusal.contains("a single workflow"), refusal);
    assertTrue(refusal.contains("template-path"), refusal);

  }

  @Test
  @DisplayName("The open sections, the lists and everything outside the cockpit are left alone")
  public void whatIsNotACockpitKeyIsLeftAlone() {

    assertDoesNotThrow(
        () -> ConfigurationKeyCheck
            .refuseKeysNobodyReads(
                List
                    .of(
                        "vanillabp.cockpit.kafka.properties.security.protocol",
                        "vanillabp.cockpit.rest.authentication.oauth.proxy.host",
                        "vanillabp.cockpit.process-engine-api.remembered-user-tasks",
                        "vanillabp.workflow-modules.taxi.cockpit.group-hierarchy.TEAM_LEAD[0]",
                        "vanillabp.workflow-modules.taxi.cockpit.i18n-languages[1]",
                        "vanillabp.workflow-modules.taxi.adapters.c7.resources-location",
                        "vanillabp.adapters.c7.type",
                        "quarkus.datasource.db-kind",
                        "spring.datasource.url")));

  }

  @Test
  @DisplayName("A key which holds for one profile is the same key")
  public void aProfiledKeyIsTheSameKey() {

    final var refusal = refusalOf("%prod.vanillabp.cockpit.rest.base-rul");

    assertTrue(refusal.contains("rest.base-url"), refusal);

    assertDoesNotThrow(
        () -> ConfigurationKeyCheck
            .refuseKeysNobodyReads(List.of("%dev.vanillabp.cockpit.rest.base-url")));

  }

  @Test
  @DisplayName("Every key of a boot is reported at once, not the first one only")
  public void everyKeyIsReportedAtOnce() {

    final var refusal = refusalOf(
        "vanillabp.cockpit.rest.base-rul", "vanillabp.cockpit.template-loader-pth");

    assertTrue(refusal.contains("base-rul"), refusal);
    assertTrue(refusal.contains("template-loader-pth"), refusal);

  }

}
