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
properties of the root `pom.xml`, in percent of covered instructions. Between a threshold and the
rule of 90 the build passes, and the gate prints how far each report still is from that rule.

Adding a module means adding it to the report which covers it. The second rule above is what tells
you if you forgot.
