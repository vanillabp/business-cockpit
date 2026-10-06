# Contributing

This repository is the VanillaBP Business Cockpit: the application business people work in, its
APIs and UI libraries, and the platform neutral half of the Business Cockpit extension for VanillaBP
Version 2. How to run and use the cockpit is in the
[wiki](https://github.com/vanillabp/business-cockpit/wiki). This file is for somebody who changes
the code.

## From a bug to a pull request

Found a bug? You can hand it to a coding agent.

1. Clone [`development-workspace`](https://github.com/vanillabp/development-workspace) with
   `git clone --recurse-submodules`. It holds the VanillaBP repositories side by side. Its
   `.claude/skills` folder holds the skills Claude Code reads when it starts at the root of the
   workspace.
2. Start your coding agent at the root of the workspace and describe the bug: what you did, what
   you expected and what happened instead.
3. The agent builds a scenario which reproduces the bug, looks for the cause and fixes it. Then it
   pushes a branch to your fork and opens a pull request from there.
4. The VanillaBP team reviews the pull request and merges it.

You need a GitHub account, the GitHub CLI logged in with `gh auth login`, a fork of the repository
you change (the agent can create it with `gh repo fork`), and Java 21, Maven and Docker for the
build. The submodules are cloned over SSH. If GitHub has no SSH key of yours, run
`git config --global url."https://github.com/".insteadOf "git@github.com:"` before you clone. If
the bug is in a repository the workspace does not hold, the agent clones it next to the others.
Before it opens the pull request, the agent follows the `CONTRIBUTING.md` and the `AGENTS.md` of
the repository it changes, where it has one. The two Camunda adapters live in the Camunda Community
Hub, which asks you to sign its contributor license agreement on your first pull request there.

## Before you change the code

Read [`README.md`](./README.md) first. It says what each module holds and how to build.
[`DECISIONS.md`](./DECISIONS.md) holds the decisions several places rely on. Read it before you
change behavior, and ask before a change makes one of its entries untrue.
[`AGENTS.md`](./AGENTS.md) holds the rules a change follows, in the form an agent reads.
