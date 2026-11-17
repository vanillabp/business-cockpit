# Spring Boot client of the official GUI API

The HTTP interface clients of the official GUI API for a Spring Boot application, published as
`io.vanillabp.businesscockpit:official-gui-api-spring-boot-client`. The interfaces and models are
generated from [`openapi/v1.yaml`](../../openapi/v1.yaml) by the generator `spring` with the library
`spring-http-interface`. `OfficialGuiApiClientAutoConfiguration` imports them into the HTTP service
group `official-gui-api`, and the HTTP service helper of the support module builds them from keys.
`ForwardTheUsersCookie` sends the cookie of the user's request along.

The [README one level up](../README.md#spring-boot) shows how an application uses it: the
dependency, where the keys live, two workflow modules with different keys, and the cookie.

Its tests start the client against a stub server. Its coverage counts in no aggregated report,
because it runs in a workflow module and in no cockpit. `CoverageGateTest` names it among the
modules which are left out on purpose.

Built as part of the reactor build described in the [root README](../../../../README.md#building-it).
