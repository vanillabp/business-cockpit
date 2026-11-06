# The chain check reads every snapshot from Maven Central, and needs no token for it

This decision replaces one part of decision 35: where the check reads the published snapshots. The
rest of decision 35 stays as it is. Whoever merges this entry marks decision 35 in its headline as
"where the snapshots are read superseded by decision <n>", the way the log marks other entries.

Decided on 2026-11-06 by the maintainer.

## What was found

Decision 35 says that the chain check reads everything from the GitHub API, Maven Central and
GitHub Packages. GitHub Packages was there for one library only, `phactum/mongodb-changesets`. Its
snapshots were published to the GitHub Packages registry of its repository. That registry asks for
a login even for a public package. So the nightly workflow needed a token with the scope
`read:packages`, and without it question 4 was answered for the library from its publish runs
alone.

The library now publishes its snapshots to the snapshot repository of Maven Central, like the
VanillaBP repositories. Reading them there needs no login.

## The decision

`bin/check-the-chain.sh` reads the snapshot of every repository it watches from the snapshot
repository of Maven Central. That includes the MongoDB changeset library. Everything else comes from
the GitHub API, as before.

The check needs no token for the snapshots. The variables `PACKAGES_TOKEN` and `PACKAGES_USER` are
gone, and so is the case in question 4 which answered from the publish runs alone. A snapshot which
cannot be read counts as not answered, like every other question which could not be answered.

## What it costs

Nothing which the check did before is lost. Central removes a snapshot about 90 days after it was
published. So a repository which has not published for that long has no snapshot to read, and
question 4 is not answered for it. The report is then not green, which is right: such a repository
needs somebody to look at it.
