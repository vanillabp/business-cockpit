package io.vanillabp.cockpit.support.security.usercontext;

import io.quarkus.security.identity.SecurityIdentity;
import io.vanillabp.cockpit.support.security.jwt.LoginTokenIdentityProvider;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Stands in for the endpoints of a workflow module's form. Each answers what a test wants to know
 * about the user of the request.
 */
@Path("/form")
public class FormResource {

  @Inject
  UserContext userContext;

  @Inject
  SecurityIdentity identity;

  /**
   * Open to everybody, so a test sees who the request belongs to.
   *
   * @return The user, or {@code anonymous}
   */
  @GET
  @Path("/free")
  @PermitAll
  public String free() {

    return identity.isAnonymous()
        ? "anonymous"
        : identity.getPrincipal().getName();

  }

  /**
   * The user id, as the user context tells it.
   *
   * @return The user id
   */
  @GET
  @Path("/who")
  public String who() {

    return userContext.getUserLoggedIn();

  }

  /**
   * The details of the user, one per line: id, display, display short, e-mail and the sorted
   * authorities.
   *
   * @return The details
   */
  @GET
  @Path("/details")
  public String details() {

    final var details = userContext.getUserLoggedInDetails();
    return String.join("\n",
        details.getId(),
        String.valueOf(details.getDisplay()),
        String.valueOf(details.getDisplayShort()),
        String.valueOf(details.getEmail()),
        String.join(",", details.getAuthorities().stream().sorted().toList()));

  }

  /**
   * The roles of the request, sorted.
   *
   * @return The roles
   */
  @GET
  @Path("/roles")
  @PermitAll
  public String roles() {

    return String.join(",", identity.getRoles().stream().sorted().toList());

  }

  /**
   * Whether the identity carries the checked token.
   *
   * @return The subject of the token
   */
  @GET
  @Path("/token")
  @PermitAll
  public String token() {

    final io.vanillabp.cockpit.support.security.jwt.LoginToken token = identity
        .getAttribute(LoginTokenIdentityProvider.LOGIN_TOKEN_ATTRIBUTE);
    return token.subject();

  }

  /**
   * Only for the group {@code SALES}.
   *
   * @return {@code sold}
   */
  @GET
  @Path("/sales")
  @RolesAllowed("SALES")
  public String sales() {

    return "sold";

  }

}
