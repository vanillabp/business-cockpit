# Support for Spring Boot

What the Business Cockpit and a workflow module behind it share, on Spring Boot. A workflow module
depends on this module, and so does the cockpit. It is published as
`io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`. Below, `support` means this
module.

With `support` as its only dependency, a workflow module compiles. `support` brings the cockpit's
SPI, `io.vanillabp.businesscockpit:spi-for-java`, with the annotations and types for the details of
user tasks and workflows, and through it the SPI of VanillaBP. To run, the workflow module needs
the cockpit's adapter for its BPMS, `business-cockpit-*-adapter`. Its tests take the adapter in test
scope. The module [workflow-module-on-support-only](../../development/workflow-module-on-support-only)
compiles such a workflow module in every build, with `support` as its only dependency.

The classes which know no platform live in [core](../core), the artifact
`business-cockpit-support-core`, and this module brings it. They kept their packages, so the lists
below name them where they belong, whichever module holds them.

- `io.vanillabp.spi.cockpit`, from `spi-for-java`: the annotations and types a workflow module
  uses to tell the cockpit about its user tasks and workflows.

- `io.vanillabp.cockpit.support.security.jwt`: reads and writes the cockpit's login token (a JSON
  Web Token, JWT) in its cookie, and turns it into a Spring Security authentication. The key which
  signs and checks the token comes from a `JwtKeySource`. Today that is the shared HMAC secret of
  the settings. Behind these classes, `HmacJwtKeySource` checks a token with `LoginTokenVerifier`
  of `core`, `JwtAuthenticationTokenMapper` finds the user and the groups with
  `DirectoryUserLookup`, and `JwtUserDetails` reads the name with `TokenUserDetails`. A Quarkus
  workflow module uses the same three, so both accept the same tokens.
  `ATokenOfTheCockpitIsAcceptedByTheCoreVerifierTest` checks it in both directions.

- `io.vanillabp.cockpit.support.security.usercontext`: the user of the current request, the user's
  groups, and how a group hierarchy adds groups to the ones assigned. `UserDetails`,
  `UserDetailsImpl`, `UserDirectoryService` and `GroupHierarchyResolver` come from `core`, the
  rest is Spring Security. `UserDirectoryService` is the
  directory of all users, which the application provides. `UserDetailsImpl` is a plain
  implementation of `UserDetails`, so neither the cockpit nor a workflow module has to write one.
  A workflow module which reads the login token needs a `UserDirectoryService` of its own, because
  the token carries no groups. Where a bean needs one and the application has none,
  `UserDirectoryIsMissingFailureAnalyzer` says so on start, with the bean to add and why it should
  cache. It is registered in `META-INF/spring.factories`.

- `io.vanillabp.cockpit.support.usertask`: the exceptions for a user task a request names, from
  `core`.
  `BcUnknownTaskException` says that the workflow has no user task of that id and kind.
  `BcTaskNotOpenException` says that the task was completed or canceled already, for example
  because a form was sent twice. `BcTaskClaimedByAnotherUserException` says that the task is open,
  but another user claimed it. `BcUserTaskExceptionHandler` answers the first two with
  `404 Not Found` and the third with `409 Conflict`, with the message as plain text, so a form can
  show it. Nothing registers the
  handler on its own. A workflow module imports it with `@Import(BcUserTaskExceptionHandler.class)`.
  The exceptions carry no `@ResponseStatus`, because Spring Boot would then answer with its error
  page and leave out the message.

- `io.vanillabp.cockpit.support.rest.httpservice`: configures every HTTP service group of the
  application, the clients Spring Framework builds from generated `@HttpExchange` interfaces. The
  keys of a group live below a prefix the client module or the application chooses. Next to Spring
  Boot's keys a group gets a proxy, `verify-ssl`, retries, logging and OAuth, and the start stops
  with a list of every missing or misspelled key. The requests go through the JDK's own
  `HttpClient`, so the module brings no HTTP library.

  The section [REST clients](../README.md#rest-clients) of the support README describes it, lists
  every key with its default and shows, step by step, how a client module brings its own
  auto-configuration.

- `io.vanillabp.cockpit.support.userinterface`: the route which passes the files of a workflow
  module's user interface on to an nginx if the application sets `<id>.user-interface.address`.
  `UserInterfaceRouteAutoConfiguration` registers one route of Spring Cloud Gateway per such module,
  as a bean named `<id>-user-interface`, and the application adds
  `spring-cloud-starter-gateway-server-webmvc`. A wrong key ends the start, and
  `UserInterfaceRoutesMisconfiguredFailureAnalyzer` prints why. The section
  [The user interface from an nginx](../README.md#the-user-interface-from-an-nginx) of the support
  README describes it. Its tests run in a Surefire execution of their own with the gateway, because
  the gateway changes a default of every HTTP client; the other tests run without it.

The module brings no MongoDB and no Kafka. Spring Web, Spring Security and Spring's JWT classes are
optional dependencies: an application which checks the login token has them anyway, and one which
only calls a REST API does not need them. Spring Security's OAuth client is optional as well: only
an HTTP service group with OAuth needs it. So is Spring Cloud Gateway: only an application with a
route to an nginx needs it. The MongoDB, Kafka and error handling helpers which only
the cockpit uses live in the library module [business-cockpit](../../business-cockpit).

The Quarkus version is the sibling module [support/quarkus](../quarkus). Both bring the same
`core`. The packages are `io.vanillabp.cockpit.support...` in all three.

Built as part of the reactor build described in the [root README](../../README.md#building-it), and
published as `io.vanillabp.businesscockpit:business-cockpit-spring-boot-support` to Maven Central, snapshots to its snapshot
repository.
