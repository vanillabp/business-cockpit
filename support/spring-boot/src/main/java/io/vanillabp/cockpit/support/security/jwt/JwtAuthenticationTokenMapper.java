package io.vanillabp.cockpit.support.security.jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
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
 */
public class JwtAuthenticationTokenMapper extends JwtMapper<JwtAuthenticationToken> {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationTokenMapper.class);

  /**
   * How long the mapper keeps quiet about a user the directory does not know, after it warned
   * about that user once. A browser sends many requests, and one warning for each would flood the
   * log.
   */
  static final Duration UNKNOWN_USER_WARNING_INTERVAL = Duration.ofMinutes(1);

  /**
   * Above this number of noted warnings, the old ones are dropped, so the map does not grow
   * forever.
   */
  private static final int MAX_NOTED_WARNINGS = 1000;

  private final UserDirectoryService userDirectory;

  private final Function<Collection<String>, Collection<String>> groupResolverFunction;

  private final Map<String, Instant> lastWarningAboutAnUnknownUser = new ConcurrentHashMap<>();

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
    this.groupResolverFunction = groupResolverFunction;

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
    this.groupResolverFunction = groupResolverFunction;

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

    final var userId = jwt.getSubject();
    final Optional<UserDetails> user;
    try {
      user = userDirectory.getUser(userId);
    } catch (RuntimeException e) {
      logger.error("The user directory could not load the user '{}', so the request fails.", userId, e);
      throw e;
    }

    if (user.isEmpty()) {
      warnAboutAnUnknownUser(userId);
      return new JwtAuthenticationToken(jwt, bcAuthorities);
    }

    final var groups = Optional
        .ofNullable(user.get().getAuthorities())
        .orElse(List.of());
    groupResolverFunction
        .apply(groups)
        .stream()
        .map(SimpleGrantedAuthority::new)
        .forEach(bcAuthorities::add);

    return new JwtAuthenticationToken(jwt, bcAuthorities, user.get());

  }

  private void warnAboutAnUnknownUser(
      final String userId) {

    final var now = clock.instant();
    final var lastWarning = lastWarningAboutAnUnknownUser.get(userId);
    if ((lastWarning != null) && now.isBefore(lastWarning.plus(UNKNOWN_USER_WARNING_INTERVAL))) {
      return;
    }
    if (lastWarningAboutAnUnknownUser.size() >= MAX_NOTED_WARNINGS) {
      lastWarningAboutAnUnknownUser
          .values()
          .removeIf(noted -> !now.isBefore(noted.plus(UNKNOWN_USER_WARNING_INTERVAL)));
    }
    lastWarningAboutAnUnknownUser.put(userId, now);
    logger.warn("""
        The user directory does not know the user '{}' of a valid login token. The user keeps its \
        login, but it has no groups, only its own authority '{}{}'. This warning comes once a \
        minute at most for each user. Check whether the user was removed from the directory, or \
        whether the directory knows the user by another id than the subject of the token.""",
        userId, JwtUserDetails.USER_AUTHORITY_PREFIX, userId);

  }

  /**
   * Writes the user id as subject. The id is the name of the authentication. For a login by user
   * name and password it is the user name. For a login by OpenID Connect it is the claim Spring
   * Security is configured to name the user by, {@code sub} by default. No groups are written.
   */
  @Override
  protected void applyJwtClaimsSet(
      final JwtClaimsSet.Builder claimsSetBuilder,
      final SecurityContext context) {

    claimsSetBuilder
        .issuer("bc")
        .subject(context.getAuthentication().getName())
        .audience(List.of("bc"));

  }

}
