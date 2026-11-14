package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.ConfigurationKeyCheck;
import io.vanillabp.cockpit.extension.spi.AdapterCockpitKey;
import io.vanillabp.cockpit.extension.spi.AdapterCockpitKeys;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A key which looks like a setting of the Business Cockpit and is read by nobody.
 * <p>
 * What is asserted here is the message and not the refusal. Which key was meant is the part a
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
    assertTrue(bean.contains("WorkflowModuleGroupHierarchy"), bean);

  }

  @Test
  @DisplayName("A key of the whole application written for a module names where it belongs")
  public void aKeyOfTheWholeApplicationNamesWhereItBelongs() {

    final var ofAModule = refusalOf(
        "vanillabp.workflow-modules.taxi.cockpit.process-engine-api.remembered-user-tasks");
    assertTrue(
        ofAModule
            .contains(
                "'vanillabp.workflow-modules.taxi.cockpit.process-engine-api.remembered-user-tasks'"),
        ofAModule);
    // this key moved below the adapter id, so the message names that place
    assertTrue(
        ofAModule.contains("'vanillabp.adapters.<adapter-id>.cockpit.remembered-user-tasks'"),
        ofAModule);

    final var ofAWorkflow = refusalOf(
        "vanillabp.workflow-modules.taxi.workflows.TaxiRide.cockpit.rest.base-url");
    assertTrue(ofAWorkflow.contains("a single workflow"), ofAWorkflow);
    assertTrue(ofAWorkflow.contains("'vanillabp.cockpit.rest.base-url'"), ofAWorkflow);

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
                        "vanillabp.cockpit.rest.baseUrl",
                        "vanillabp.cockpit.usertasksenabled",
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


  /**
   * A BPMS half which reads one key at the adapter and one at the adapter and the workflow
   * module.
   */
  private static final AdapterCockpitKeys CAMUNDA_8 = new AdapterCockpitKeys() {

    @Override
    public String adapterType() {

      return "camunda8";

    }

    @Override
    public List<AdapterCockpitKey> keys() {

      return List
          .of(
              AdapterCockpitKey
                  .atTheAdapterOnly(
                      "suspension-check-interval", "PT1M",
                      "One check runs for each cluster, so a workflow module cannot have an interval of its own."),
              new AdapterCockpitKey(
                  "page-size", Set.of(AdapterCockpitKey.Level.ADAPTER,
                      AdapterCockpitKey.Level.WORKFLOW_MODULE), "50", "A page is read for a whole workflow module."));

    }

  };

  /** The adapter ids of the tests below and their types. */
  private static final Map<String, String> TYPES = Map.of("saas", "camunda8", "old", "camunda7");

  private static String refusalAtTheAdapter(
      final String... propertyNames) {

    return assertThrows(
        IllegalStateException.class,
        () -> ConfigurationKeyCheck
            .refuseKeysNobodyReads(
                List.of(propertyNames), adapterId -> TYPES.getOrDefault(adapterId, adapterId), List.of(CAMUNDA_8)))
        .getMessage();

  }

  @Test
  @DisplayName("A key a BPMS half announced passes at the levels it is read at")
  public void anAnnouncedKeyPassesWhereItIsRead() {

    assertDoesNotThrow(
        () -> ConfigurationKeyCheck
            .refuseKeysNobodyReads(
                List
                    .of(
                        "vanillabp.adapters.saas.cockpit.suspension-check-interval",
                        "vanillabp.adapters.saas.cockpit.pageSize",
                        "vanillabp.workflow-modules.taxi.adapters.saas.cockpit.page-size"),
                TYPES::get, List.of(CAMUNDA_8)));

  }

  @Test
  @DisplayName("A key of a BPMS half below the level it is read at gets the words of the VanillaBP core")
  public void anAnnouncedKeyAtTheWrongLevelIsRefusedInTheWordsOfTheCore() {

    final var refusal = refusalAtTheAdapter(
        "vanillabp.workflow-modules.taxi.workflows.TaxiRide.adapters.saas.cockpit.suspension-check-interval",
        "vanillabp.workflow-modules.taxi.adapters.saas.cockpit.suspension-check-interval");

    assertEquals(
        """
            The Business Cockpit adapter for 'camunda8' reads 'suspension-check-interval' at the adapter, but it is configured at:
              vanillabp.workflow-modules.taxi.adapters.saas.cockpit.suspension-check-interval
              vanillabp.workflow-modules.taxi.workflows.TaxiRide.adapters.saas.cockpit.suspension-check-interval
            Move each of them to the adapter it is meant for:
              vanillabp.adapters.<adapter-id>.cockpit.suspension-check-interval: PT1M
            One check runs for each cluster, so a workflow module cannot have an interval of its own.""",
        refusal);

  }

  @Test
  @DisplayName("A key read at two levels names both keys to write instead")
  public void aKeyReadAtTwoLevelsNamesBoth() {

    final var refusal = refusalAtTheAdapter(
        "vanillabp.workflow-modules.taxi.workflows.TaxiRide.tasks.approve.adapters.saas.cockpit.page-size");

    assertTrue(refusal.contains("at the adapter and at the workflow module"), refusal);
    assertTrue(refusal.contains("one of these keys"), refusal);
    assertTrue(refusal.contains("vanillabp.adapters.<adapter-id>.cockpit.page-size: 50"), refusal);
    assertTrue(
        refusal.contains("vanillabp.workflow-modules.<workflow-module>.adapters.<adapter-id>.cockpit.page-size: 50"),
        refusal);

  }

  @Test
  @DisplayName("A neutral key at the place per adapter names the place it belongs to")
  public void aNeutralKeyAtTheAdapterNamesItsPlace() {

    final var global = refusalAtTheAdapter("vanillabp.adapters.saas.cockpit.rest.base-url");
    assertTrue(global.contains("'vanillabp.cockpit.rest.base-url'"), global);
    assertTrue(global.contains("setting for every BPMS"), global);

    final var ofAModule = refusalAtTheAdapter("vanillabp.workflow-modules.taxi.adapters.saas.cockpit.template-path");
    assertTrue(ofAModule.contains("'vanillabp.workflow-modules.taxi.cockpit.template-path'"), ofAModule);

    final var ofAModuleAtTheAdapter = refusalAtTheAdapter("vanillabp.adapters.saas.cockpit.ui-uri-path");
    assertTrue(
        ofAModuleAtTheAdapter.contains("'vanillabp.workflow-modules.<workflow-module>.cockpit.ui-uri-path'"),
        ofAModuleAtTheAdapter);

  }

  @Test
  @DisplayName("Both spellings of the task level are checked")
  public void bothSpellingsOfTheTaskLevelAreChecked() {

    final var platform = refusalAtTheAdapter(
        "vanillabp.workflow-modules.taxi.workflows.TaxiRide.tasks.approve.adapters.saas.cockpit.template-path");
    assertTrue(
        platform
            .contains("'vanillabp.workflow-modules.taxi.workflows.TaxiRide.user-tasks.approve.cockpit.template-path'"),
        platform);

    final var versionOne = refusalAtTheAdapter(
        "vanillabp.workflow-modules.taxi.workflows.TaxiRide.user-tasks.approve.adapters.saas.cockpit.suspension-check-interval");
    assertTrue(versionOne.contains("reads 'suspension-check-interval' at the adapter"), versionOne);

  }

  @Test
  @DisplayName("A key no BPMS half of the type announced is refused, with a guess where one is near")
  public void aKeyTheBpmsHalfDoesNotKnowIsRefused() {

    final var typo = refusalAtTheAdapter("vanillabp.adapters.saas.cockpit.suspension-check-intervall");
    assertTrue(typo.contains("Did you mean 'vanillabp.adapters.saas.cockpit.suspension-check-interval'?"), typo);

    final var unknown = refusalAtTheAdapter("vanillabp.adapters.saas.cockpit.something-else-entirely");
    assertTrue(unknown.contains("Those are: page-size, suspension-check-interval."), unknown);

    final var noKeysAtAll = refusalAtTheAdapter("vanillabp.adapters.old.cockpit.suspension-check-interval");
    assertTrue(noKeysAtAll.contains("adapter type 'camunda7' reads no key below an adapter id"), noKeysAtAll);

  }

  @Test
  @DisplayName("The key the Process-Engine-API half read in the neutral tree names its new place")
  public void theMovedKeyNamesItsNewPlace() {

    final var refusal = refusalOf("vanillabp.cockpit.process-engine-api.remembered-user-tasks");

    assertTrue(refusal.contains("vanillabp.cockpit.process-engine-api.remembered-user-tasks"), refusal);
    assertTrue(refusal.contains("vanillabp.adapters.<adapter-id>.cockpit.remembered-user-tasks"), refusal);

  }

  @Test
  @DisplayName("Unknown keys and misplaced keys of one boot are all reported at once")
  public void everyKindOfDefectIsReportedAtOnce() {

    final var refusal = refusalAtTheAdapter(
        "vanillabp.cockpit.rest.base-rul",
        "vanillabp.workflow-modules.taxi.adapters.saas.cockpit.suspension-check-interval");

    assertTrue(refusal.contains("base-rul"), refusal);
    assertTrue(refusal.contains("but it is configured at"), refusal);

  }

}
