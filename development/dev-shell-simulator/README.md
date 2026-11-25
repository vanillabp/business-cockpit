![](../../readme/vanillabp-headline.png)

# Dev shell simulator

The server behind a dev shell: it takes the reports a workflow module sends
and serves them back to the components the dev shell renders, and it supplies the users those
components are looked at as. So a workflow module's user interface can be built without a cockpit
and without a BPMS. The dev shells live in the repositories of the user interfaces they fit, see
decision 63 in the repository's [DECISIONS.md](../../DECISIONS.md).

Running it, giving it users and pointing an application at it is in the wiki, under
[Developing UI components locally](https://github.com/vanillabp/business-cockpit/wiki/Developing-UI-components-locally#the-dev-shell-simulator).
This file is about the module.

It answers the parts of the cockpit's two interfaces the dev shell needs, which is why it is a
Spring Boot application rather than a mock inside the dev shell: what it answers has to be what the
cockpit answers, or a form works here and not there. It holds everything in H2, in memory unless a
file is configured.

An application which extends the simulator may keep its tables in another database and create
them with the Liquibase changelog `src/main/resources/db/changelog/dev-shell-simulator.yaml`. So a
new field of an entity needs a new change set there. A change set which is out already is never
changed, because Liquibase refuses a changelog whose applied change sets differ.
`TheChangelogMatchesTheEntitiesTest` applies the changelog to an empty H2 and an empty MySQL and
starts the simulator with `ddl-auto=validate`, so it goes red when a column is missing. The time
columns of the entities name no SQL type, because `TIMESTAMP WITH TIME ZONE` does not exist in
MySQL.

Of the BPMS API it answers version 1.1 only, at `/bpms/api/v1_1`. A report to `/bpms/api/v1` gets a
404, although the cockpit still takes it. Version 1.1 has existed for more than a year, and a
workflow module which still reports through version 1 needs a current adapter for the dev shell.

Each system the simulator plays gets a prefix of its own, and the path of the original goes below
it. The cockpit's official GUI API has the prefix `/official-api`, so the simulator answers it at
`/official-api/gui/api/v1`, where the cockpit answers `/gui/api/v1`. The paths of two systems
cannot collide that way. A workflow module points its generated client at the simulator by the
address with the prefix, for example
`spring.http.serviceclient.official-gui-api.base-url=http://localhost:8079/official-api`, and the
client adds `/gui/api/v1` as it does for the cockpit. See decision 114 in the repository's
[DECISIONS.md](../../DECISIONS.md).

Of the official GUI API it answers the lists, a single user task or case, the workflow modules the
user signed in sees at `/workflow-module`, the user signed in at `/app/current-user` and the
notification settings of that user. The user signed in comes with his groups completed by the group
hierarchies of the registered workflow modules, as the cockpit does it. The same groups decide which
workflow modules he sees: a module which registered groups is listed for their members, a module
without groups for everybody. The simulator sends no notifications, so it offers no medium, and it keeps the notification
settings in memory only.

It does not answer what only the cockpit's own frame needs, such as
`/gui/api/v1/app/current-user/group-hierarchy`, which tells the menu of the cockpit where the rights
of the user come from. No component of a workflow module asks for it.

What only the simulator answers, the reports and the workflow modules of a named user at
`/simulator-api` and the switch of users at
`/dev-shell/user`, is described in an OpenAPI file of its own, in
[`apis/dev-shell-simulator-api`](../../apis/dev-shell-simulator-api), see decision 112. The controllers implement the
interfaces generated from it, and the tests call these paths through the generated client.

Built as part of the reactor build described in the [root README](../../README.md#building-it), and
published as `io.vanillabp.businesscockpit:dev-shell-simulator`, with a runnable jar attached to a
release.

## Noteworthy & Contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back to the community as it
has benefited the community in the past.\
![Phactum](../../readme/phactum.png)

## License

Copyright 2025 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0
