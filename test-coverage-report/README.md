# Test coverage report

The aggregated coverage reports and the gate which judges them. Coverage is measured once per
deployable and per platform, because none of them executes another's code, and the
[root README](../README.md#test-coverage) is where the numbers and their badges are.

Each directory here aggregates one of them: `application/spring-boot` for the cockpit itself,
`extensions-commons/spring-boot` and `extensions-commons/quarkus` for the two halves of the
extension. A build writes each report to `report` below its directory, and the default branch
publishes them as pages.

`coverage-gate` is what breaks a build. It breaks on a report below its threshold, and it breaks on
a module producing coverage data no aggregated report reads, because everything covered by that
module alone would otherwise count as missed with nobody able to fix it. The thresholds are
properties of the root `pom.xml`, in percent of covered instructions.
`coverage.threshold.application-spring-boot` holds 55, which is what the container's suite reached.
`coverage.threshold.extensions-commons-spring-boot` and
`coverage.threshold.extensions-commons-quarkus` hold 85, the number every VanillaBP repository gates
on. None of them is the target. `coverage.rule` is, and it holds 90. Between a threshold and the
rule the build passes, and the gate prints how far each report still is from the rule. A threshold
is raised as tests arrive and never lowered to make a build green.

The gate reports what it measured on every run, green ones included. It is the one place in this
repository where a passing test prints, because here the passing run is the measurement. The angle
brackets stand for the numbers of the run:

```
coverage gate | Business Cockpit container (Spring Boot): <percent> % instructions (<missed> of <total> missed) | <gap> points below the rule of 90 %, build breaks below 55 %
coverage gate | Business Cockpit extension (Spring Boot): <percent> % instructions (<missed> of <total> missed) | at the rule of 90 %
coverage gate | Business Cockpit extension (Quarkus): <percent> % instructions (<missed> of <total> missed) | <gap> points below the rule of 90 %, build breaks below 85 %
```

A measurement at or above the rule ends in the short form. Anything below it names the gap and the
threshold its build would break at.

The gate judges what a run built. The reports are written in the `verify` phase, so `mvn package`
checks no threshold at all. It prints a line per measurement which names the command the build was
started with and says that the coverage was NOT checked, and the three tests holding the thresholds
are reported as skipped. Nothing fails over a file the run never wrote, and nobody reads such a run
as a checked one. `mvn install` is the run which judges.

Adding a module means adding it to the report which covers it. The second rule above is what tells
you if you forgot.

The gate module also keeps a red build short. Every test class of this repository registers
`SuppressOutputExtension` from `io.vanillabp:test-utils`, so a passing test prints nothing and a
failing one replays all of it, INFO included. `TestClassConventionsTest` reads the test sources of
the whole repository and names every class which forgot, and every class which registers the
suppression below `@Testcontainers`, where a container gets to talk before anybody listens.
`CoverageGateTest` is the one class which prints while it passes, and its `@PrintsWhenPassing` says
why.

`TestClassConventionsTest` also reads the main sources and checks the guiding messages. A message is
what a developer reads when something goes wrong, so a sentence which fell apart in the source takes
away the one explanation they get. Two ways of falling apart are caught: a run of spaces between two
words, which is what a text block pulled onto one line leaves behind, and a line ending with a word
and a backslash, which glues that word to the first word of the next line.
