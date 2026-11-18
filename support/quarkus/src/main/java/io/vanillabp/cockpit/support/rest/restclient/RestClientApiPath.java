package io.vanillabp.cockpit.support.rest.restclient;

import java.util.List;

/**
 * The path of an API below the address of its server, which {@link RestClients} adds to
 * {@code base-url} for every client of one of the API's interfaces. A client module produces it as
 * a bean, so the application sets only the address of the server, for example
 * {@code https://cockpit.example.com}, and a client of a new version of the API brings its new path
 * along:
 *
 * <pre>
 * &#64;Produces
 * &#64;Singleton
 * &#64;Unremovable
 * RestClientApiPath officialGuiApiPath() {
 *   return new RestClientApiPath(OfficialGuiApiPath.PATH, List.of(OfficialTasklistApi.class));
 * }
 * </pre>
 *
 * The bean is looked up by {@link RestClients} only, so it needs {@code @Unremovable}. A
 * {@code base-url} which ends with the path stops the start, because each call would hold the path
 * twice.
 *
 * @param path the path, for example {@code /gui/api/v1}, best taken from the server URL of the
 *             OpenAPI file at build time
 * @param apis the interfaces of the API. A client of one of them, or of an interface which extends
 *             one of them, gets the path.
 */
public record RestClientApiPath(
                                String path,
                                List<Class<?>> apis) {

  /**
   * Checks and keeps the values.
   *
   * @param path the path, for example {@code /gui/api/v1}
   * @param apis the interfaces of the API
   */
  public RestClientApiPath {

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
   * Whether a client interface belongs to this API.
   *
   * @param type the client interface
   * @return whether it is one of the interfaces of the API or extends one of them
   */
  public boolean covers(
      final Class<?> type) {
    return apis.stream().anyMatch(api -> api.isAssignableFrom(type));
  }

}
