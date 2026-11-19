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
  It reads the `SecurityIdentity` of the request, whatever login made it. So with the check of the
  token switched off it tells the user of the application's own login, and in an application
  without `quarkus-security` nobody is logged in.
- `io.vanillabp.cockpit.support.security.usercontext.GroupResolver`, which turns the groups of the
  directory into all groups of the user, for example with `GroupHierarchyResolver`. The module
  brings one which takes the groups as they are. A bean of the application replaces it.

Each answer is the message of the exception as plain text, so a form can show it. That is the same
answer the Spring Boot module's `BcUserTaskExceptionHandler` gives. On Spring Boot the cockpit's
own handler answers `BcUnauthorizedException`, and a Spring Boot workflow module has no answer for
it yet.

- `io.vanillabp.cockpit.support.rest.restclient.RestClients`, which builds clients of Quarkus' REST
  client from keys at a prefix the workflow module chooses, see below.
- `io.vanillabp.cockpit.support.userinterface.UserInterfaceRoutesOnQuarkus`, which passes the files
  of a workflow module's user interface on to an nginx if the application sets
  `<id>.user-interface.address`. A route of Vert.x, with the keys and checks of the Spring Boot
  module, see [The user interface from an nginx](../README.md#the-user-interface-from-an-nginx).

## The Quarkus extensions it needs

The module brings no Quarkus extension, see below. The workflow module adds the ones it uses:

|                What it uses                |                            Extensions                             |
|--------------------------------------------|-------------------------------------------------------------------|
| the SPI and the exceptions, compile only   | none                                                              |
| the answers to the exceptions              | `quarkus-rest`                                                    |
| the check of the cockpit's login token     | `quarkus-rest`, `quarkus-security`                                |
| REST clients built from keys               | `quarkus-rest-client-jackson`                                     |
| the route to the nginx of a user interface | `quarkus-vertx-http`, and the library `io.vertx:vertx-http-proxy` |

`quarkus-rest` brings `quarkus-vertx-http`, the HTTP layer the check of the token needs as well, so
it does not have to be named. Without `quarkus-security` the application builds and starts, and the
mappers answer. The check of the token is not there then, one line in the log says so, and the user
context knows nobody. If such an application sets the secret anyway, the start ends and says to add
`quarkus-security`. An application with `quarkus-security` which does not read the cockpit's token,
for example because it has a login of its own, sets the build property
`business-cockpit.jwt.enabled=false`.

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
- a secret in an application without `quarkus-security`, which could not check a token with it.

## REST clients

`RestClients` is a bean which builds a client of Quarkus' REST client from the keys at a prefix:

```java
@Produces
@Singleton
@Startup
InventoryApi inventory(
    final RestClients restClients) {
  return restClients.build(InventoryApi.class, "loan-approval.inventory");
}
```

The keys, their defaults, OAuth, two workflow modules with different keys and what differs from
Spring Boot are in the section
[REST clients on Quarkus](../README.md#rest-clients-on-quarkus) of the support README.

Quarkus' REST client is built with `QuarkusRestClientBuilder` and not configured through
`quarkus.rest-client.*`, for two reasons. The keys of a client belong at the prefix of the module
which calls the service. And one interface can be configured there only once, so two modules could
not call the same service with different keys. The builder gets the timeouts, the redirects, TLS and
the proxy from the keys. Filters add the default headers and the OAuth token, and a logger of
Quarkus' REST client writes requests and answers with `log: true`. Retries wrap the client, because
the REST client has none and MicroProfile Fault Tolerance would need annotations on the generated
interface. The OAuth token is fetched by a REST client of its own, `TokenEndpoint`, built with the
same timeouts, TLS settings and proxy. The filter waits for it without blocking, so a token request
never holds the thread which reads its answer. `quarkus-oidc-client` could fetch tokens too, but it
reads its keys below `quarkus.oidc-client`, and a library would switch it on in every application.

## A library, not an extension

The module is a plain jar with a Jandex index (`META-INF/jandex.idx`). Quarkus reads the classes of
a jar with such an index while it builds the application, and Quarkus REST then finds the methods
annotated with `@ServerExceptionMapper`. So an application which has this module and Quarkus REST
gets the answers without a line of its own. On Spring Boot a workflow module has to import the
handler. Nothing here needs a build step, so there is no deployment module.

Quarkus itself is an optional dependency: `quarkus-arc`, `quarkus-rest`, `quarkus-vertx-http`,
`quarkus-security` and `quarkus-rest-client`, and so is `vertx-http-proxy`. The application brings
Quarkus, and it decides on the version and adds the extensions it uses, see the table above. Nothing in the module needs a bean of an extension the
application may leave out: the user context and the start check look the beans of
`quarkus-security` up instead of injecting them. The mechanism and the identity provider are beans
nobody asks for without `quarkus-security`, so Quarkus leaves them out of the application. An application on the older RESTEasy Classic, the extension
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

[quarkus-it-without-security](../quarkus-it-without-security) runs one without
`quarkus-security`, and `AModuleWithoutTheCheckTest` of `quarkus-it` one with the check switched
off.

`TheUserInterfaceComesFromTheNginxTest` starts an application with the route and a stub which
plays the nginx, the twin of the Spring Boot test of the same name, and
`AWrongRouteKeyEndsTheStartTest` one with a wrong address. `AModuleWithItsUserInterfaceInAnNginxTest`
of `quarkus-it` shows that Quarkus finds the route in the plain jar.

The REST client tests start such an application too, with the client the generator `jaxrs-spec`
writes from a small inventory API and a stub server: `RequestsTest`, `TwoModulesTest`,
`RetryTest`, `OAuthTest`, `ProxyTest`, `TlsTest`, `LoggingTest`, `StartupChecksTest` and
`AMissingAddressStopsTheStartTest`. They are the twins of the tests of the HTTP service clients
of the Spring Boot module. The test framework loads a test class twice, so the port of a stub is
chosen once and kept in a system property, and the copy which runs the tests starts the stub.
`AClientOfAWorkflowModuleTest` of `quarkus-it` builds a client with OAuth from the plain jar.

Their coverage, together with that of `core`, `quarkus-it` and `quarkus-it-without-security`, is
the report `test-coverage-report/support/quarkus`, which also counts the Quarkus client of the
official GUI API. No Spring Boot test counts there.

Built as part of the reactor build described in the [root README](../../README.md#building-it).
Quarkus tests read the modules they use from the local Maven repository, so build them with
`install` and not with `package`.
