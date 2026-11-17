package io.vanillabp.cockpit.support.security.usercontext;

/**
 * The words which explain a missing {@link UserDirectoryService}. Both platforms end the start of an
 * application without one with this text, each with the code its developers write.
 */
public final class UserDirectoryIsMissing {

  /**
   * Where the wiki explains the directory.
   */
  public static final String WIKI_SECURITY = "https://github.com/vanillabp/business-cockpit/wiki/Security";

  private UserDirectoryIsMissing() {
  }

  /**
   * What is wrong.
   *
   * @param whoNeedsIt A sentence which names what needs the directory
   * @return The description of the failure
   */
  public static String whatIsWrong(
      final String whoNeedsIt) {

    return "The application has no bean of type '"
        + UserDirectoryService.class.getName()
        + "'. "
        + whoNeedsIt
        + " The login token of the Business Cockpit names the user but carries no groups, so "
        + "whatever reads the token asks this directory for the groups of the user, on every request.";

  }

  /**
   * What to do about it.
   *
   * @param example The code which adds such a bean on the platform of the application
   * @return The action
   */
  public static String whatToDo(
      final String example) {

    final var whatToAdd = """
        Add a bean of type 'UserDirectoryService' which asks the system your users are kept in, \
        for example Keycloak or LDAP:
        """;
    final var whyItCaches = """
        It is asked on every request, so let it cache the users if it asks a remote system. \
        A workflow module which reads the token of the cockpit needs a directory of its own, \
        because the directory of the cockpit is not shared with it. The wiki says more: \
        """;
    return whatToAdd
        + "\n"
        + example.indent(4)
        + "\n"
        + whyItCaches
        + WIKI_SECURITY;

  }

}
