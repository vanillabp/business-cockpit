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
below `vanillabp`, and a call made while the module answers a request of the cockpit's user
interface sends that request's login cookie along, so the cockpit answers as that user.

Built as part of the reactor build described in the [root README](../../../README.md#building-it).
