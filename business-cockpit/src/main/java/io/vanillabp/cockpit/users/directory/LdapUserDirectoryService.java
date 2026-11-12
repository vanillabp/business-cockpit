package io.vanillabp.cockpit.users.directory;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.ContainerCriteria;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.util.StringUtils;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;

/**
 * The users and groups of an LDAP directory, read with Spring LDAP. The cockpit brings it if
 * {@code business-cockpit.user-directory.type} is {@code ldap} and the application has the
 * dependency {@code org.springframework.ldap:spring-ldap-core}.
 * <p>
 * A user is an entry of {@link LdapUserDirectoryProperties#userObjectClass()} below
 * {@link LdapUserDirectoryProperties#userSearchBase()}. Its groups are the entries below
 * {@link LdapUserDirectoryProperties#groupSearchBase()} which name the DN of the user in
 * {@link LdapUserDirectoryProperties#groupMemberAttribute()}. The name of a group is the group the
 * cockpit sees. A search looks at the id, the e-mail address and the full name of a user.
 */
public class LdapUserDirectoryService extends CachingUserDirectoryService {

  private final LdapUserDirectoryProperties properties;

  private final LdapTemplate ldapTemplate;

  private record LdapUser(
                          String dn,
                          String id,
                          String email,
                          String firstName,
                          String lastName,
                          String fullName) {
  }

  /**
   * Creates the directory.
   *
   * @param properties How to reach the directory and where the users and groups are
   * @param cacheDuration How long a user is kept with its groups
   */
  public LdapUserDirectoryService(
      final LdapUserDirectoryProperties properties,
      final Duration cacheDuration) {

    this(properties, cacheDuration, Clock.systemUTC());

  }

  LdapUserDirectoryService(
      final LdapUserDirectoryProperties properties,
      final Duration cacheDuration,
      final Clock clock) {

    super(cacheDuration, clock);
    this.properties = properties;
    this.ldapTemplate = new LdapTemplate(contextSourceOf(properties));
    // Active Directory answers with referrals to other domains, which the cockpit does not follow
    this.ldapTemplate.setIgnorePartialResultException(true);

  }

  private static LdapContextSource contextSourceOf(
      final LdapUserDirectoryProperties properties) {

    final var contextSource = new LdapContextSource();
    contextSource.setUrl(properties.url());
    if (StringUtils.hasText(properties.base())) {
      contextSource.setBase(properties.base());
    }
    if (StringUtils.hasText(properties.bindDn())) {
      contextSource.setUserDn(properties.bindDn());
      contextSource.setPassword(properties.bindPassword());
    } else {
      contextSource.setAnonymousReadOnly(true);
    }
    contextSource.setPooled(true);
    contextSource.afterPropertiesSet();
    return contextSource;

  }

  @Override
  Optional<UserDetails> loadUser(
      final String id) {

    final var users = ask(() -> ldapTemplate.search(
        usersWhere()
            .and(properties.userIdAttribute())
            .is(id),
        new UserMapper()));
    return users
        .stream()
        .findFirst()
        .map(user -> toUserDetails(user, groupsOf(user)));

  }

  private List<String> groupsOf(
      final LdapUser user) {

    final LdapQuery groups = query()
        .base(baseOf(properties.groupSearchBase()))
        .attributes(properties.groupNameAttribute())
        .where(properties.groupMemberAttribute())
        .is(user.dn());
    final AttributesMapper<String> groupName = attributes -> {
      final var name = attributes.get(properties.groupNameAttribute());
      return name == null ? null : String.valueOf(name.get());
    };
    return ask(() -> ldapTemplate.search(groups, groupName))
        .stream()
        .filter(StringUtils::hasText)
        .toList();

  }

  @Override
  List<UserDetails> searchUsers(
      final String query) {

    // a star in the text would be a wildcard of LDAP. Somebody searching types letters
    final var pattern = "*"
        + query.replace("*", "")
        + "*";
    final LdapQuery search = usersWhere()
        .and(query()
            .where(properties.userIdAttribute())
            .like(pattern)
            .or(properties.emailAttribute())
            .like(pattern)
            .or(properties.fullNameAttribute())
            .like(pattern));
    return ask(() -> ldapTemplate.search(search, new UserMapper()))
        .stream()
        .map(user -> toUserDetails(user, List.of()))
        .toList();

  }

  @Override
  List<UserDetails> loadAllUsers() {

    return ask(() -> ldapTemplate.search(usersWhere(), new UserMapper()))
        .stream()
        .map(user -> toUserDetails(user, List.of()))
        .toList();

  }

  private ContainerCriteria usersWhere() {

    return query()
        .base(baseOf(properties.userSearchBase()))
        .where("objectclass")
        .is(properties.userObjectClass());

  }

  private static String baseOf(
      final String searchBase) {

    return StringUtils.hasText(searchBase) ? searchBase : "";

  }

  private UserDetails toUserDetails(
      final LdapUser user,
      final List<String> groups) {

    if (!StringUtils.hasText(user.lastName()) && StringUtils.hasText(user.fullName())) {
      // shown by the full name, as it is, instead of by the id
      return userOf(user.id(), user.email(), null, user.fullName(), groups);
    }
    return userOf(user.id(), user.email(), user.firstName(), user.lastName(), groups);

  }

  private <T> T ask(
      final Supplier<T> search) {

    try {
      return search.get();
    } catch (AuthenticationException e) {
      throw new UserDirectoryFailedException(
          ("The LDAP directory at '%s' refused the login of '%s'. Check "
              + "'business-cockpit.user-directory.ldap.bind-dn' and '.bind-password'.")
              .formatted(properties.url(), properties.bindDn()), e);
    } catch (CommunicationException e) {
      throw new UserDirectoryFailedException(
          "The LDAP directory at '%s' cannot be reached. Check 'business-cockpit.user-directory.ldap.url'."
              .formatted(properties.url()), e);
    } catch (NamingException e) {
      throw new UserDirectoryFailedException(
          "The LDAP directory at '%s' failed to answer a search for users or groups: %s"
              .formatted(properties.url(), e.getMessage()), e);
    }

  }

  private class UserMapper extends AbstractContextMapper<LdapUser> {

    @Override
    protected LdapUser doMapFromContext(
        final DirContextOperations context) {

      return new LdapUser(
          context.getNameInNamespace(), context.getStringAttribute(properties.userIdAttribute()), context
              .getStringAttribute(properties.emailAttribute()), context
                  .getStringAttribute(properties.firstNameAttribute()), context.getStringAttribute(
                      properties.lastNameAttribute()), context.getStringAttribute(properties.fullNameAttribute()));

    }

  }

}
