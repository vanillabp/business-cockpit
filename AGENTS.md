# Working on business-cockpit

The VanillaBP Business Cockpit: the cockpit application itself, its APIs and UI libraries, and the
platform-neutral half of the Business Cockpit extension for VanillaBP Version 2.

Read [`README.md`](./README.md) first. It says what each module is, how coverage is measured and
what breaks a build.

Documentation is split by who reads it. The README files of this repository and its modules are for
somebody working on the code: what a module is, how it is built, what a change to it costs. The
[wiki](https://github.com/vanillabp/business-cockpit/wiki) is for somebody using the cockpit:
running it, configuring it, deriving an application from it, connecting a workflow module. Put a new
paragraph on the side its reader is on, and link across rather than writing it twice.

## One integration lives here

The Version 1 integration under `adapters/*` was built on `io.vanillabp:spring-boot-support`. It is
no longer part of this repository. Read it at the `0.4.0` tag if you need it, and use the 0.8.x
line for a workflow module which still runs on VanillaBP Version 1.

`extensions-commons` is the Version 2 integration, or rather its platform-neutral half. It joins the
deployment pipeline of the VanillaBP core through `io.vanillabp:vanillabp-extension-spi` and knows
no BPMS. The three BPMS halves live in their own repositories and consume this one as a published
artifact:

- [vanillabp/businesscockpit-camunda7-adapter](https://github.com/vanillabp/businesscockpit-camunda7-adapter)
- [vanillabp/businesscockpit-camunda8-adapter](https://github.com/vanillabp/businesscockpit-camunda8-adapter)
- [vanillabp/businesscockpit-process-engine-api-adapter](https://github.com/vanillabp/businesscockpit-process-engine-api-adapter)

Whatever those three need from here is an interface in `io.vanillabp.cockpit.extension.spi`. A
change there is a change to a published contract, so it is made deliberately and documented in the
module's README.

## Two names for the same thing

The wiki calls this an adapter. This repository, and the three adapter repositories, call it an extension. Both are right, and which
word fits depends on where you stand.

Somebody using the Business Cockpit adds one dependency and sees their user tasks in the cockpit.
From there this is a cockpit adapter, sitting next to the BPMS adapter which runs their workflows.
The VanillaBP core sees something else: a bean which joins its deployment pipeline through
`vanillabp-extension-spi`, and a bean like that is what the core calls an extension.

So the end-user documentation says adapter and never extension. The documentation in this repository
says extension where the core's own term is meant, and adapter where it is about what a user adds to
their application. Say which of the two you mean, rather than assuming the reader knows.

## The decision log is binding

[`DECISIONS.md`](./DECISIONS.md) holds the decisions several places in this repository rely on. It
is the ONLY thing the code is allowed to cite, in the plain greppable form
`see decision 7 in the repository's DECISIONS.md`, and only entries of THIS repository.

Read it before you change behaviour. An entry is not background reading, it is the reason the code
around it looks the way it does, so a change which contradicts one is wrong until the entry says
otherwise.

**A decision is changed or replaced only after asking.** Where your change would make an entry
untrue, stop and put the question to the maintainer before you write the change. If the answer is
yes, the same commit updates the log: the old entry STAYS, marked as superseded and naming the
entry which replaced it, and the new decision takes the next free number. Numbers are never reused
and never renumbered, because a citation in an older release still points at them.

Adding an entry has the same rule. A decision earns a number when several places rely on it and
copying the explanation to each of them would rot; anything smaller is a comment where it belongs,
and anything larger is documentation.

## How we write

Most people who read this repository read English as a second language, and so does the
maintainer. Long sentences, rare words and stacked nouns slow them down. Write so that
nobody has to read a sentence twice.

Short main sentences, one thought each. One subordinate clause is enough. Active voice.
The common word instead of the rare one: `use` instead of `leverage`, `about` instead of
`regarding`, `so` instead of `consequently`, `has` instead of `possesses`. A technical term
stays a technical term, but say what it means the first time it turns up, and write an
abbreviation out once. If a sentence trips you up when you read it aloud, rewrite it.

This holds for every English text here: the README files, `DECISIONS.md`, this file, the
Javadoc and comments which explain something, and the texts of commits and pull requests. It
holds for the [wiki](https://github.com/vanillabp/business-cockpit/wiki) as well, because the
wiki clone has no `AGENTS.md` of its own.

Nothing a program reads is renamed for the sake of language. Class and method names,
configuration keys, artifact coordinates and the headlines of decision log entries stay as
they are, because code, tests and other repositories point at them.

Before and after, from this repository:

> What a pull request is checked by is two jobs beside each other.

becomes

> A pull request is checked by two jobs which run beside each other.

> The BPMS' own event id is deliberately not part of the key, and no clock reading is - version 1
> built an event id from `System.nanoTime()`, which made every repetition a new event.

becomes

> The BPMS' own event id is deliberately left out of the key, and so is any clock reading.
> Version 1 built an event id from `System.nanoTime()`, which turned every repetition into a new
> event.

> A result type `UserTaskDetails` is expected. The effected task-id can be passed by defining a
> parameter annotated by `@TaskId`.

becomes

> The method returns `UserTaskDetails`. To get the id of the task, add a parameter annotated with
> `@TaskId`.

## Before you open a pull request

A number your branch hands out can be taken by the time you open the pull request. Another branch
was open at the same time and got there first. So check your numbers against `origin/main` and
against every open pull request, before the pull request exists.

It went wrong twice on 2026-09-13 in this repository: two branches claimed one number, which
had to become 19 and 20, and two more claimed the next, which had to become 21 and 22. Both
times it showed up at the merge, which is the worst moment for it. A merge happens on GitHub,
and a `see decision 21` in a Java file cannot be changed there.

The check:

```bash
bin/check-decision-numbers.sh
```

The script reports and changes nothing. By hand it is:

```bash
git fetch origin
git show origin/main:DECISIONS.md | grep -E '^#+ [0-9]+\. '   # the numbers already taken
gh pr list --state open
gh pr diff <n> | grep -E '^\+#+ [0-9]+\. '                    # for each open pull request
```

`gh pr diff` takes no path argument, so the grep does the filtering.

If your number is taken, your entry gets the next free one, and you correct every citation of it
in the code and in the documentation.

Read each citation before you change it. Not every `see decision <n>` in the branch is about your
decision. A branch can cite a number somebody else handed out long ago, and that citation stays
as it is. A search and replace over the branch turns a right reference into a wrong one.

None of this breaks the rule that a number is never renumbered. That rule is about a merged
number, which a citation in a released artifact points at. Until the pull request is merged,
nothing outside the branch has seen the number, so correcting it costs no more than the branch.

Every other running number is checked the same way. The story prompts are such a series. They are
kept outside this repository, so they are checked where they are kept.

## What code may point at

Nothing which a later change can invalidate without anything noticing: no story or prompt number,
no issue or pull-request number, no chat transcript, no person. Those record a conversation at a
point in time.

Where a name can carry the reason, the name is the better fix. Where it cannot, a comment says why
in its own words, complete where it stands. Only what several places have to carry becomes an entry
in the log.

Commit messages and pull-request descriptions may cite whatever they like. They are records of a
point in time themselves.

## Formatting

Spotless runs under `extensions-commons` only. The rest of the repository has no formatter and
stays that way, because reformatting it would bury every later diff. Inside that subtree
`mvn spotless:apply` runs before every commit; it formats the POMs and the Markdown as well as the
Java, and the build fails on a violation.

## Tests keep quiet until one fails

A test which passed has nothing to say. Whoever reads a red build is looking for the one place
where something went wrong, and the output of hundreds of happy tests is what hides it.

So every test class registers the output suppression of `io.vanillabp:test-utils`:

```java
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput   // only where a container or a server keeps writing
class MyTest {
```

The version of `test-utils` comes from the VanillaBP BOM imported in the root `pom.xml`, so a module
names the dependency in test scope and no version.

The annotation belongs on the class which holds the tests, not only on its base class. JUnit would
inherit it, but `TestClassConventionsTest` in `test-coverage-report/coverage-gate` reads the sources,
and it can only see what the file says.

Write the suppression above `@Testcontainers`. JUnit registers extensions in the order they are
written, and a Testcontainers extension registered first starts its container and logs it before
anything is listening.

INFO is the level everything is collected at, and nothing above it is ever dropped. A failure hands
all of it back, which is what somebody analysing that failure needs, and a level which drops a
record keeps it out of the capture too. So no `logback-test.xml` and no test configuration ever
goes above INFO. Going up TO INFO is a different thing and sometimes needed: logback defaults to
DEBUG when nothing configures it, and `extensions-commons/core` has the file which brings it to
INFO, because at DEBUG the Docker client writes a few hundred lines into every green build. A test
which has to read what was logged takes a `CapturedOutput` parameter instead of redirecting the
streams itself.

`CoverageGateTest` is the one class which prints on a green build. It carries `@PrintsWhenPassing`
with the reason, because its measured number is worth having in every log. A second exemption needs a
reason of that shape.

## Building

The reactor contains an NPM build, so a full `mvn install` at the root is slow and needs a node
toolchain. Build the modules you touched instead:

```bash
mvn --batch-mode -pl extensions-commons/core,extensions-commons/spring-boot -am install
```

Quarkus tests load the extension from `~/.m2`, so they need `install`, never `package`.

When you do build the whole reactor, pass `-Pjava-install`:

```bash
mvn --batch-mode -Pjava-install -DskipTests install
```

The profile switches every npm step off. Without it the build publishes each npm package to a
local registry at `http://localhost:4873/`, and a dev container has none. The build then dies in
`official-gui-api-client` with `ECONNREFUSED`, which looks like your own mistake. The pull
request build passes the same profile, see `.github/workflows/build.yaml`. It adds
`-Dskip.npm.publish-snapshot=true`, which the profile already sets.

The npm steps also change files which are checked in. The root POM has a step called
`npm install`, and that step runs `npm update`. So it raises each dependency to the newest
version its range allows and writes the result into the `package-lock.json` of the module.
A release wants that and commits it. Your branch does not, so never stage a lock file you did
not set out to change. Under `-Pjava-install` the step does not run at all.

A root build without the profile is a frontend build, and it needs the local npm registry from
`development/README.md`. A pull request checks the frontend with `bin/frontend-checks.sh`.
