### Addendum to decision 91: the chains of `container` refuse `/actuator`, and an application which exposes an endpoint writes a chain for it

This entry adds to decision 91. It replaces the first point of its "What follows". The rest of
decision 91 stays as it is.

#### What was found

Decision 91 said that the two security chains of the cockpit do not cover `/actuator`. That was not
true. The GUI chain of `container` has no `securityMatcher`, so it takes every request which no
other chain takes, and it ends with `anyRequest().authenticated()`. So every user who was logged in
reached the actuator endpoints an application exposed, `DELETE /actuator/userdirectorycache` among
them. A management port of its own did not change that. Spring Boot hands the security filter of
the application to the management side as well, and a cookie is not bound to a port.

#### The decision

1. The GUI chain of `container` refuses `/actuator/**` to every user, with `denyAll()` before
   `anyRequest()`. The chain of the derived cockpit in the tests and the chains the skill
   `business-cockpit-creator` writes do the same.
2. An application which exposes an actuator endpoint writes a security chain of its own with
   `securityMatcher("/actuator/**")` and an order below 99, the order of the GUI chain, and lets
   only operations in. This holds for a management port of its own as well.
3. The library brings no chain and does not change. `container` brings no Actuator, so nothing
   changes for it until an application adds Actuator.

#### What follows

- The wiki page Security shows such a chain for operations.
- The rule `BE-005` of the skill names the refusal of `/actuator/**`.

