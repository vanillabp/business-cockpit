# Simulator

A standalone Spring Boot application which stands in for a business service: it reports user tasks
and workflows to the cockpit the way a workflow module does. Cockpit features can be built against
it without a BPMS and without a real application.

It also answers a part of the cockpit's official API. A user task or a case asked for by id is
made up and kept, so the next request gets the same one. `POST /usertask/count` and
`POST /workflow/count` count what it keeps, by the modes of the cockpit. It has no lists.

How to run it and what its test data form does is in
[development/README.md](../README.md#simulation-service).

Its React and Angular forms are gone, see decision 63 in the repository's
[DECISIONS.md](../../DECISIONS.md). So a task it reports points at `/remoteEntry.js`, which nothing
answers. The test data form is a page of the server and still works.

Built as part of the reactor build described in the [root README](../../README.md#building-it), and
published as `io.vanillabp.businesscockpit:simulator`, with a runnable jar attached to a release.
