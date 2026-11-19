package io.vanillabp.cockpit.support.userinterface;

import java.net.URI;
import java.util.Locale;

/**
 * The route of one workflow module which passes the files of its user interface on to the server
 * which serves them, usually an nginx. The cockpit loads the user interface of a workflow module
 * from {@code /wm/<id>/bc-manifest.json} and forwards that to {@code /<id>/bc-manifest.json} of
 * the application. The same holds for the files the manifest names. If those files are not in the
 * application, because the user interface runs in an nginx of its own, this route passes every such
 * request on, with the path unchanged.
 *
 * <p>
 * The API of the workflow module is below {@code /<id>/api/} and stays in the application. The
 * route never takes a path below it, and never a path with a dot segment or with an encoded dot
 * or slash, so a request cannot leave the path of its module on the other server.
 * </p>
 *
 * <p>
 * Both platform modules of {@code support} build their route from this class, so both take the
 * same requests.
 * </p>
 *
 * @param workflowModuleId The id of the workflow module, as in its {@code META-INF/workflow-module}
 * @param address          Where the files are served: only the scheme, the host and the port
 */
public record UserInterfaceRoute(
                                 String workflowModuleId,
                                 URI address) {

  /** The part of the key after the id of the workflow module. */
  public static final String KEY_SUFFIX = ".user-interface.address";

  /**
   * The key of the address of a workflow module: {@code <id>.user-interface.address}. The key is
   * below the id of the module, like every key the module reads itself, so each module of an
   * application gets an address of its own. It is not below {@code vanillabp}, because VanillaBP
   * and the cockpit adapter own that namespace and refuse keys they do not read.
   *
   * @param workflowModuleId The id of the workflow module
   * @return The key
   */
  public static String keyOf(
      final String workflowModuleId) {

    return workflowModuleId + KEY_SUFFIX;

  }

  /**
   * The name of the route, which a platform uses as the name of its bean or for its log:
   * {@code <id>-user-interface}.
   *
   * @return The name
   */
  public String name() {

    return workflowModuleId
        + "-user-interface";

  }

  /**
   * The port of the address, with the default of its scheme if the address names none.
   *
   * @return The port
   */
  public int port() {

    if (address.getPort() != -1) {
      return address.getPort();
    }
    return isTls()
        ? 443
        : 80;

  }

  /**
   * Whether the address asks for TLS.
   *
   * @return {@code true} for {@code https}
   */
  public boolean isTls() {

    return "https".equals(address.getScheme().toLowerCase(Locale.ROOT));

  }

  /**
   * Whether the route passes a request for this path on. It does for every path below
   * {@code /<id>/}, except the API below {@code /<id>/api/} and paths which could climb out of the
   * module's path on the other server. The path is the one the request carries, still encoded and
   * without the query.
   *
   * @param path The path of the request, below the context path of the application
   * @return {@code true} if the server of the user interface answers it
   */
  public boolean forwards(
      final String path) {

    if (path == null) {
      return false;
    }
    final var files = "/"
        + workflowModuleId
        + "/";
    if (!path.startsWith(files)) {
      return false;
    }
    final var below = path.substring(files.length());
    if (below.equals("api") || below.startsWith("api/")) {
      return false;
    }
    final var lowerCase = below.toLowerCase(Locale.ROOT);
    if (lowerCase.contains("%2e") || lowerCase.contains("%2f") || lowerCase.contains("%5c") || below.contains("\\")) {
      return false;
    }
    for (final var segment : below.split("/", -1)) {
      if (segment.equals(".") || segment.equals("..")) {
        return false;
      }
    }
    return true;

  }

}
