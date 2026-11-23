package io.vanillabp.cockpit.support.security.jwt;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import io.vanillabp.cockpit.support.security.usercontext.DirectoryUserLookup;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * Builds a {@link JwtAuthenticationToken} out of a token and a token out of a login.
 * <p>
 * A token names the user and the time of the login, but not the groups of the user. The groups
 * come from the {@link UserDirectoryService} on every request. So a group which the directory
 * takes away is gone on the next request, or once the cache of the directory expires. A token
 * written by an older version may still carry the claim {@value JwtMapper#AUTHORITIES_CLAIM}. It
 * stays valid, and the claim is not read.
 * <p>
 * The user id is the subject of the token. It is the name of the authentication of the login. So
 * a login by user name and password and a login by OpenID Connect both give the id under which the
 * directory knows the user.
 * <p>
 * The user and its groups are found by {@link DirectoryUserLookup} of the support's core, the same
 * lookup a Quarkus workflow module runs.
 */
public class JwtAuthenticationTokenMapper extends JwtMapper<JwtAuthenticationToken> {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationTokenMapper.class);

  private final UserDirectoryService userDirectory;

  private final DirectoryUserLookup userLookup;

  private Clock clock = Clock.systemUTC();

  /**
   * Creates a mapper which signs and checks tokens with the shared secret of the settings.
   *
   * @param properties            The token settings
   * @param userDirectory         Gives the groups of the user on every request
   * @param groupResolverFunction Turns the groups the directory gives into all groups of the user,
   *                              for example by a group hierarchy. Pass
   *                              {@code Function.identity()} to take them as they are.
   */
  public JwtAuthenticationTokenMapper(
      final JwtProperties properties,
      final UserDirectoryService userDirectory,
      final Function<Collection<String>, Collection<String>> groupResolverFunction) {

    super(properties);
    this.userDirectory = userDirectory;
    this.userLookup = lookupWithTheClockOfThisMapper(userDirectory, groupResolverFunction);

  }

  /**
   * Creates a mapper which signs and checks tokens with the keys of the given source.
   *
   * @param properties            The token settings, for the lifetime of a token
   * @param keySource             Signs new tokens and checks the ones sent in
   * @param userDirectory         Gives the groups of the user on every request
   * @param groupResolverFunction Turns the groups the directory gives into all groups of the user,
   *                              for example by a group hierarchy. Pass
   *                              {@code Function.identity()} to take them as they are.
   */
  public JwtAuthenticationTokenMapper(
      final JwtProperties properties,
      final JwtKeySource keySource,
      final UserDirectoryService userDirectory,
      final Function<Collection<String>, Collection<String>> groupResolverFunction) {

    super(properties, keySource);
    this.userDirectory = userDirectory;
    this.userLookup = lookupWithTheClockOfThisMapper(userDirectory, groupResolverFunction);

  }

  /**
   * The lookup writes under the name of this class and asks the clock of this mapper, so a test
   * can change it with {@link #setClock(Clock)}.
   */
  private DirectoryUserLookup lookupWithTheClockOfThisMapper(
      final UserDirectoryService userDirectory,
      final Function<Collection<String>, Collection<String>> groupResolverFunction) {

    return new DirectoryUserLookup(userDirectory, groupResolverFunction, logger, () -> clock.instant());

  }

  /**
   * Replaces the clock which decides when the next warning about an unknown user is due.
   *
   * @param clock The clock
   */
  void setClock(
      final Clock clock) {

    this.clock = clock;

  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  /**
   * Asks the directory for the user of the token. If the directory fails, the request fails as
   * well. Going on without groups would show the user less than it may see, and nobody would know
   * why.
   */
  @Override
  protected JwtAuthenticationToken buildAuthenticationToken(
      final Jwt jwt,
      final Collection<GrantedAuthority> bcAuthorities) {

    final var user = userLookup.lookUp(jwt.getSubject());
    user
        .authorities()
        .stream()
        .map(SimpleGrantedAuthority::new)
        .forEach(bcAuthorities::add);

    return new JwtAuthenticationToken(jwt, bcAuthorities, user.directoryUser().orElse(null));

  }

  /**
   * Writes the user id as subject. The id is the name of the authentication. For a login by user
   * name and password it is the user name. For a login by OpenID Connect it is the claim Spring
   * Security is configured to name the user by, {@code sub} by default. No groups are written.
   * <p>
   * If the directory knows the user, the claim {@value JwtMapper#KNOWN_TO_THE_DIRECTORY_CLAIM}
   * says so. Then the login is not renewed once the directory does not know the user any more,
   * see {@link JwtCookie#getUnknownUserRenewal()}.
   */
  @Override
  protected void applyJwtClaimsSet(
      final JwtClaimsSet.Builder claimsSetBuilder,
      final SecurityContext context) {

    final var userId = context.getAuthentication().getName();
    claimsSetBuilder
        .issuer("bc")
        .subject(userId)
        .audience(List.of("bc"));
    if (directoryKnows(userId)) {
      claimsSetBuilder.claim(KNOWN_TO_THE_DIRECTORY_CLAIM, true);
    }

  }

  /**
   * A directory which fails does not stop the login. The next request asks the directory again,
   * and it fails there if the directory still fails. The first renewal which finds the user notes
   * the claim.
   */
  private boolean directoryKnows(
      final String userId) {

    try {
      return userDirectory.getUser(userId).isPresent();
    } catch (RuntimeException e) {
      logger.debug("The user directory could not load the user '{}' at the login.", userId, e);
      return false;
    }

  }

}
