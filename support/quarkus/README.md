# Support for Quarkus

What a workflow module behind the Business Cockpit needs on Quarkus. It is published as
`io.vanillabp.businesscockpit:business-cockpit-quarkus-support`. It is the twin of
[spring-boot](../spring-boot), and both bring the same [core](../core).

With this module as its only dependency, a Quarkus workflow module compiles. It brings the cockpit's
SPI, `io.vanillabp.businesscockpit:spi-for-java`, and through it the SPI of VanillaBP, as the Spring
Boot module does. The module
[workflow-module-on-quarkus-support-only](../../development/workflow-module-on-quarkus-support-only)
proves it in every build.

What it holds today:

- `UserDetails`, `UserDetailsImpl`, `UserDirectoryService`, `GroupHierarchyResolver` and the
  exceptions, from `core`.
- `io.vanillabp.cockpit.support.usertask.BcUserTaskExceptionMapper` answers `BcUnknownTaskException`
  with `404 Not Found` and `BcTaskNotOpenException` with `409 Conflict`.
- `io.vanillabp.cockpit.support.security.BcUnauthorizedExceptionMapper` answers
  `BcUnauthorizedException` with `401 Unauthorized`.

Each answer is the message of the exception as plain text, so a form can show it. That is the same
answer the Spring Boot module's `BcUserTaskExceptionHandler` gives. On Spring Boot the cockpit's
own handler answers `BcUnauthorizedException`, and a Spring Boot workflow module has no answer for
it yet.

Checking the cockpit's login token, the user of a request and a REST client for the cockpit's APIs
are not here yet. They follow.

## A library, not an extension

The module is a plain jar with a Jandex index (`META-INF/jandex.idx`). Quarkus reads the classes of
a jar with such an index while it builds the application, and Quarkus REST then finds the methods
annotated with `@ServerExceptionMapper`. So an application which has this module and Quarkus REST
gets the answers without a line of its own. On Spring Boot a workflow module has to import the
handler. Nothing here needs a build step, so there is no deployment module.

Quarkus itself is an optional dependency: `quarkus-arc` and `quarkus-rest`. The application brings
Quarkus, and it decides on the version. An application on the older RESTEasy Classic, the extension
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

The test framework builds that application only out of the classes the test names. It leaves out
the main classes of this module, so it cannot show that an application finds the mappers on its
own. `TheMappersAreInTheJandexIndexTest` closes that gap: it reads the Jandex index of the module
and checks that every mapper method is in it.

Their coverage, together with that of `core`, is the report `test-coverage-report/support/quarkus`.
No Spring Boot test counts there.

Built as part of the reactor build described in the [root README](../../README.md#building-it).
Quarkus tests read the modules they use from the local Maven repository, so build them with
`install` and not with `package`.
