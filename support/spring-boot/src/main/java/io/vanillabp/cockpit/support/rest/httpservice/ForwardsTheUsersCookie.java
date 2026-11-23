package io.vanillabp.cockpit.support.rest.httpservice;

/**
 * A bean which sends the cookie of the user's request along with the calls of some HTTP service
 * groups, for example to call another application as the user who is logged in.
 * <p>
 * An HTTP service group has one HTTP client for all users. A group which forwards the user's cookie
 * therefore keeps no cookies of its own: with a store of cookies, a cookie the server sets in its
 * answer to one user would go out with the calls of other users. {@link HttpServiceClientConventions}
 * switches cookie handling off for such a group, whatever {@code spring.http.clients.cookie-handling}
 * says for all clients, and the group's own key {@code cookie-handling} stops the start unless it
 * is {@code disable}.
 */
public interface ForwardsTheUsersCookie {

  /**
   * Whether the calls of an interface carry the cookie of the user's request.
   *
   * @param type an interface of an HTTP service group
   * @return whether the calls of this interface carry the user's cookie
   */
  boolean forwardsTheUsersCookieOf(
      Class<?> type);

}
