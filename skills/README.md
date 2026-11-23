![VanillaBP](https://raw.githubusercontent.com/vanillabp-blueprints/.github/main/profile/vanillabp-headline.png)

# Business Cockpit agent skills

[![Apache License V.2](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](../LICENSE)

Skills which teach an AI coding agent how to build a [Business Cockpit](https://github.com/vanillabp/business-cockpit)
of your own with [VanillaBP](https://www.vanillabp.io). They follow the
[Agent Skills specification](https://agentskills.io/specification). So the same files work in
Claude Code, GitHub Copilot, VS Code, Cursor, OpenAI Codex, Gemini CLI and the other agents
which support the standard.

|                         Skill                          |                                                                                                                                                                                                                                                                   What it does                                                                                                                                                                                                                                                                    |
|--------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`business-cockpit-creator`](business-cockpit-creator) | Generates a Business Cockpit that fits your organisation's tech stack, as two repositories: the user interface in the web framework you choose, with its types, module kit, module host, theme and dev shells, and the backend on the business-cockpit library with login, list controllers and a docker-compose file. It interviews you first, plans the work as small business stories in the repository, builds them one by one and checks the result against a versioned checklist. It also upgrades a generated cockpit to a newer checklist |

The skill lives here, next to the cockpit, because it follows the cockpit's contract with workflow
modules and changes together with it. For workflow modules, and for questions about the cockpit in
general, use the skills in [vanillabp/skills](https://github.com/vanillabp/skills): `vanillabp`
builds the applications whose user tasks the cockpit shows, and `business-cockpit` leads a question
to the page which answers it.

## Installing

### With the GitHub CLI

Needs `gh` 2.90 or later.

```bash
gh skill install vanillabp/business-cockpit business-cockpit-creator@main               # into the current project
gh skill install vanillabp/business-cockpit business-cockpit-creator@main --scope user  # for every project
```

Name `@main` until the cockpit 1.0 is released. Without a version, `gh skill` takes the newest
release tag of the repository, and the releases before 1.0 do not hold the skill. A skill installed
with `@main` is pinned to that commit, so install it again with `--force` to get a newer one.

`gh skill install` picks the directory your agent reads. Name it explicitly with `--agent`, for
example `--agent claude-code`, `--agent github-copilot` or `--agent cursor`. Later,
`gh skill update` pulls newer versions, and `gh skill list` shows what is installed.

### With the skills registry

```bash
npx skills add vanillabp/business-cockpit
```

### By hand

Copy the directory `business-cockpit-creator` into the place your agent reads. The usual ones
are:

|          Agent          |             Project scope              |      User scope      |
|-------------------------|----------------------------------------|----------------------|
| Claude Code             | `.claude/skills/`                      | `~/.claude/skills/`  |
| GitHub Copilot, VS Code | `.github/skills/` or `.agents/skills/` | `~/.copilot/skills/` |
| Cursor                  | `.cursor/skills/`                      |                      |
| OpenAI Codex            | `.codex/skills/` or `.agents/skills/`  | `~/.codex/skills/`   |
| Several agents together | `.agents/skills/`                      | `~/.agents/skills/`  |

## Using it

The skill loads itself when a task matches its description, for example "build us a Business
Cockpit in Vue". Some agents let you name it directly; there `/business-cockpit-creator` does
that.

Start it in an empty directory, or in a cockpit it generated before. It asks its questions first
and writes the answers down, then plans the work in `plan/` of the repository. Every start begins
by reading that plan, so you can stop the agent at any time, and the same or another agent goes
on where the work stopped. Questions it cannot settle alone go into the plan with the default it
takes, so a run does not wait for you.

Name the exact versions of the VanillaBP packages the cockpit builds on, or let the skill look
them up: `@vanillabp/bc-module-kit` and `@vanillabp/bc-module-host` are published on npmjs, the
backend artifacts on Maven Central.

## Where the knowledge comes from

|                                         Source                                          |                                                              What it carries                                                              |
|-----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| [business-cockpit wiki](https://github.com/vanillabp/business-cockpit/wiki)             | the cockpit container, its configuration, its extension points, connecting a workflow module                                              |
| [business-cockpit](https://github.com/vanillabp/business-cockpit)                       | the GUI API and the BPMS API as OpenAPI files, the business-cockpit library, the support libraries for workflow modules                   |
| [business-cockpit-module-kit](https://github.com/vanillabp/business-cockpit-module-kit) | the minimum contract between a cockpit and the parts of a workflow module, the module host, and example modules in React, Vue and Angular |
| [Blueprints](https://github.com/vanillabp-blueprints)                                   | workflow modules you can run against a cockpit, with their user task forms, case pages and header actions                                 |

The skill links this documentation instead of copying it. Where the skill and the documentation
disagree, the documentation wins.

## Contributing

Issues and pull requests are welcome here. A change to the cockpit itself belongs in the code and
the wiki first. The skill follows afterwards.

Check a change before you open a pull request:

```bash
gh skill publish --dry-run
```

## Noteworthy and contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back
to the community as it has benefited from the community in the past.

## License

Copyright 2026 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0.
