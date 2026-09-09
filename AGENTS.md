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

## Two integrations live here at the same time

`adapters/*` is the Version 1 integration. It is built on `io.vanillabp:spring-boot-support` and it
stays untouched while the 0.8.x line is maintained. Nothing new belongs there.

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

## Building

The reactor contains an NPM build, so a full `mvn install` at the root is slow and needs a node
toolchain. Build the modules you touched instead:

```bash
mvn --batch-mode -pl extensions-commons/core,extensions-commons/spring-boot -am install
```

Quarkus tests load the extension from `~/.m2`, so they need `install`, never `package`.
