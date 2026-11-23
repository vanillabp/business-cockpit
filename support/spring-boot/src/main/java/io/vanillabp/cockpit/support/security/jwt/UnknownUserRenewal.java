package io.vanillabp.cockpit.support.security.jwt;

/**
 * Whether the login of a user whom the user directory does not know is renewed, the setting
 * {@code business-cockpit.jwt.cookie.unknown-user-renewal}. A login which is not renewed ends when
 * its token expires, at most {@code expires-duration} later.
 * <p>
 * The directory gives its answer from its cache. So a user removed from the directory, or disabled
 * there, counts as unknown only once the cache has forgotten the user. For the directories the
 * cockpit brings, that is at most {@code business-cockpit.user-directory.cache-duration} later, or
 * at the nightly clearing of the cache, whichever comes first.
 * <p>
 * See decision 91 in the repository's DECISIONS.md, and the addendum it names.
 */
public enum UnknownUserRenewal {

  /**
   * The login is renewed whether the directory knows the user or not, up to
   * {@code max-login-duration}.
   */
  ALLOW,

  /**
   * The default. The login is not renewed once the directory does not know the user any more,
   * although it knew the user at the login or at an earlier renewal. The token notes that in the
   * claim {@value JwtMapper#KNOWN_TO_THE_DIRECTORY_CLAIM}.
   * <p>
   * A user the directory never knew keeps the login as with {@link #ALLOW}. This is what happens in
   * an application whose directory does not know the users by the id of the login, for example
   * because it is not set up yet. Such an application keeps working, and the log warns about every
   * unknown user.
   */
  REFUSE_IF_KNOWN_BEFORE,

  /**
   * The login is not renewed whenever the directory does not know the user. In an application
   * whose directory knows nobody, every login ends after {@code expires-duration}.
   */
  REFUSE;

  /**
   * Whether a login whose user the directory does not know right now is renewed.
   *
   * @param knownBefore Whether the directory knew the user at the login or at an earlier renewal
   * @return Whether the login is renewed
   */
  boolean renews(
      final boolean knownBefore) {

    return switch (this) {
      case ALLOW -> true;
      case REFUSE_IF_KNOWN_BEFORE -> !knownBefore;
      case REFUSE -> false;
    };

  }

}
