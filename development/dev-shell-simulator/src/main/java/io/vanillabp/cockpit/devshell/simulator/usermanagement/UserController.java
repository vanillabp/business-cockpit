package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.TransientSecurityContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.devshell.simulator.api.DevShellUser;
import io.vanillabp.cockpit.devshell.simulator.api.DevShellUserApi;
import io.vanillabp.cockpit.devshell.simulator.config.Properties;
import io.vanillabp.cockpit.support.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The REST endpoints the dev shell uses to list the made-up users and to switch between them.
 * Business applications use them too, to load the current user. The paths and the answers are
 * described in the simulator's own OpenAPI file, {@code dev-shell-simulator-api.yaml}.
 */
@RestController
public class UserController implements DevShellUserApi {

  private final Properties properties;

  private final JwtServerSecurityContextRepository securityContextRepository;

  private final JwtProperties jwtProperties;

  private final SignedInUser signedInUser;

  /**
   * Creates the controller.
   *
   * @param properties The properties which list the users
   * @param securityContextRepository Keeps the current user in a JWT cookie
   * @param jwtProperties The settings of the JWT and its cookie
   */
  public UserController(
      final Properties properties,
      final JwtServerSecurityContextRepository securityContextRepository,
      final JwtProperties jwtProperties) {

    this.properties = properties;
    this.securityContextRepository = securityContextRepository;
    this.jwtProperties = jwtProperties;
    this.signedInUser = new SignedInUser(securityContextRepository);

  }

  /**
   * Used by the Dev-Shell to get the list of all user ids.
   *
   * @return The ids of all configured users
   */
  @Override
  public ResponseEntity<List<String>> allUsersIds() {

    return ResponseEntity.ok(properties.getUsers().stream().map(User::getId).toList());

  }

  /**
   * Used by the Dev-Shell to get the list of all user details.
   *
   * @return All configured users
   */
  @Override
  public ResponseEntity<List<DevShellUser>> allUsers() {

    return ResponseEntity.ok(properties.getUsers().stream().map(UserController::toApi).toList());

  }

  /**
   * Used by Dev-Shell to retrieve the user id based on the JWT.
   *
   * @return The id of the current user, or "not found" if nobody is logged in
   */
  @Override
  public ResponseEntity<String> getUser() {

    return signedInUser
        .id()
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

  }

  /**
   * Used by business applications to load the current user based on the JWT.
   *
   * @return The current user, or "not found" if nobody is logged in or the user is unknown
   */
  @Override
  public ResponseEntity<DevShellUser> getUserDetails() {

    return signedInUser
        .id()
        .map(properties::getUser)
        .map(UserController::toApi)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

  }

  /**
   * Used by business applications to get user details by user id.
   *
   * @param userId The id of the user
   * @return The user, or "not found" if the id is unknown
   */
  @Override
  public ResponseEntity<DevShellUser> getUserById(
      final String userId) {

    return Optional
        .ofNullable(properties.getUser(userId))
        .map(UserController::toApi)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

  }

  /**
   * Used by Dev-Shell to change the user logged on. The user id {@code ---} logs the
   * current user out.
   *
   * @param userId The id of the user to switch to
   * @return "bad request" if the id is empty, "not found" if no user has the id, otherwise "ok"
   */
  @Override
  public ResponseEntity<Void> changeUser(
      final String userId) {

    if (!StringUtils.hasText(userId)) {
      return ResponseEntity.badRequest().build();
    }
    if (userId.equals("---")) {
      logout(SignedInUser.currentResponse());
      return ResponseEntity.ok().build();
    }
    final var user = properties.getUser(userId);
    if (user == null) {
      return ResponseEntity.notFound().build();
    }
    setJwtForChangedUser(SignedInUser.currentRequest(), SignedInUser.currentResponse(), user);
    return ResponseEntity.ok().build();

  }

  private static DevShellUser toApi(
      final User user) {

    return new DevShellUser()
        .id(user.getId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .groups(user.getGroups())
        .attributes(user.getAttributes());

  }

  /**
   * Used by Dev-Shell to logout the current user.
   */
  private void logout(
      final HttpServletResponse response) {

    final var cookie = new Cookie(jwtProperties.getCookie().getName(), "");
    cookie.setMaxAge(0);
    cookie.setPath(jwtProperties.getCookie().getPath());
    cookie.setDomain(jwtProperties.getCookie().getDomain());
    cookie.setSecure(jwtProperties.getCookie().isSecure());
    cookie.setHttpOnly(true);
    final var sameSite = getSecurityCookieSameSiteFromEnum();
    if (sameSite != null) {
      cookie.setAttribute("SameSite", sameSite);
    }
    response.addCookie(cookie);

  }

  private void setJwtForChangedUser(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final User user) {

    final var authorities = Optional.ofNullable(user.getGroups())
        .map(groups -> groups.stream().map(SimpleGrantedAuthority::new).toList())
        .orElse(List.of());
    final var userDetails = new org.springframework.security.core.userdetails.User(user.getId(), "", authorities);
    final var context = new TransientSecurityContext(
        new UsernamePasswordAuthenticationToken(userDetails, "", authorities));
    securityContextRepository.saveContext(context, request, response);

  }

  private String getSecurityCookieSameSiteFromEnum() {

    final var sameSite = this.jwtProperties.getCookie().getSameSite();
    return sameSite == null ? null : sameSite.attributeValue();

  }

}
