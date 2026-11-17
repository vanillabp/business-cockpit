# Support core

The part of `support` which knows no platform. It is published as
`io.vanillabp.businesscockpit:business-cockpit-support-core`. Nobody names it as a dependency:
[spring-boot](../spring-boot) and [quarkus](../quarkus) both bring it, and a workflow module
depends on the module of its platform.

- `io.vanillabp.cockpit.support.security`: `BcUnauthorizedException`, which says that nobody is
  logged in or that the login is not valid.
- `io.vanillabp.cockpit.support.security.jwt`: `LoginTokenVerifier` checks the cockpit's login
  token: signed with HMAC SHA-256 and the shared secret, not expired and valid already, with a
  minute of tolerance for clocks which differ. It gives a `LoginToken` or throws
  `InvalidLoginTokenException`. `TokenUserDetails` tells the name and the e-mail address of the
  user, from the directory or else from the claims of the token.
- `io.vanillabp.cockpit.support.security.usercontext`: `UserDetails`, the user as the cockpit knows
  it, and `UserDetailsImpl`, a plain implementation of it. `UserDirectoryService`, the directory of
  all users, which the application provides. `GroupHierarchyResolver`, which adds the groups a
  group hierarchy implies to the groups a user is assigned to. `DirectoryUserLookup` asks the
  directory for the user of a token on every request and gives a `LoggedInUser` with the authority
  `USER_<id>` and the groups. A user the directory does not know keeps its login without groups and
  is warned about once a minute at most. A directory which fails fails the request.
  `UserDirectoryIsMissing` holds the words both platforms say when the application has no
  directory.
- `io.vanillabp.cockpit.support.usertask`: `BcUnknownTaskException` and `BcTaskNotOpenException`,
  which a form endpoint throws for a task id the workflow does not know and for a task which is not
  open any more.
- `io.vanillabp.cockpit.support.rest.adapter`: `tls.TlsTruststoreUtil`, which builds trust managers
  and key managers out of a PKCS12 file, and `versioning.ApiVersionAware`.

The packages are the same as in the platform modules. The classes moved here out of the Spring Boot
module and kept their packages, so no import changed.

The module has two dependencies in compile scope, and it should not get more. Whatever it brings,
a workflow module gets on both platforms. Nimbus (`com.nimbusds:nimbus-jose-jwt`) checks the
signature of the token. Spring Security uses the same library under its JWT classes, and the
version is the one it reads. The SLF4J API is the logging API both platforms route to their own
logging. A class which needs Spring or Quarkus belongs into the platform module. The answers to the
exceptions are there too, because how a status code is sent depends on the platform. So are the
reading of the cookie and the settings.

The Spring Boot module checks a token with `LoginTokenVerifier`, finds the user with
`DirectoryUserLookup` and reads the name with `TokenUserDetails`, behind its own classes, so its
public classes did not change. The Quarkus module uses the same three.

The test jar of the module, `business-cockpit-support-core` with the type `test-jar`, holds what
the tests of both platform modules share: `LoginTokens`, which signs a token the way the cockpit
does, and `ChangeableUserDirectory`, a user directory a test changes while it runs.

Its tests are plain JUnit tests. Their coverage counts in the report of the container, which
carries the Spring Boot module, and in the report of the Quarkus support. See
[test-coverage-report](../../test-coverage-report).

Built as part of the reactor build described in the [root README](../../README.md#building-it).
