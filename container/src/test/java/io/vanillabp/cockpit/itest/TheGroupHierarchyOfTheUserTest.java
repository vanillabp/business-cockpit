package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The menu of a user interface shows the user's groups and rights and where each of them comes
 * from: from the organisation, or from an entry of a workflow module's group hierarchy. The
 * cockpit merges the hierarchies of all modules for its permissions, and
 * {@code GET /app/current-user/group-hierarchy} takes the result apart again for the user signed
 * in.
 *
 * <p>All test classes share one cockpit, and other tests register hierarchies for the group of
 * Martin as well. So each test registers modules of its own and looks only at those.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheGroupHierarchyOfTheUserTest extends ItestBase {

  private static final String GROUP_HIERARCHY = "/app/current-user/group-hierarchy";

  /** Registrations need a URI, but no test here follows the proxy route to it. */
  private static final String SOME_MODULE_URI = "http://localhost:65001";

  private String cookieOfMartin;
  private String cookieOfPetra;

  @BeforeEach
  void logInBothUsers() {

    cookieOfMartin = loginToGui(USER_MARTIN);
    cookieOfPetra = loginToGui(USER_PETRA);

  }

  private void register(
      final String workflowModuleId,
      final String permissionMembers) {

    final var response = bpmsV1_1(
        "/workflow-module/"
            + workflowModuleId,
        """
            {
              "id": "%s",
              "uri": "%s",
              "taskProviderApiUriPath": "/task-provider/v1",
              "workflowProviderApiUriPath": "/workflow-provider/v1"%s
            }
            """.formatted(workflowModuleId, SOME_MODULE_URI, permissionMembers));
    assertThat(response.statusCode()).isEqualTo(200);

  }

  private void registerWithGroupHierarchy(
      final String workflowModuleId,
      final String... hierarchyEntries) {

    register(workflowModuleId,
        ",\n  \"groupHierarchy\": [ "
            + String.join(", ", hierarchyEntries)
            + " ]");

  }

  /** One entry of the {@code groupHierarchy} array. Members of {@code group} also get the targets. */
  private static String granting(
      final String group,
      final String... targets) {

    return "{ \"group\": \""
        + group
        + "\", \"targets\": [ "
        + Arrays
            .stream(targets)
            .map(target -> "\""
                + target
                + "\"")
            .collect(Collectors.joining(", "))
        + " ] }";

  }

  /** What the endpoint says about one module, or empty if it does not list the module. */
  @SuppressWarnings("unchecked")
  private Optional<List<Map<String, Object>>> entriesOfModuleFor(
      final String cookie,
      final String workflowModuleId) {

    final var response = guiGet(cookie, GROUP_HIERARCHY);
    assertThat(response.statusCode()).isEqualTo(200);
    final List<Map<String, Object>> modules = json(response).read("$.modules", List.class);
    return modules
        .stream()
        .filter(module -> workflowModuleId.equals(module.get("workflowModuleId")))
        .findFirst()
        .map(module -> (List<Map<String, Object>>) module.get("entries"));

  }

  private static Map<String, Object> entry(
      final String group,
      final boolean givenByOrganisation,
      final String... targets) {

    return Map.of(
        "group", group,
        "givenByOrganisation", givenByOrganisation,
        "targets", List.of(targets));

  }

  /**
   * The groups the user directory gives are listed on their own, also those which have no entry
   * in any hierarchy, like {@code bc-users} here. Petra's groups are not part of Martin's answer.
   */
  @Test
  void theGroupsOfTheOrganisationAreListedForTheUserSignedInOnly() {

    final var response = guiGet(cookieOfMartin, GROUP_HIERARCHY);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).read("$.givenByOrganisation", List.class))
        .containsExactly(GROUP_OF_MARTIN, "bc-users");

  }

  /**
   * Each entry says which group gives its targets and whether the organisation gives that group.
   * A group which comes from another entry is marked as not given by the organisation. Entries
   * and targets are sorted, so a user interface can show them as they come.
   */
  @Test
  void anEntryOfAGroupTheUserHasNamesItsGroupAndWhetherTheOrganisationGivesIt() {

    final var moduleId = unique("rights-module");
    final var dispatchers = unique("dispatchers");
    final var rightToDispatch = unique("right-to-dispatch");
    final var rightToDrive = unique("right-to-drive");
    registerWithGroupHierarchy(moduleId,
        granting(GROUP_OF_MARTIN, rightToDrive, dispatchers),
        granting(dispatchers, rightToDispatch));

    await().untilAsserted(() -> assertThat(entriesOfModuleFor(cookieOfMartin, moduleId))
        .contains(List.of(
            entry(GROUP_OF_MARTIN, true, dispatchers, rightToDrive),
            entry(dispatchers, false, rightToDispatch))));

  }

  /**
   * Only the user's own rights are shown. An entry of a group the user does not have is left
   * out, so Petra sees the module without Martin's entry, and Martin without Petra's.
   */
  @Test
  void anEntryOfAGroupTheUserDoesNotHaveIsLeftOut() {

    final var moduleId = unique("shared-module");
    final var rightOfAccounting = unique("right-of-accounting");
    final var rightOfSales = unique("right-of-sales");
    registerWithGroupHierarchy(moduleId,
        granting(GROUP_OF_MARTIN, rightOfAccounting),
        granting(GROUP_OF_PETRA, rightOfSales));

    await().untilAsserted(() -> {
      assertThat(entriesOfModuleFor(cookieOfMartin, moduleId))
          .contains(List.of(entry(GROUP_OF_MARTIN, true, rightOfAccounting)));
      assertThat(entriesOfModuleFor(cookieOfPetra, moduleId))
          .contains(List.of(entry(GROUP_OF_PETRA, true, rightOfSales)));
    });

  }

  /**
   * The cockpit merges the hierarchies of all modules. So a group which one module gives brings
   * the entry of another module into effect, and the answer names the module of each entry.
   */
  @Test
  void aGroupWhichOneModuleGivesBringsTheEntryOfAnotherModuleIntoEffect() {

    final var givingModuleId = unique("giving-module");
    final var usingModuleId = unique("using-module");
    final var supervisors = unique("supervisors");
    final var rightToApprove = unique("right-to-approve");
    registerWithGroupHierarchy(givingModuleId, granting(GROUP_OF_MARTIN, supervisors));
    registerWithGroupHierarchy(usingModuleId, granting(supervisors, rightToApprove));

    await().untilAsserted(() -> {
      assertThat(entriesOfModuleFor(cookieOfMartin, givingModuleId))
          .contains(List.of(entry(GROUP_OF_MARTIN, true, supervisors)));
      assertThat(entriesOfModuleFor(cookieOfMartin, usingModuleId))
          .contains(List.of(entry(supervisors, false, rightToApprove)));
      assertThat(entriesOfModuleFor(cookieOfPetra, usingModuleId))
          .contains(List.of());
    });

  }

  /**
   * A module without a hierarchy has nothing to explain, so it is not listed. A module which
   * drops its hierarchy on its next registration disappears from the answer.
   */
  @Test
  void aModuleWithoutAGroupHierarchyIsNotListed() {

    final var moduleId = unique("module-without-hierarchy");
    registerWithGroupHierarchy(moduleId, granting(GROUP_OF_MARTIN, unique("right")));
    await().untilAsserted(() -> assertThat(entriesOfModuleFor(cookieOfMartin, moduleId)).isPresent());

    register(moduleId, "");

    await().untilAsserted(() -> assertThat(entriesOfModuleFor(cookieOfMartin, moduleId)).isEmpty());

  }

  /**
   * Unlike {@code current-user}, the path is not open to everybody. Without a login there is
   * nobody to answer about.
   */
  @Test
  void withoutALoginTheAnswerIsUnauthorized() {

    final var response = send(HttpRequest
        .newBuilder(URI.create(url("/gui/api/v1"
            + GROUP_HIERARCHY)))
        .GET()
        .build());

    assertThat(response.statusCode()).isEqualTo(401);

  }

}
