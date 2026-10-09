# Simulator

A standalone Spring Boot application which stands in for a business service: it reports user tasks
and workflows to the cockpit the way a workflow module does. Cockpit features can be built against
it without a BPMS and without a real application.

How to run it and what its test data form does is in
[development/README.md](../README.md#simulation-service).

Its user interface is here twice, `src/main/webapp-react` and `src/main/webapp-angular`. Maven does
not build either of them any more, so the runnable jar serves neither, see decision 62 in the
repository's [DECISIONS.md](../../DECISIONS.md). The React one is also the worked example a workflow module's user
interface is copied from, described in the wiki under
[User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites).

Built as part of the reactor build described in the [root README](../../README.md#building-it), and
published as `io.vanillabp.businesscockpit:simulator`, with a runnable jar attached to a release.
