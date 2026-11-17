# Support for Quarkus

What a workflow module behind the Business Cockpit needs on Quarkus. It is published as
`io.vanillabp.businesscockpit:business-cockpit-quarkus-support`. It is the twin of
[spring-boot](../spring-boot), and both bring the same [core](../core).

With this module as its only dependency, a Quarkus workflow module compiles. It brings the cockpit's
SPI, `io.vanillabp.businesscockpit:spi-for-java`, and through it the SPI of VanillaBP, as the Spring
Boot module does. The module
[workflow-module-on-quarkus-support-only](../../development/workflow-module-on-quarkus-support-only)
proves it in every build.

What it holds:

- `UserDetails`, `UserDetailsImpl`, `UserDirectoryService`, `GroupHierarchyResolver` and the
  exceptions, from `core`.
- `io.vanillabp.cockpit.support.usertask.BcUserTaskExceptionMapper` answers `BcUnknownTaskException`
  and `BcTaskNotOpenException` with `404 Not Found` and `BcTaskClaimedByAnotherUserException` with
  `409 Conflict`, with the message as plain text.
- `io.vanillabp.cockpit.support.security.BcUnauthorizedExceptionMapper` answers
  `BcUnauthorizedException` with `401 Unauthorized`.
- The check of the cockpit's login token, in `io.vanillabp.cockpit.support.security.jwt`, see
  below.
- `io.vanillabp.cockpit.support.security.usercontext.UserContext`, the user of the current request.
  It has the same name and methods as the class of the Spring Boot module, so code which uses it
  moves between the platforms unchanged. A request without a user ends in `BcUnauthorizedException`.
- `io.vanillabp.cockpit.support.security.usercontext.GroupResolver`, which turns the groups of the
  directory into all groups of the user, for example with `GroupHierarchyResolver`. The module
  brings one which takes the groups as they are. A bean of the application replaces it.

Each answer is the message of the exception as plain text, so a form can show it. That is the same
answer the Spring Boot module's `BcUserTaskExceptionHandler` gives. On Spring Boot the cockpit's
own handler answers `BcUnauthorizedException`, and a Spring Boot workflow module has no answer for
it yet.

The REST client for the cockpit's APIs is not here yet. It follows.

## The login token

The browser sends the cockpit's cookie with every request to a workflow module behind it.
`CockpitCookieAuthenticationMechanism` reads the token from the cookie, and
`LoginTokenIdentityProvider` checks it with `LoginTokenVerifier` of `core` and asks the
application's `UserDirectoryService` for the user with `DirectoryUserLookup` of `core`. The groups
go through the `GroupResolver` and become the roles of the request, together with `USER_<id>`. So
`@RolesAllowed("CLERK")` means what it means in the cockpit. The directory may ask a remote system,
so it is asked off the I/O thread. The identity carries the user as `UserDetails` in its attribute
`io.vanillabp.cockpit.user-details` and the checked token in `io.vanillabp.cockpit.login-token`.

The mechanism is passive, like `PassiveJwtSecurityFilter` on Spring Boot. A request without the
cookie, or with a token which is not valid, has no user and goes on. An endpoint which needs a user
answers `401`, one which needs a role the user lacks answers `403`. It sends no challenge, because
the login happens in the cockpit. A directory which fails fails the request with `500`, because
going on without groups would show the user less than it may see. The module brings its own
mechanism instead of `quarkus-smallrye-jwt`: that extension reads other keys, answers `401` on its
own, and the groups come from the directory and not from the token anyway.

The settings are those of the cockpit:

|                   Key                    | Default |                                                      Meaning                                                      |
|------------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------|
| `business-cockpit.jwt.hmacSHA256-base64` | none    | The secret the cockpit signs its tokens with, Base64 encoded. Required.                                           |
| `business-cockpit.jwt.cookie.name`       | `bc`    | The name of the cookie which carries the token.                                                                   |
| `business-cockpit.jwt.enabled`           | `true`  | A build property. `false` leaves the mechanism, the identity provider and the check below out of the application. |

`JwtSettings` reads them one by one. A `@ConfigMapping` would make Quarkus refuse every other key
below `business-cockpit.jwt` while it reads the configuration, with a message which does not say
what was meant. `JwtStartupCheck` ends the start at `StartupEvent` instead, with a message which
says what to do:

- a key below `business-cockpit.` which nobody reads, with the key that was probably meant;
- a key which says how the cookie is written, like `business-cockpit.jwt.cookie.secure`,
  `same-site`, `domain`, `path`, `expires-duration` and `max-login-duration`: only the cockpit
  writes the cookie;
- no secret, with the text of the Spring Boot module and a newly generated secret, or a secret
  which is no Base64 or shorter than 256 bits;
- no bean of type `UserDirectoryService`, with the text of the Spring Boot module's failure
  analyzer and the Quarkus code to add.

## A library, not an extension

The module is a plain jar with a Jandex index (`META-INF/jandex.idx`). Quarkus reads the classes of
a jar with such an index while it builds the application, and Quarkus REST then finds the methods
annotated with `@ServerExceptionMapper`. So an application which has this module and Quarkus REST
gets the answers without a line of its own. On Spring Boot a workflow module has to import the
handler. Nothing here needs a build step, so there is no deployment module.

Quarkus itself is an optional dependency: `quarkus-arc`, `quarkus-rest`, `quarkus-vertx-http` and
`quarkus-security`. The application brings Quarkus, and it decides on the version and adds the
extensions it uses. An application on the older RESTEasy Classic, the extension
`quarkus-resteasy`, does not get the mappers, because `@ServerExceptionMapper` belongs to Quarkus
REST.

The version of Quarkus this module is built and tested with is the property `version.quarkus` of
the root `pom.xml`, the same one the Quarkus half of the extension uses.
`business-cockpit-bom` does not fit a Quarkus application, because it imports the Spring Boot BOM.
So a Quarkus application names the version of this module itself.

## Tests

`BcUserTaskExceptionMapperTest` starts a Quarkus application in the test's JVM, with a form
endpoint and the two mappers, and checks that each exception gets its status and its message. It is
the twin of `BcUserTaskExceptionHandlerTest` of the Spring Boot module.

The tests of the login token start such an application too, with a form endpoint, a directory of
the test and tokens which `LoginTokens` of the core's test jar signs:
`TheCookieLogsTheUserInTest`, `ARequestWithoutAValidTokenIsAnonymousTest`, `UserContextTest`,
`GroupsComeFromTheDirectoryTest`, the twin of the Spring Boot test of the same name, and
`TheCheckCanBeSwitchedOffTest`. The start checks have one test each: `AMissingKeyIsExplainedTest`,
`ASecretWhichIsTooShortIsExplainedTest`, `AMissingDirectoryIsExplainedTest`,
`AMisspelledJwtKeyIsRefusedTest` and `AWriteOnlyCookieKeyIsRefusedTest`.

The test framework builds that application only out of the classes the test names. It leaves out
the main classes of this module, so `TheSupportOnQuarkus` names them, and the tests cannot show
that an application finds them on its own. Two things close that gap.
`TheMappersAreInTheJandexIndexTest` reads the Jandex index of the module and checks that every
mapper method is in it. And [quarkus-it](../quarkus-it) runs an application which has this module
as a plain dependency.

Their coverage, together with that of `core` and `quarkus-it`, is the report
`test-coverage-report/support/quarkus`. No Spring Boot test counts there.

Built as part of the reactor build described in the [root README](../../README.md#building-it).
Quarkus tests read the modules they use from the local Maven repository, so build them with
`install` and not with `package`.
