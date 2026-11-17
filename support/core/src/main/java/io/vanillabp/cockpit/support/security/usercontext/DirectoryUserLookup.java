package io.vanillabp.cockpit.support.security.usercontext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the user and the groups of a valid login token, on every request.
 * <p>
 * The login token of the Business Cockpit names the user but carries no groups. They come from the
 * {@link UserDirectoryService} of the application. So a group which the directory takes away is
 * gone on the next request, or once the cache of the directory expires.
 * <p>
 * A user the directory does not know keeps its login, but it has no groups, only its own authority
 * {@code USER_} plus the user id. That is worth a warning, but a browser sends many requests, so
 * the warning comes once a minute at most for each user. A directory which fails fails the request
 * as well. Going on without groups would show the user less than it may see, and nobody would know
 * why.
 * <p>
 * The lookup knows no platform. The Spring Boot module and the Quarkus module of the support both
 * find the user with it.
 */
public class DirectoryUserLookup {

  /**
   * The start of the authority each user has for itself, followed by the user id. With it, a check
   * for a list of authorities can name single users as well as groups.
   */
  public static final String USER_AUTHORITY_PREFIX = "USER_";

  /**
   * How long the lookup keeps quiet about a user the directory does not know, after it warned about
   * that user once.
   */
  static final Duration UNKNOWN_USER_WARNING_INTERVAL = Duration.ofMinutes(1);

  /**
   * Above this number of noted warnings, the old ones are dropped, so the map does not grow
   * forever.
   */
  private static final int MAX_NOTED_WARNINGS = 1000;

  private final UserDirectoryService userDirectory;

  private final Function<Collection<String>, Collection<String>> groupResolver;

  private final Logger logger;

  private final InstantSource clock;

  private final Map<String, Instant> lastWarningAboutAnUnknownUser = new ConcurrentHashMap<>();

  /**
   * Creates a lookup which writes its warnings under its own name.
   *
   * @param userDirectory The directory of the application
   * @param groupResolver Turns the groups the directory gives into all groups of the user, for
   *                      example by a group hierarchy. Pass {@code Function.identity()} to take
   *                      them as they are.
   */
  public DirectoryUserLookup(
      final UserDirectoryService userDirectory,
      final Function<Collection<String>, Collection<String>> groupResolver) {

    this(userDirectory, groupResolver, LoggerFactory.getLogger(DirectoryUserLookup.class), Clock.systemUTC());

  }

  /**
   * Creates a lookup which writes its warnings to the given logger.
   *
   * @param userDirectory The directory of the application
   * @param groupResolver Turns the groups the directory gives into all groups of the user. Pass
   *                      {@code Function.identity()} to take them as they are.
   * @param logger        Where the warnings and errors go, so they appear under the name of the
   *                      class which uses the lookup
   * @param clock         Says when the next warning about an unknown user is due
   */
  public DirectoryUserLookup(
      final UserDirectoryService userDirectory,
      final Function<Collection<String>, Collection<String>> groupResolver,
      final Logger logger,
      final InstantSource clock) {

    this.userDirectory = userDirectory;
    this.groupResolver = groupResolver;
    this.logger = logger;
    this.clock = clock;

  }

  /**
   * Asks the directory for the user of a valid login token.
   *
   * @param userId The subject of the token
   * @return The user with its authorities
   * @throws RuntimeException What the directory threw, after it was logged
   */
  public LoggedInUser lookUp(
      final String userId) {

    final Optional<UserDetails> user;
    try {
      user = userDirectory.getUser(userId);
    } catch (RuntimeException e) {
      logger.error("The user directory could not load the user '{}', so the request fails.", userId, e);
      throw e;
    }

    final var authorities = new HashSet<String>();
    authorities.add(USER_AUTHORITY_PREFIX + userId);

    if (user.isEmpty()) {
      warnAboutAnUnknownUser(userId);
      return new LoggedInUser(userId, Optional.empty(), authorities);
    }

    final var groups = Optional
        .ofNullable(user.get().getAuthorities())
        .orElse(List.of());
    authorities.addAll(groupResolver.apply(groups));

    return new LoggedInUser(userId, user, authorities);

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
        userId, USER_AUTHORITY_PREFIX, userId);

  }

}
