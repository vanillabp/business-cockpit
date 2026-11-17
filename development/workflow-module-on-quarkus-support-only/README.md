![](../../readme/vanillabp-headline.png)

# A workflow module on the Quarkus support only

The Quarkus twin of [workflow-module-on-support-only](../workflow-module-on-support-only). A small
workflow module which the build compiles and never runs. Its only dependency is the Quarkus module
of [support](../../support/quarkus), the artifact `business-cockpit-quarkus-support`. It has the
same workflow service with the annotations of VanillaBP and of the cockpit's SPI, the same user
class of its own which implements `UserDetails`, and a form check which throws the two exceptions
of a user task. If a change to `support` takes away something a Quarkus workflow module needs to
compile, this module stops the build.

So keep the Quarkus module of `support` its only dependency, and add no test scope dependency
either. Any second dependency could bring the class which `support` lost, and the build would stay
green.

The module has no REST endpoint. Quarkus itself is an optional dependency of `support`, so it does
not reach this module, and that is on purpose: the application brings Quarkus. The tests of
[support/quarkus](../../support/quarkus) run a Quarkus application with a form endpoint instead,
and [support/quarkus-it](../../support/quarkus-it) runs one with the Quarkus module as a plain
dependency, the way a workflow module has it.
Running a workflow module needs a BPMS and the cockpit's adapter for it, `business-cockpit-*-adapter`,
and that is not what this module is about.

It is built as part of the reactor build described in the
[root README](../../README.md#building-it). It is neither installed nor published.
