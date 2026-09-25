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

The gate judges what a run built. The reports are written in the `verify` phase, so a build which
stops at `package` prints a line per measurement saying that the coverage was not checked, instead
of failing over a file it never wrote.

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
