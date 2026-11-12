# Support

What the Business Cockpit and a workflow module behind it share. A workflow module depends on this
module, and so does the cockpit.

With `support` as its only dependency, a workflow module compiles. `support` brings the cockpit's
SPI, `io.vanillabp.businesscockpit:spi-for-java`, with the annotations and types for the details of
user tasks and workflows, and through it the SPI of VanillaBP. To run, the workflow module needs
the cockpit's adapter for its BPMS, `business-cockpit-*-adapter`. Its tests take the adapter in test
scope. The module [workflow-module-on-support-only](../development/workflow-module-on-support-only)
compiles such a workflow module in every build, with `support` as its only dependency.

- `io.vanillabp.spi.cockpit`, from `spi-for-java`: the annotations and types a workflow module
  uses to tell the cockpit about its user tasks and workflows.

- `io.vanillabp.cockpit.support.security.jwt`: reads and writes the cockpit's login token (a JSON
  Web Token, JWT) in its cookie, and turns it into a Spring Security authentication. The key which
  signs and checks the token comes from a `JwtKeySource`. Today that is the shared HMAC secret of
  the settings.

- `io.vanillabp.cockpit.support.security.usercontext`: the user of the current request, the user's
  groups, and how a group hierarchy adds groups to the ones assigned. `UserDirectoryService` is the
  directory of all users, which the application provides. `UserDetailsImpl` is a plain
  implementation of `UserDetails`, so neither the cockpit nor a workflow module has to write one.

- `io.vanillabp.cockpit.support.rest`: a REST client built on OpenFeign and OkHttp, with retries,
  a proxy, bearer and OAuth authentication and a trust store of its own. The cockpit's adapters
  and an application use it to call the API of a BPMS or of the cockpit.

The module brings no MongoDB and no Kafka. Spring Web, Spring Security and Spring's JWT classes are
optional dependencies: an application which checks the login token has them anyway, and one which
only calls a REST API does not need them. The MongoDB, Kafka and error handling helpers which only
the cockpit uses live in the library module [business-cockpit](../business-cockpit).

There is no Quarkus version yet. The classes of the REST client know Spring only in
`ClientsConfigurationBase`, so a Quarkus version can come later without changing the others.

Built as part of the reactor build described in the [root README](../README.md#building-it), and
published as `io.vanillabp.businesscockpit:support` to Maven Central, snapshots to its snapshot
repository.
