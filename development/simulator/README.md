# Simulator

A standalone Spring Boot application which stands in for a business service: it reports user tasks
and workflows to the cockpit the way a workflow module does. Cockpit features can be built against
it without a BPMS and without a real application.

In the profile `rest-sync` it reports by REST, with an HTTP service client of
[`support`](../../support/README.md#http-service-clients). Its keys are those of an HTTP service
group below `business-cockpit`, for example `business-cockpit.base-url`. That is the address of
the cockpit only, like `http://localhost:8080`. The client adds the path of the BPMS API itself, and
an address which ends with that path stops the start. `business-cockpit.default-header.Authorization`
carries the basic authentication.

It also answers a part of the cockpit's official GUI API, at `/official-api/gui/api/v1`. Each
system a simulator plays gets a prefix of its own, and the path of the original goes below it, see
decision 114 in the repository's [DECISIONS.md](../../DECISIONS.md). So a client of the official GUI
API takes `http://localhost:8079/official-api` as its address and adds `/gui/api/v1` itself, as it
does for the cockpit. A user task or a case asked for by id is made up and kept, so the next
request gets the same one. `POST /usertask/count` and
`POST /workflow/count` count what it keeps, by the modes of the cockpit. It has no lists.

It is also the OpenID Connect provider which `container` logs a person in at in its profile
`local`. That is Spring Authorization Server, configured in `AuthorizationServerConfiguration`. The
persons are the users of `dev-shell-simulator.users`, and each logs in with `test` at the login
form of the simulator. The client `business-cockpit` with its secret and the address the browser
goes back to, `http://localhost:8080/login/oauth2/code/simulator`, are Spring Boot keys below
`spring.security.oauth2.authorizationserver.client` in `config/application.yaml`. The key which
signs the tokens is new at every start. That is enough, because the cockpit reads the tokens of the
provider only once, at the login, and keeps the login in a cookie of its own. Once the code is
issued, the simulator ends the session of the person, so the next login asks again. Its session
cookie is `SIMULATOR_SESSION`, so it does not replace the `JSESSIONID` of the cockpit on the same
host.

How to run it and what its test data form does is in
[development/README.md](../README.md#simulation-service).

Its React and Angular forms are gone, see decision 63 in the repository's
[DECISIONS.md](../../DECISIONS.md). So a task it reports points at `/remoteEntry.js`, which nothing
answers. The test data form is a page of the server and still works.

Built as part of the reactor build described in the [root README](../../README.md#building-it).
Its runnable jar is attached to each GitHub release. It is not published to Maven Central, because
nothing builds against it.
