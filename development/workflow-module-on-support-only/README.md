![](../../readme/vanillabp-headline.png)

# A workflow module on support only

A small workflow module which the build compiles and never runs. Its only dependency is
[support](../../support/spring-boot), the artifact `business-cockpit-spring-boot-support`. It has a workflow service with the annotations of VanillaBP and of the
cockpit's SPI, and a user class of its own which implements `UserDetails`. If a change to `support`
takes away something a workflow module needs to compile, this module stops the build.

So keep `support` its only dependency, and add no test scope dependency either. Any second
dependency could bring the class which `support` lost, and the build would stay green. Running a
workflow module needs a BPMS and the cockpit's adapter for it, `business-cockpit-*-adapter`, and
that is not what this module is about.

It is built as part of the reactor build described in the
[root README](../../README.md#building-it). It is neither installed nor published.
