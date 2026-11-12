package io.vanillabp.cockpit.users.directory;

import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How the cockpit reaches an LDAP directory and where it finds users and groups there, read from
 * {@code business-cockpit.user-directory.ldap.*}.
 * <p>
 * The defaults fit a directory with users of the object class {@code inetOrgPerson} and groups which
 * name their members by DN in the attribute {@code member}, like {@code groupOfNames}. That is the
 * usual layout of OpenLDAP. For Active Directory set {@code user-object-class} to {@code user} and
 * {@code user-id-attribute} to {@code sAMAccountName}.
 *
 * @param url The address of the directory, for example {@code ldaps://ldap.example.com:636}
 * @param base The DN every search starts below, for example {@code dc=example,dc=com}
 * @param bindDn The DN the cockpit logs in with. Without it the cockpit reads anonymously
 * @param bindPassword The password of that DN
 * @param userSearchBase Where the users are, relative to {@code base}, for example
 *          {@code ou=people}. Empty means {@code base} itself
 * @param userObjectClass The object class every user has
 * @param userIdAttribute The attribute which holds the id of a user. Its value is the name the
 *          login names the user by
 * @param emailAttribute The attribute which holds the e-mail address of a user
 * @param firstNameAttribute The attribute which holds the first name of a user
 * @param lastNameAttribute The attribute which holds the last name of a user
 * @param fullNameAttribute The attribute which holds the full name of a user. A search looks at it,
 *          next to the id and the e-mail address. A user without a last name is shown by it
 * @param groupSearchBase Where the groups are, relative to {@code base}, for example
 *          {@code ou=groups}. Empty means {@code base} itself
 * @param groupMemberAttribute The attribute of a group which holds the DN of each member
 * @param groupNameAttribute The attribute which holds the name of a group. The name is the group
 *          the cockpit sees
 */
public record LdapUserDirectoryProperties(
                                          String url,
                                          String base,
                                          String bindDn,
                                          String bindPassword,
                                          String userSearchBase,
                                          @DefaultValue("inetOrgPerson") String userObjectClass,
                                          @DefaultValue("uid") String userIdAttribute,
                                          @DefaultValue("mail") String emailAttribute,
                                          @DefaultValue("givenName") String firstNameAttribute,
                                          @DefaultValue("sn") String lastNameAttribute,
                                          @DefaultValue("cn") String fullNameAttribute,
                                          String groupSearchBase,
                                          @DefaultValue("member") String groupMemberAttribute,
                                          @DefaultValue("cn") String groupNameAttribute) {
}
