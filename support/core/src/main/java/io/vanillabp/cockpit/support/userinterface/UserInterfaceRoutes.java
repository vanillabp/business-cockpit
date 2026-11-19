package io.vanillabp.cockpit.support.userinterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Finds the routes of the user interfaces an application needs, and every key which stops one from
 * working. Both platform modules of {@code support} call it while the application starts, so both
 * read the same keys and refuse the same mistakes.
 *
 * <p>
 * A workflow module gets a route if the application sets {@code <id>.user-interface.address}. The
 * workflow modules are the ones with a file {@code META-INF/workflow-module} on the class path, the
 * same file VanillaBP reads. The module itself needs no class and no key for its route.
 * </p>
 */
public final class UserInterfaceRoutes {

  /** The file of a workflow module which holds its id, the same one VanillaBP reads. */
  public static final String WORKFLOW_MODULE_FILE = "META-INF/workflow-module";

  private UserInterfaceRoutes() {
  }

  /**
   * The ids of the workflow modules on the class path: the content of each file
   * {@value #WORKFLOW_MODULE_FILE}, without the white space around it.
   *
   * @param classLoader The class loader of the application
   * @return The ids, sorted, each once
   */
  public static List<String> workflowModulesOn(
      final ClassLoader classLoader) {

    final var ids = new TreeSet<String>();
    try {
      final var files = classLoader.getResources(WORKFLOW_MODULE_FILE);
      for (final var file : Collections.list(files)) {
        try (InputStream content = file.openStream()) {
          final var id = new String(content.readAllBytes(), StandardCharsets.UTF_8).strip();
          if (!id.isEmpty()) {
            ids.add(id);
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read the files "
          + WORKFLOW_MODULE_FILE, e);
    }
    return List.copyOf(ids);

  }

  /**
   * The routes the application asks for, one for each workflow module whose address is set.
   *
   * @param workflowModuleIds The ids of the workflow modules of the application
   * @param valueOf           Reads the value of a key, empty if the key is not set
   * @param propertyNames     The names of all keys the application sets, to find an address for a
   *                          workflow module the application does not have
   * @param proxyIsThere      Whether the application has the library which passes a request on
   * @param howToAddTheProxy  What to add if that library is missing, a sentence for the developer
   * @return The routes, sorted by the id of the workflow module
   * @throws UserInterfaceRoutesMisconfigured with every key which stops a route from working
   */
  public static List<UserInterfaceRoute> of(
      final Collection<String> workflowModuleIds,
      final Function<String, Optional<String>> valueOf,
      final Iterable<String> propertyNames,
      final boolean proxyIsThere,
      final String howToAddTheProxy) {

    final var problems = new ArrayList<String>();
    final var routes = new ArrayList<UserInterfaceRoute>();
    for (final var workflowModuleId : new TreeSet<>(workflowModuleIds)) {
      final var key = UserInterfaceRoute.keyOf(workflowModuleId);
      final var value = valueOf
          .apply(key)
          .map(String::strip)
          .filter(text -> !text.isEmpty());
      if (value.isEmpty()) {
        continue;
      }
      final var problem = problemOf(key, value.get());
      if (problem != null) {
        problems.add(problem);
        continue;
      }
      routes.add(new UserInterfaceRoute(workflowModuleId, URI.create(value.get())));
    }
    problems.addAll(addressesOfUnknownModules(workflowModuleIds, propertyNames));
    if (!routes.isEmpty() && !proxyIsThere) {
      problems.add("""
          '%s' asks for a route to the user interface, but the application has no library which \
          passes a request on. %s"""
          .formatted(UserInterfaceRoute.keyOf(routes.getFirst().workflowModuleId()), howToAddTheProxy));
    }
    if (!problems.isEmpty()) {
      throw new UserInterfaceRoutesMisconfigured(problems);
    }
    return List.copyOf(routes);

  }

  private static String problemOf(
      final String key,
      final String value) {

    final URI address;
    try {
      address = new URI(value);
    } catch (URISyntaxException e) {
      return "'%s' is no address: %s. Write it like 'http://nginx:8080'.".formatted(key, e.getMessage());
    }
    final var scheme = address.getScheme() == null
        ? null
        : address.getScheme().toLowerCase(Locale.ROOT);
    if (!"http".equals(scheme) && !"https".equals(scheme) || address.getHost() == null) {
      return """
          '%s' is '%s', but it needs the scheme, the host and maybe the port of the server of the \
          user interface, like 'http://nginx:8080'.""".formatted(key, value);
    }
    if (address.getRawUserInfo() != null) {
      return """
          '%s' holds a user and maybe a password. The route sends no login to the server of the \
          user interface, so remove them.""".formatted(key);
    }
    final var path = address.getRawPath();
    if ((path != null && !path.isEmpty() && !path.equals("/")) || address.getRawQuery() != null || address
        .getRawFragment() != null) {
      return """
          '%s' is '%s', but only its scheme, host and port count. The path of a request stays as \
          it is, so the server of the user interface serves the files below '/<id of the module>/'. \
          Remove everything after the port.""".formatted(key, value);
    }
    return null;

  }

  private static List<String> addressesOfUnknownModules(
      final Collection<String> workflowModuleIds,
      final Iterable<String> propertyNames) {

    final var unknown = new TreeSet<String>();
    for (final var propertyName : propertyNames) {
      final var key = withoutAProfile(propertyName);
      if (!key.endsWith(UserInterfaceRoute.KEY_SUFFIX)) {
        continue;
      }
      final var workflowModuleId = key.substring(0, key.length() - UserInterfaceRoute.KEY_SUFFIX.length());
      if (!workflowModuleIds.contains(workflowModuleId)) {
        unknown.add(propertyName);
      }
    }
    final var known = workflowModuleIds.isEmpty()
        ? "The application has no workflow module with a file "
            + WORKFLOW_MODULE_FILE
            + "."
        : "The workflow modules of the application are: "
            + String.join(", ", new TreeSet<>(workflowModuleIds))
            + ".";
    return unknown
        .stream()
        .map(key -> """
            '%s' names no workflow module of this application, so there is no route for it. %s \
            Correct the id in the key or remove the key."""
            .formatted(key, known))
        .toList();

  }

  /**
   * A key which holds for one profile is the same key on Quarkus: {@code %dev.a.user-interface.address}
   * names the workflow module {@code a} as well.
   */
  private static String withoutAProfile(
      final String propertyName) {

    if (!propertyName.startsWith("%")) {
      return propertyName;
    }
    final var name = propertyName.indexOf('.');
    return name < 0
        ? propertyName
        : propertyName.substring(name + 1);

  }

}
