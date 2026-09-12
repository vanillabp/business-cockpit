[![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](../LICENSE)

![VanillaBP](../readme/vanillabp-headline.png)

# The microservice

The runnable Business Cockpit: the module which builds the executable jar, from the library
`business-cockpit` next to it plus what makes it start.

Running it, configuring it, securing it and deriving an application from it are described in the
[wiki](https://github.com/vanillabp/business-cockpit/wiki), under
[Running the Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Running-the-Business-Cockpit),
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security) and
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).
This file is about the module.

**Contents:**

1. [What is here](#what-is-here)
1. [Why the library and the application are two modules](#why-the-library-and-the-application-are-two-modules)
1. [Running it from a build](#running-it-from-a-build)
1. [Why it is not reactive](#why-it-is-not-reactive)

## What is here

Four things, and each of them is the reference answer to something a custom cockpit has to write for
itself:

* `BusinessCockpitStandaloneApplication`, which extends `BusinessCockpitApplication` and adds a
  `main` method and nothing else.
* The `application*.yaml` files below `config/`, the defaults of a standalone deployment, including
  the profile `local` with its demo users and its MongoDB URI.
* The five concrete GUI API controllers below `io/vanillabp/cockpit`, which implement the three
  abstract controllers of the library and decide which tasks, workflows and modules a user gets.
  The rule they apply is described in the wiki under
  [Who sees which workflow](https://github.com/vanillabp/business-cockpit/wiki/Security#who-sees-which-workflow).
* The local user directory below `users/local`, which answers who a user is without an identity
  provider.

## Why the library and the application are two modules

`business-cockpit` holds the functionality and builds no runnable jar. `container` holds what makes
it start and is published so that the cockpit can be run as it is.

They used to be one, and that one jar was both at once: somebody adding it as a dependency to get
the functionality inherited a second application's main class and its configuration files along with
it. The split is what keeps a custom cockpit from doing that.

Anything added here which a custom cockpit would also need belongs in the library instead. The test
for it is whether an application depending only on `business-cockpit` would miss it.

## Running it from a build

```sh
java -Dspring.profiles.active=local -jar target/container-*-runnable.jar
```

It wants the MongoDB of the compose file in [development](../development), and
[development/README.md](../development/README.md) is what starts that. The user interface is at
[http://localhost:8080/](http://localhost:8080/), with `test` and `test` as the login.

Backend changes usually belong in `business-cockpit`; the user interface always does. Only the four
things listed above are changed here.

## Why it is not reactive

The backend is written in blocking style on Spring MVC, and every request runs on a virtual thread.

It used to be reactive. Reactive code never blocks a thread, and the price is a programming model in
which a database read is a `Mono` and a stack trace hardly says where you are. Virtual threads make
that trade pointless: a thread waiting for MongoDB or for a workflow module behind the proxy costs
almost nothing, so the code can be plain Java without giving up concurrency.

Two parts looked like arguments for staying reactive, and neither turned out to be one.
Server-sent events are `SseEmitter` instances the scheduled collector writes to. The workflow module
proxy is `spring-cloud-gateway-server-webmvc`, which copies the exchange chunk by chunk with a
blocking `RestClient` rather than buffering it. Both cost one virtual thread per subscriber and per
proxied request.

Workflow modules are separate applications and pick their own web stack, so none of this applies to
them.
