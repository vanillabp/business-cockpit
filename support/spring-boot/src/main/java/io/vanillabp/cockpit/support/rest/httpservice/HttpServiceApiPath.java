package io.vanillabp.cockpit.support.rest.httpservice;

import java.util.List;

/**
 * The path of an API below the address of its server, which every HTTP service group with one of
 * the API's interfaces adds to its {@code base-url}. A client module declares it as a bean, so the
 * application sets only the address of the server, for example {@code https://cockpit.example.com},
 * and a client of a new version of the API brings its new path along:
 *
 * <pre>
 * &#64;Bean
 * HttpServiceApiPath officialGuiApiPath() {
 *   return new HttpServiceApiPath(OfficialGuiApiPath.PATH, List.of(OfficialTasklistApi.class));
 * }
 * </pre>
 *
 * A group whose {@code base-url} ends with the path stops the start, because each call would hold
 * the path twice. See {@link HttpServiceClientConventions}.
 *
 * @param path the path, for example {@code /gui/api/v1}, best taken from the server URL of the
 *             OpenAPI file at build time
 * @param apis the interfaces of the API. A group with one of them, or with an interface which
 *             extends one of them, gets the path.
 */
public record HttpServiceApiPath(
                                 String path,
                                 List<Class<?>> apis) {

  /**
   * Checks and keeps the values.
   *
   * @param path the path, for example {@code /gui/api/v1}
   * @param apis the interfaces of the API
   */
  public HttpServiceApiPath {

    if ((path == null) || !path.startsWith("/")) {
      throw new IllegalArgumentException("""
          The path '%s' of an API starts with '/', for example '/gui/api/v1'."""
          .formatted(path));
    }
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    apis = List.copyOf(apis);

  }

  /**
   * Whether a type of an HTTP service group belongs to this API.
   *
   * @param type an interface of the group
   * @return whether it is one of the interfaces of the API or extends one of them
   */
  public boolean covers(
      final Class<?> type) {
    return apis.stream().anyMatch(api -> api.isAssignableFrom(type));
  }

}
