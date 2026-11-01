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
2. [Why the library and the application are two modules](#why-the-library-and-the-application-are-two-modules)
3. [Why its own tests cannot prove the wiring](#why-its-own-tests-cannot-prove-the-wiring)
4. [Running it from a build](#running-it-from-a-build)
5. [Why it is not reactive](#why-it-is-not-reactive)
6. [A suspended task or case](#a-suspended-task-or-case)

## What is here

Five things, and each of them is the reference answer to something a custom cockpit has to write for
itself:

* `BusinessCockpitStandaloneApplication`, a `@SpringBootApplication` with a `main` method and nothing
  else. It says nothing about the cockpit, because the dependency on `business-cockpit` brings the
  auto-configurations of `io.vanillabp.cockpit.autoconfigure` along. A custom cockpit writes the same
  class.
* The `application*.yaml` files below `config/`, the defaults of a standalone deployment, including
  the profile `local` with its demo users and its MongoDB URI. They also carry
  `mongodb.write-concern: majority`, which a custom cockpit writes too.
  [The write concern](https://github.com/vanillabp/business-cockpit/wiki/Running-the-Business-Cockpit#the-write-concern)
  in the wiki says why, and what another value would cost.
  They set two values the library has none for: `application.spa-default-file`, the shell a deep
  link is answered with, and `business-cockpit.default-locale: en`, the language of a mail to a
  user who has none of their own.
* The five concrete GUI API controllers below `io/vanillabp/cockpit`, which implement the three
  abstract controllers of the library and decide which tasks, workflows and modules a user gets.
  The rule they apply is described in the wiki under
  [Who sees which workflow](https://github.com/vanillabp/business-cockpit/wiki/Security#who-sees-which-workflow).
* The local user directory below `users/local`, which answers who a user is without an identity
  provider.
* The two security chains below `security`. `guiHttpSecurity` logs a person in by HTTP basic, with
  the password `test` for every user of the directory, and hands out the JWT cookie.
  `bpmsApiHttpSecurity` lets the BPMS adapters in with `bpms-api.username` and `bpms-api.password`.
  The same package has the bean `userDetailsProvider`, which reads the user from the cookie. The
  library brings none of the three, so that no example login is active in an application built on
  it. [Security](https://github.com/vanillabp/business-cockpit/wiki/Security) in the wiki says what
  every chain has to keep.

## Why the library and the application are two modules

`business-cockpit` holds the functionality and builds no runnable jar. `container` holds what makes
it start and is published so that the cockpit can be run as it is.

They used to be one, and that one jar was both at once: somebody adding it as a dependency to get
the functionality inherited a second application's main class and its configuration files along with
it. The split is what keeps a custom cockpit from doing that.

Anything added here which a custom cockpit would also need belongs in the library instead. The test
for it is whether an application depending only on `business-cockpit` would miss it.
The login is the exception. Every application needs one, but each one picks its own, so the library
holds only what every login shares: the JWT cookie and the paths which stay open.

## Why its own tests cannot prove the wiring

This module lives in `io.vanillabp.cockpit`, the library's own package. So the component scan of its
`@SpringBootApplication` reaches the library's classes, and the package Spring Boot looks in for
Spring Data repositories is that package as well. The application therefore starts whether the
library's auto-configurations do their job or not, and every test in `io.vanillabp.cockpit.itest`
starts it that way.

`io.vanillabp.derived.cockpit.ApplicationWithoutTheBaseClassTest` is the one which does not. It boots
an application in a package of its own, so nothing of the library is in reach of its scan, and that
is the application a custom cockpit really writes. Wiring work belongs in front of that test, not in
front of the suite above it.

## Running it from a build

```sh
java -Dspring.profiles.active=local -jar target/container-*-runnable.jar
```

It wants the MongoDB of the compose file in [development](../development), and
[development/README.md](../development/README.md) is what starts that. The APIs answer at
[http://localhost:8080/](http://localhost:8080/), with `test` and `test` as the login. The jar
carries no user interface, see decision 63 in the repository's [DECISIONS.md](../DECISIONS.md), so
a path no API knows gets 404.

Backend changes usually belong in `business-cockpit`. Only the five things listed above are changed
here.

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

## A suspended task or case

A workflow module reports that a process instance was suspended or activated again, by REST or by
Kafka. The cockpit keeps such a task or case in every list and marks it with `suspendedAt` in the
GUI API, until an activation, a younger change or an end removes the mark. The
[library's README](../business-cockpit/README.md#a-suspended-task-or-case) has the rules, and
[the BPMS API's README](../apis/bpms-api/README.md) the reports. `BpmsApiUserTaskLifecycleTest` and
`BpmsApiWorkflowLifecycleTest` send the reports over HTTP and read the result through the GUI API,
the way a user interface sees it.

