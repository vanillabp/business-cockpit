# The library

The Business Cockpit as a library: the services and the persistence behind the two lists, the GUI
API, the security extension points, the part which takes in what workflow modules report, the
notification feature, the proxy to workflow modules, and the React application under
`src/main/webapp`.

It has no main class and builds no runnable jar. What makes it start is [container](../container),
and that split is explained in [its README](../container/README.md#why-the-library-and-the-application-are-two-modules).
A custom cockpit depends on this module, which the wiki describes under
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).

Anything a custom cockpit would also need belongs here rather than in `container`. Built as part of
the reactor build described in the [root README](../README.md#building-it), and published as
`io.vanillabp.businesscockpit:business-cockpit`.

## How its beans reach an application

Through the auto-configurations in `io.vanillabp.cockpit.autoconfigure`, which Spring Boot reads from
this jar's `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. There
used to be a base class carrying a component scan, and an application got the beans by extending it.

So a stereotype annotation alone does not make a bean here any more. A class added to
`io.vanillabp.cockpit` is named in the `@Import` of the auto-configuration of its area, and
`EveryCockpitBeanIsRegisteredTest` fails when it is not. The package documentation of
`io.vanillabp.cockpit.autoconfigure` says why it is written out rather than scanned, and
`container/README.md` says why the delivered application cannot be the one to prove it works.
