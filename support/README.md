# Support

What the Business Cockpit and a workflow module behind it share. A workflow module which serves its
own user interface behind the cockpit's proxy depends on this module, and so does the cockpit.

- `io.vanillabp.cockpit.support.security.jwt`: reads and writes the cockpit's login token (a JSON
  Web Token, JWT) in its cookie, and turns it into a Spring Security authentication. The key which
  signs and checks the token comes from a `JwtKeySource`. Today that is the shared HMAC secret of
  the settings.
- `io.vanillabp.cockpit.support.security.usercontext`: the user of the current request, the user's
  groups, and how a group hierarchy adds groups to the ones assigned.
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
