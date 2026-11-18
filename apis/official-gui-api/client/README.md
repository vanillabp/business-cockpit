# Clients of the official GUI API

Java clients of the [official GUI API](..), generated from [`openapi/v1.yaml`](../openapi/v1.yaml).
A workflow module uses them to ask the cockpit about a user task, for example whom it is assigned
to, and its tests use them to read what the cockpit knows. Nobody writes the calls by hand.

There is one module per platform. Its README says how an application uses it: the dependency,
where the keys live, two workflow modules with different keys, and the user's cookie.

- [spring-boot](./spring-boot/README.md), published as
  `io.vanillabp.businesscockpit:official-gui-api-spring-boot-client`.
- [quarkus](./quarkus/README.md), published as
  `io.vanillabp.businesscockpit:official-gui-api-quarkus-client`.

On every platform the keys live below a prefix the application or workflow module chooses, never
below `vanillabp`. `base-url` is the address of the cockpit only, never with `/gui/api/v1`: the
clients carry the server path of the OpenAPI file themselves, so moving to a new version of the
API means switching the dependency only. And a call made while the module answers a request of
the cockpit's user interface sends that request's login cookie along, so the cockpit answers as
that user.

The dependency alone asks for no key. On Spring Boot the clients are there only if the address of
the cockpit is set, and on Quarkus a workflow module builds the clients it needs itself. Both jars
come with a sources jar, which holds the generated interfaces and models.

Built as part of the reactor build described in the [root README](../../../README.md#building-it).
