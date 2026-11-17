package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import java.io.IOException;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Sends the cookie of the user's request along with each call of the official GUI API. The cookie
 * holds the user's login, so the cockpit answers as the user who sent the request to the workflow
 * module. The workflow module needs no key and no user directory for that, because the cockpit
 * checks the login.
 * <p>
 * The cookie is taken from the servlet request the current thread answers. A call outside of a
 * request, for example in a test or in a scheduled job, sends no cookie. A call which sets the
 * {@code Cookie} header itself keeps its own.
 * <p>
 * It applies to every HTTP service group with an interface of the API, the group of
 * {@link OfficialGuiApiClientAutoConfiguration} and the groups of the application alike.
 */
public class ForwardTheUsersCookie implements RestClientHttpServiceGroupConfigurer, ClientHttpRequestInterceptor {

  private static final boolean SERVLET_PRESENT = ClassUtils.isPresent("jakarta.servlet.http.HttpServletRequest",
      ForwardTheUsersCookie.class.getClassLoader());

  private final List<Class<?>> apis;

  /**
   * Creates the configurer.
   *
   * @param apis the interfaces of the API. A group with one of them, or with an interface which
   *             extends one of them, forwards the cookie.
   */
  public ForwardTheUsersCookie(
      final List<Class<?>> apis) {
    this.apis = List.copyOf(apis);
  }

  @Override
  public void configureGroups(
      final Groups<RestClient.Builder> groups) {

    groups
        .filter(group -> group.httpServiceTypes().stream().anyMatch(this::isOfTheApi))
        .forEachClient((
            group,
            builder) -> builder.requestInterceptor(this));

  }

  private boolean isOfTheApi(
      final Class<?> type) {
    return apis.stream().anyMatch(api -> api.isAssignableFrom(type));
  }

  @Override
  public ClientHttpResponse intercept(
      final HttpRequest request,
      final byte[] body,
      final ClientHttpRequestExecution execution) throws IOException {

    if (!request.getHeaders().containsHeader(HttpHeaders.COOKIE)) {
      final var cookie = cookieOfTheUsersRequest();
      if (cookie != null) {
        request.getHeaders().set(HttpHeaders.COOKIE, cookie);
      }
    }
    return execution.execute(request, body);

  }

  private static @Nullable String cookieOfTheUsersRequest() {
    return SERVLET_PRESENT ? ServletRequests.cookieOfTheCurrentRequest() : null;
  }

  /** Touches the servlet API, so it is loaded only if the servlet API is there. */
  private static final class ServletRequests {

    static @Nullable String cookieOfTheCurrentRequest() {

      if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
        return attributes.getRequest().getHeader(HttpHeaders.COOKIE);
      }
      return null;

    }

  }

}
