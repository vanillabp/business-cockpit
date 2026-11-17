package io.vanillabp.cockpit.support.security.jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.vanillabp.cockpit.support.security.usercontext.LoggedInUser;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;

/**
 * The details of a user logged in by a login token of the Business Cockpit. The id is the subject
 * of the token. The name and the e-mail address come from the user directory. If the directory does
 * not know the user or has no value, they come from the claims of the token: {@code name},
 * {@code given_name}, {@code family_name} and {@code email}. The authorities are the ones given,
 * usually those {@link io.vanillabp.cockpit.support.security.usercontext.DirectoryUserLookup}
 * found for this request.
 * <p>
 * It knows no platform. The Spring Boot module and the Quarkus module of the support both tell the
 * user with it.
 */
public class TokenUserDetails implements UserDetails {

  private static final Pattern PATTERN_NAME = Pattern.compile("^(\\S+)\\s+(.*)$");

  private final Map<String, Object> claims;

  private final Optional<UserDetails> directoryUser;

  private final List<String> authorities;

  /**
   * Creates the details out of the claims of a token.
   *
   * @param claims        The claims of the checked token
   * @param directoryUser The user as the directory gave it, or empty if the directory does not know
   *                      the user
   * @param authorities   The authorities of the user
   */
  public TokenUserDetails(
      final Map<String, Object> claims,
      final Optional<UserDetails> directoryUser,
      final Collection<String> authorities) {

    this.claims = claims;
    this.directoryUser = directoryUser;
    this.authorities = List.copyOf(authorities);

  }

  /**
   * Creates the details out of a checked token and the user the directory found for it.
   *
   * @param token The checked token
   * @param user  The user the directory found for the subject of the token
   */
  public TokenUserDetails(
      final LoginToken token,
      final LoggedInUser user) {

    this(token.claims(), user.directoryUser(), user.authorities());

  }

  private String claim(
      final String name) {

    return LoginToken.asString(claims.get(name));

  }

  private String fromTheDirectory(
      final Function<UserDetails, String> value) {

    return directoryUser
        .map(value)
        .orElse(null);

  }

  @Override
  public String getId() {

    return claim("sub");

  }

  @Override
  public String getDisplay() {

    final var fromTheDirectory = fromTheDirectory(UserDetails::getDisplay);
    if (fromTheDirectory != null) {
      return fromTheDirectory;
    }
    final var familyAndGivenName = familyAndGivenName();
    if (familyAndGivenName != null) {
      return familyAndGivenName;
    }
    return claim("name");

  }

  @Override
  public String getDisplayShort() {

    final var fromTheDirectory = fromTheDirectory(UserDetails::getDisplayShort);
    if (fromTheDirectory != null) {
      return fromTheDirectory;
    }
    final var familyAndGivenName = familyAndGivenName();
    if (familyAndGivenName != null) {
      return familyAndGivenName;
    }
    final var name = claim("name");
    if ((name == null) || (name.indexOf(' ') == -1)) {
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

  private String familyAndGivenName() {

    final var givenName = claim("given_name");
    final var familyName = claim("family_name");
    if (familyName == null) {
      return null;
    }
    if (givenName == null) {
      return familyName;
    }
    return familyName
        + ", "
        + givenName;

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
    return claim("email");

  }

  @Override
  public List<String> getAuthorities() {

    return authorities;

  }

}
