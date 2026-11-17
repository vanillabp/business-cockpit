# Support core

The part of `support` which knows no platform. It is published as
`io.vanillabp.businesscockpit:business-cockpit-support-core`. Nobody names it as a dependency:
[spring-boot](../spring-boot) and [quarkus](../quarkus) both bring it, and a workflow module
depends on the module of its platform.

- `io.vanillabp.cockpit.support.security`: `BcUnauthorizedException`, which says that nobody is
  logged in or that the login is not valid.
- `io.vanillabp.cockpit.support.security.usercontext`: `UserDetails`, the user as the cockpit knows
  it, and `UserDetailsImpl`, a plain implementation of it. `UserDirectoryService`, the directory of
  all users, which the application provides. `GroupHierarchyResolver`, which adds the groups a
  group hierarchy implies to the groups a user is assigned to.
- `io.vanillabp.cockpit.support.usertask`: `BcUnknownTaskException` and `BcTaskNotOpenException`,
  which a form endpoint throws for a task id the workflow does not know and for a task which is not
  open any more.
- `io.vanillabp.cockpit.support.rest.adapter`: `tls.TlsTruststoreUtil`, which builds trust managers
  and key managers out of a PKCS12 file, and `versioning.ApiVersionAware`.

The packages are the same as in the platform modules. The classes moved here out of the Spring Boot
module and kept their packages, so no import changed.

The module has no dependency in compile scope, and it should stay that way. Whatever it brings, a
workflow module gets on both platforms. A class which needs Spring or Quarkus belongs into the
platform module. The answers to the exceptions are there too, because how a status code is sent
depends on the platform.

Its tests are plain JUnit tests. Their coverage counts in the report of the container, which
carries the Spring Boot module, and in the report of the Quarkus support. See
[test-coverage-report](../../test-coverage-report).

Built as part of the reactor build described in the [root README](../../README.md#building-it).
