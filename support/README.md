# Support

What the Business Cockpit and a workflow module behind it share. A workflow module depends on one
of the modules below, and so does the cockpit. With it as its only dependency, a workflow module
compiles.

There is one module per platform, because what a workflow module needs to check a login or to answer
a request depends on the platform it runs on:

- [spring-boot](./spring-boot), published as
  `io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`. Its README says what it holds.
- `quarkus` does not exist yet. It will be added next to `spring-boot`.

This directory itself is only the parent POM, `support-parent`, which lists the modules. Nothing
builds against it.
