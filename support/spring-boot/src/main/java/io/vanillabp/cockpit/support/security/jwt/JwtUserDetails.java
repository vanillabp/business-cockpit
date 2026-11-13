package io.vanillabp.cockpit.support.security.jwt;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;

/**
 * The details of a user logged in by a login token (a JSON Web Token, JWT). The id is the subject
 * of the token. The name and the e-mail address come from the user directory. If the directory
 * does not know the user or has no value, they come from the claims of the token. The authorities
 * are those of the authentication, so the groups the directory gave for this request.
 */
public class JwtUserDetails implements UserDetails {

  private static final Pattern PATTERN_NAME = Pattern.compile("^(\\S+)\\s+(.*)$");
  /**
   * The start of the authority each user has for itself, followed by the user id. With it, a check
   * for a list of authorities can name single users as well as groups.
   */
  public static final String USER_AUTHORITY_PREFIX = "USER_";

  private final JwtAuthenticationToken authenticationToken;

  /**
   * Creates the details out of an authentication.
   *
   * @param authenticationToken The authentication, which holds the token
   */
  public JwtUserDetails(
      final JwtAuthenticationToken authenticationToken) {

    this.authenticationToken = authenticationToken;

  }

  private Jwt getJwt() {
    return (Jwt) authenticationToken.getDetails();
  }

  @Override
  public String getId() {

    return getJwt().getSubject();

  }

  private String fromTheDirectory(
      final Function<UserDetails, String> value) {

    return authenticationToken
        .getDirectoryUser()
        .map(value)
        .orElse(null);

  }

  @Override
  public String getDisplay() {

    final var fromTheDirectory = fromTheDirectory(UserDetails::getDisplay);
    if (fromTheDirectory != null) {
      return fromTheDirectory;
    }
    final var name = getJwt().getClaimAsString("name");
    final var givenName = getJwt().getClaimAsString("given_name");
    final var familyName = getJwt().getClaimAsString("family_name");
    if (familyName != null) {
      if (givenName == null) {
        return familyName;
      }
      return familyName
          + ", "
          + givenName;
    }
    return name;

  }

  @Override
  public String getDisplayShort() {

    final var fromTheDirectory = fromTheDirectory(UserDetails::getDisplayShort);
    if (fromTheDirectory != null) {
      return fromTheDirectory;
    }
    final var name = getJwt().getClaimAsString("name");
    final var givenName = getJwt().getClaimAsString("given_name");
    final var familyName = getJwt().getClaimAsString("family_name");
    if (familyName != null) {
      if (givenName == null) {
        return familyName;
      }
      return familyName
          + ", "
          + givenName;
    }
    if (name == null) {
      return null;
    }
    final var posOfSeparator = name.indexOf(' ');
    if (posOfSeparator == -1) {
      return name;
    }
    final var matcher = PATTERN_NAME.matcher(name);
    if (!matcher.matches()) {
      return name;
    }
    return matcher.group(1)
        + ", "
        + matcher.group(2).trim();

  }

  /**
   * The e-mail address the directory knows, or else the claim {@code email} of the token. The
   * subject is the user id and no e-mail address.
   *
   * @return The e-mail address, or {@code null} if neither knows one
   */
  @Override
  public String getEmail() {

    final var fromTheDirectory = fromTheDirectory(UserDetails::getEmail);
    if (fromTheDirectory != null) {
      return fromTheDirectory;
    }
    return getJwt().getClaimAsString("email");

  }

  @Override
  public List<String> getAuthorities() {

    return authenticationToken
        .getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

  }

}
