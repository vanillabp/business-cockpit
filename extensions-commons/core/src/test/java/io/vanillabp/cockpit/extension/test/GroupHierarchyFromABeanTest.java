package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchy;

/**
 * Where the group hierarchy of a workflow module comes from: the key <code>group-hierarchy</code>
 * or a {@link WorkflowModuleGroupHierarchy} bean, never both.
 */
@ExtendWith(SuppressOutputExtension.class)
public class GroupHierarchyFromABeanTest {

  /** A bean whose hierarchy a test changes while the configuration is in use. */
  private static final class ChangingHierarchy implements WorkflowModuleGroupHierarchy {

    private final String workflowModuleId;

    private volatile Map<String, Collection<String>> hierarchy;

    private ChangingHierarchy(
        final String workflowModuleId,
        final Map<String, Collection<String>> hierarchy) {

      this.workflowModuleId = workflowModuleId;
      this.hierarchy = hierarchy;

    }

    @Override
    public String getWorkflowModuleId() {

      return workflowModuleId;

    }

    @Override
    public Map<String, Collection<String>> getGroupHierarchy() {

      return hierarchy;

    }

  }

  private static BusinessCockpitConfiguration read(
      final ConfigurationFixture fixture,
      final WorkflowModuleGroupHierarchy... beans) {

    return BusinessCockpitConfiguration
        .readAndValidate(fixture.properties(), fixture.settings(), false, false, Arrays.asList(beans));

  }

  private static String defectsOf(
      final ConfigurationFixture fixture,
      final WorkflowModuleGroupHierarchy... beans) {

    return assertThrows(IllegalStateException.class, () -> read(fixture, beans)).getMessage();

  }

  @Test
  @DisplayName("Without a bean the hierarchy comes from the configuration")
  public void withoutABeanTheConfigurationAnswers() {

    final var configuration = read(
        ConfigurationFixture
            .aConfiguredApplication()
            .withWorkflowModule("group-hierarchy.TEAM_LEAD", "TEAM_MEMBER"));

    assertEquals(
        Map.of("TEAM_LEAD", List.of("TEAM_MEMBER")),
        configuration.groupHierarchyOf(ConfigurationFixture.WORKFLOW_MODULE));

  }

  @Test
  @DisplayName("A bean gives the hierarchy, and it is asked again on every call")
  public void aBeanGivesTheHierarchy() {

    final var bean = new ChangingHierarchy(
        ConfigurationFixture.WORKFLOW_MODULE, Map.of("TEAM_LEAD", List.of("TEAM_MEMBER")));
    final var configuration = read(ConfigurationFixture.aConfiguredApplication(), bean);

    assertEquals(
        Map.of("TEAM_LEAD", List.of("TEAM_MEMBER")),
        configuration.groupHierarchyOf(ConfigurationFixture.WORKFLOW_MODULE));

    bean.hierarchy = Map.of("ADMIN", List.of("TEAM_LEAD"));

    assertEquals(
        Map.of("ADMIN", List.of("TEAM_LEAD")),
        configuration.groupHierarchyOf(ConfigurationFixture.WORKFLOW_MODULE));

  }

  @Test
  @DisplayName("A gap in the hierarchy of a bean is left out instead of failing the registration")
  public void aGapInTheHierarchyIsLeftOut() {

    final var withGaps = new HashMap<String, Collection<String>>();
    withGaps.put("TEAM_LEAD", Arrays.asList("TEAM_MEMBER", null));
    withGaps.put("ASSISTANT", null);
    withGaps.put(null, List.of("NOBODY"));
    final var bean = new ChangingHierarchy(ConfigurationFixture.WORKFLOW_MODULE, withGaps);
    final var configuration = read(ConfigurationFixture.aConfiguredApplication(), bean);

    assertEquals(
        Map.of("TEAM_LEAD", List.of("TEAM_MEMBER"), "ASSISTANT", List.of()),
        configuration.groupHierarchyOf(ConfigurationFixture.WORKFLOW_MODULE));

    bean.hierarchy = null;

    assertEquals(Map.of(), configuration.groupHierarchyOf(ConfigurationFixture.WORKFLOW_MODULE));

  }

  @Test
  @DisplayName("A bean and the key for one module end the start, and the message names both")
  public void aBeanAndTheKeyAreRefused() {

    final var defects = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .withWorkflowModule("group-hierarchy.TEAM_LEAD", "TEAM_MEMBER"),
        new ChangingHierarchy(ConfigurationFixture.WORKFLOW_MODULE, Map.of()));

    assertTrue(
        defects
            .contains(
                ConfigurationKeys
                    .workflowModuleKey(
                        ConfigurationFixture.WORKFLOW_MODULE, ConfigurationKeys.GROUP_HIERARCHY)),
        defects);
    assertTrue(defects.contains(ChangingHierarchy.class.getName()), defects);
    assertTrue(defects.contains("Remove the key"), defects);

  }

  @Test
  @DisplayName("A bean for a module the application does not have ends the start")
  public void aBeanForAnUnknownModuleIsRefused() {

    final var defects = defectsOf(
        ConfigurationFixture.aConfiguredApplication(),
        new ChangingHierarchy("no-such-module", Map.of()));

    assertTrue(defects.contains("'no-such-module'"), defects);
    assertTrue(defects.contains(ConfigurationFixture.WORKFLOW_MODULE), defects);
    assertTrue(defects.contains("getWorkflowModuleId()"), defects);

  }

  @Test
  @DisplayName("A bean for a module which reports nothing to the cockpit ends the start")
  public void aBeanForAModuleWhichReportsNothingIsRefused() {

    final var defects = defectsOf(
        ConfigurationFixture
            .anApplication()
            .with(ConfigurationKeys.REST_BASE_URL, "http://localhost:8080")
            .with(ConfigurationKeys.INITIATOR_SOURCE, "by-application"),
        new ChangingHierarchy(ConfigurationFixture.WORKFLOW_MODULE, Map.of()));

    assertTrue(defects.contains("reports nothing"), defects);
    assertTrue(
        defects
            .contains(
                ConfigurationKeys
                    .workflowModuleKey(
                        ConfigurationFixture.WORKFLOW_MODULE, ConfigurationKeys.WORKFLOW_MODULE_URI)),
        defects);

  }

  @Test
  @DisplayName("Two beans for one module end the start")
  public void twoBeansForOneModuleAreRefused() {

    final var defects = defectsOf(
        ConfigurationFixture.aConfiguredApplication(),
        new ChangingHierarchy(ConfigurationFixture.WORKFLOW_MODULE, Map.of()),
        new ChangingHierarchy(ConfigurationFixture.WORKFLOW_MODULE, Map.of()));

    assertTrue(defects.contains("Two beans"), defects);
    assertTrue(defects.contains("'%s'".formatted(ConfigurationFixture.WORKFLOW_MODULE)), defects);

  }

}
