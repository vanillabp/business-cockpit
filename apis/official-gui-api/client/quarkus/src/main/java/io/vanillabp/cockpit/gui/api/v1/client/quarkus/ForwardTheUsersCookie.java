package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import java.lang.reflect.Method;
import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialNotificationConfigApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowlistApi;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

/**
 * Sends the cookie of the user's request along with each call of the official GUI API. The cookie
 * holds the user's login, so the cockpit answers as the user who sent the request to the workflow
 * module. The workflow module needs no key and no user directory for that, because the cockpit
 * checks the login.
 * <p>
 * The cookie is taken from the HTTP request the application answers while it calls the cockpit. A
 * call outside of a request, for example in a test or in a scheduled job, sends no cookie. A call
 * which sets the {@code Cookie} header itself keeps its own.
 * <p>
 * Quarkus registers the filter for every REST client, because it is a {@link Provider}. So it
 * applies to every client of the API, however it is built. It forwards the cookie only to the
 * methods of the interfaces of this API, so a client of another service never gets the user's
 * login.
 */
@Provider
public class ForwardTheUsersCookie implements ClientRequestFilter {

  /** The interfaces generated from the API, one per tag of the OpenAPI file. */
  public static final List<Class<?>> APIS = List.of(
      OfficialTasklistApi.class,
      OfficialWorkflowlistApi.class,
      OfficialWorkflowModulesApi.class,
      OfficialNotificationConfigApi.class,
      OfficialCurrentUserApi.class);

  /** Where Quarkus' REST client names the method a request is sent for. */
  static final String INVOKED_METHOD = "org.eclipse.microprofile.rest.client.invokedMethod";

  /**
   * Creates the filter. Quarkus calls this constructor.
   */
  public ForwardTheUsersCookie() {
  }

  @Override
  public void filter(
      final ClientRequestContext request) {

    if (!(request.getProperty(INVOKED_METHOD) instanceof Method method) || !isOfTheApi(
        method.getDeclaringClass()) || request.getHeaders().containsKey(HttpHeaders.COOKIE)) {
      return;
    }
    final var cookie = cookieOfTheUsersRequest();
    if (cookie != null) {
      request.getHeaders().putSingle(HttpHeaders.COOKIE, cookie);
    }

  }

  private static boolean isOfTheApi(
      final Class<?> type) {
    return APIS.stream().anyMatch(api -> api.isAssignableFrom(type));
  }

  private static String cookieOfTheUsersRequest() {

    final var container = Arc.container();
    if ((container == null) || !container.requestContext().isActive()) {
      return null;
    }
    final var currentRequest = container.select(CurrentVertxRequest.class);
    if (!currentRequest.isResolvable() || (currentRequest.get().getCurrent() == null)) {
      return null;
    }
    return currentRequest.get().getCurrent().request().getHeader(HttpHeaders.COOKIE);

  }

}
