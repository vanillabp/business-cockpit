# The GUI API

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The GUI API is everything a cockpit user interface gets from the server. It lives under
`/gui/api/v1`. Two OpenAPI files describe it, and these two files are the truth:

- `apis/official-gui-api/openapi/v1.yaml`, the `official-gui-api`: the lists, a user task, a case,
  the workflow modules, the notifications and the update stream.
- `business-cockpit/src/main/resources/api/gui/v1.yaml`: `GET /app/info` and
  `GET /app/current-user`.

What is written here is the behaviour behind them, which neither file says.

1. [Generating the client](#generating-the-client)
1. [Protection and login](#protection-and-login)
1. [Staying logged in](#staying-logged-in)
1. [The endpoints](#the-endpoints)
1. [Reading page by page](#reading-page-by-page)
1. [Sorting](#sorting)
1. [Counting](#counting)
1. [Filtering over reported business data](#filtering-over-reported-business-data)
1. [Suggestions for a search field](#suggestions-for-a-search-field)
1. [The primary filter: which rows come at all](#the-primary-filter-which-rows-come-at-all)
1. [The timestamp that keeps a list stable](#the-timestamp-that-keeps-a-list-stable)
1. [The second form of the same query](#the-second-form-of-the-same-query)
1. [The update stream](#the-update-stream)
1. [How a row ended](#how-a-row-ended)
1. [User settings](#user-settings)
1. [What the cockpit serves](#what-the-cockpit-serves)

## Generating the client

Generate the client from the OpenAPI files. Never write it by hand.

The tool is [`orval`](https://orval.dev), a dev dependency of the project. It runs on Node alone
(22.18 or newer) and needs no Java. Set `output.client` in `orval.config.ts`:

- `fetch` for every framework except Angular. It uses the browser's `fetch`.
- `angular` for Angular. It writes services that use Angular's `HttpClient`, so the interceptors
  of the application see every request.

Why `orval`: it covers both kinds of client with one tool and one configuration. Its major version
is stable (8 in October 2026), and new versions come out every few days. `@hey-api/openapi-ts` also
has a fetch and an Angular client, but it is still below 1.0, so every minor version may break
the generated code. `openapi-typescript` with `openapi-fetch` has no Angular client.
`ng-openapi-gen` writes Angular only, so a second tool would be needed for the other frameworks.
Pin the version of `orval` exactly in `package.json`.

Generate it at build time, for example in a `prebuild` script, into a folder that Git ignores. It is
never committed. The same goes for every dev shell: it generates its own client from the same
files. `orval` also writes the TypeScript interfaces of the data, so the types library takes them
from the same run, see [types.md](types.md#data-from-the-gui-api).

How the npm project gets the two files: both ship inside jars of the cockpit. The jars lie in the
Maven repository the cockpit artifacts are deployed to. The interview names that repository and the
Maven coordinates of the cockpit (`Q-COCKPIT-VERSION`, `Q-COCKPIT-REPOSITORY`). By default the
group is `io.vanillabp.businesscockpit`.

| File                          | Jar                                     | Path inside the jar |
|-------------------------------|-----------------------------------------|---------------------|
| the `official-gui-api`        | `official-gui-api-server-<version>.jar` | `v1.yaml` |
| `app/info` and `current-user` | `business-cockpit-<version>.jar`        | `api/gui/v1.yaml` |

A script downloads the two jars with HTTP `GET` from the path Maven uses,
`<repository>/<group as path>/<artifactId>/<version>/<artifactId>-<version>.jar`. It unpacks the two
files and runs the generator. Maven Central is one possible repository, but not the fixed one.

- Today the cockpit deploys its snapshots to GitHub Packages. GitHub Packages asks for a login even
  to read. Use the same token that `.npmrc` uses for the npm registry of GitHub Packages. The script
  reads it from the environment variable the interview names, never from a file in the repository.
- A `-SNAPSHOT` version has a time stamp in its file name. The script first reads
  `<version>/maven-metadata.xml` and takes the newest jar it lists.
- Pin a release version where there is one. Then a new cockpit release never changes your client
  without a commit. A snapshot can change under the same version.

The other way is the raw file in the `business-cockpit` repository at the release tag, for example
`https://raw.githubusercontent.com/vanillabp/business-cockpit/<tag>/apis/official-gui-api/openapi/v1.yaml`.
That needs no unpacking, but it reads the source and not the released artifact. The jar is the
default.

The update stream `GET /updates` is in the `official-gui-api` since prompt `1511`. So the generated
client brings the types of its events too, `UserTaskEvent` and `WorkflowEvent`. It brings no method
to use for the stream itself, because a stream never ends. Open the stream with `EventSource`, or
with `fetch` and a reader on the body, see [The update stream](#the-update-stream).

## Protection and login

The backend serves the user interface. The single-page application has to load without a login
and log in afterwards, when needed. So these stay open:

- the static files of the user interface,
- the shell, `index.html`, also as the answer to an unknown path,
- `GET /gui/api/v1/app/info` and `GET /gui/api/v1/app/current-user`,
- the proxy to the workflow modules under `/wm/**`. A module protects itself.

Everything else asks for a login. Login is the job of the backend application, not of the user
interface, see [`../SKILL.md`](../SKILL.md#how-your-user-interface-gets-into-the-cockpit). The
`business-cockpit` library sets no rules for it. The application decides which paths are open, and
the generated user interface never asks for a change of the library. Today the library still has a
default chain of its own; prompt `1510` moves it into `container`, see
[rough-edges.md](rough-edges.md#what-needs-a-newer-cockpit).

A login answers with a cookie, and from then on the cookie travels with every request. The cookie
holds a JWT. How it is signed, how long it lives and how an identity provider is put in front is in
the wiki under [Security](https://github.com/vanillabp/business-cockpit/wiki/Security).

What follows for the start of the user interface:

1. `GET /gui/api/v1/app/info` fetches the title, the version and the build time of the
   application. It needs no user, so the frame can stand before anybody is logged in.
2. `GET /gui/api/v1/app/current-user` returns the user who is logged in, with their groups. If
   nobody is logged in, the answer has no body. Do not treat that as an error. Send the browser to
   the login of the backend (`Q-LOGIN`).
3. Only then is the first list worth loading.

The groups come from this answer and not from the JWT. A token may carry few groups or none, see
[behaviour.md](behaviour.md#who-sees-what).

`current-user` can answer without a body for a second reason: the request that logs in is
authenticated, but only its answer carries the cookie. The cockpit reads the user from the cookie,
so exactly this request has no user yet, and the client asks again with the cookie.

`/logout` deletes the cookie and sends the browser to the start page.

## Staying logged in

The server renews the cookie by itself. When the token has less than half of its lifetime left
(`business-cockpit.jwt.cookie.expires-duration`, twelve hours by default), the answer to an ordinary
request carries a new cookie. The browser takes it over, and your user interface does nothing for
it. Plan for two things anyway:

- The update stream does not count. A tab where nobody does anything is logged out after twelve
  hours. So do not ask anything in the background only to stay logged in. That is exactly what must
  not happen.
- Every login ends after `business-cockpit.jwt.cookie.max-login-duration`, seven days by default,
  even in the middle of work. After that every request answers 401, and the user has to log in
  again. So your user interface must handle a 401 at any time, not only at the start.

## The endpoints

The task lists of the runnable cockpit are the same endpoints under three base paths. Which tasks
each of them shows is decided by a controller of the application and not by the API, see
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).

| Base path                  | What the list shows |
|----------------------------|---------------------|
| `/gui/api/v1`              | everything the user may work on |
| `/gui/api/v1/current-user` | only what is assigned to the user personally |
| `/gui/api/v1/user-roles`   | what the user's groups may claim and nobody is assigned to yet |

Do not take these three paths as given. They are a choice of the runnable cockpit. An application of
the organisation's own may offer other lists, and then the paths have other names. `/tasks` uses
the first base path, and `/my-tasks` the second one, or the first with a filter on `assignee.id`.

The endpoints, all relative to the base path:

| Call                                                                     | What for |
|--------------------------------------------------------------------------|----------|
| `POST /usertask`                                                         | one page of the task list |
| `PUT /usertask`                                                          | the same query as an update, see below |
| `OPTIONS /usertask`                                                      | suggestions for a search field over user tasks |
| `GET /usertask/{id}`                                                     | one user task, with `markAsRead` also marked as read |
| `PATCH /usertask/{id}/claim`, `PATCH /usertask/claim`                    | set the assignee, with `unclaim=true` give it back |
| `PATCH /usertask/{id}/assign`, `PATCH /usertask/assign`                  | add a person as candidate user, with `unassign=true` remove them |
| `PATCH /usertask/{id}/mark-as-read`, `PATCH /usertask/mark-as-read`      | mark as read, with `unread=true` take it back |
| `PATCH /usertask/{id}/follow-up-date`                                    | set or delete the follow-up date |
| notes of a user task and of a case                                       | read and write notes, added by prompt `1507` |
| counting a list                                                          | the number of rows of a query, added by prompt `1506` |
| `GET /app/current-user/settings/{key}`, `PUT` on the same path           | read and write one entry of `customSettings`, added by prompt `1505` |
| `POST /user`                                                             | search users, to propose a task to a person |
| `POST /workflow`                                                         | one page of the case list |
| `PUT /workflow`                                                          | the same query as an update |
| `OPTIONS /workflow`                                                      | suggestions for a search field over cases |
| `GET /workflow/{id}`                                                     | one case |
| `POST /workflow/{id}/usertasks`                                          | the user tasks of a case |
| `GET /workflow-module`, `GET /workflow-module/{id}`                      | the workflow modules the user may see |
| `GET /notifications/media`, `/config`, `/recipient-config`, `/workflows` | the notification settings of the user; `PUT` on `/config` and `/recipient-config/{mediumType}` stores them |
| `GET /app/info`, `GET /app/current-user`                                 | the application and the user |
| `GET /updates`                                                           | the update stream, see below |

The two forms of each action, one for one task and one for a list of ids, do the same. The list
form is there so that several selected rows do not cause twenty requests.

The prompts `1506` and `1507` decide the paths of their endpoints. Take them from the OpenAPI file
of the pinned cockpit version, not from this text. The path of the user settings is fixed, see
[User settings](#user-settings). For counts see [Counting](#counting).

`GET /workflow-module` answers the modules the user may see, each with `id`, `version` and `uri`.
`uri` is `/wm/<id>`, the path of the module behind the cockpit's proxy, or empty if the module
registered no address. It carries no `uiUriType`. Your user interface calls it once, when it
starts, and keeps the answer in memory for all its parts. One central loader then fetches the
exposed parts of the modules from these addresses. No list and no page calls this endpoint itself,
see [module-federation.md](module-federation.md#one-registry-and-one-loader).

`GET /app/info` also returns `additionalProperties`, a free map. The backend fills it from
`business-cockpit.additional-properties`. Use it for what the user interface needs to know about
the installation, for example the languages it offers (`Q-LANGUAGES`):

```yaml
business-cockpit:
  additional-properties:
    languages: en,de
```

## Reading page by page

`pageNumber` and `pageSize` of `POST /usertask` and `POST /workflow` go to the database unchanged.
If `pageNumber` is missing, the first page comes, so `0`. If `sortAscending` is missing, the server
sorts ascending. This holds for the update with `PUT` as well.

`pageSize` has no default. If it is missing, the server answers 400 and names the field:
`The request is not valid: 'pageSize' is missing.` A page size below 1 and a page below 0 are
refused with 400 too. So always set `pageSize`.

There is no upper limit. Choose the page size with care (`Q-PAGE-SIZE`). The server stops you from
nothing, not even from 100000 rows in one request. A list never holds all its rows in the browser.
It holds a window of pages around the visible area and asks for each page with its `pageNumber`,
see [behaviour.md](behaviour.md#paging-and-the-window-of-loaded-rows).

The answer carries a `page` with `number`, `size`, `totalPages` and `totalElements`.
`totalElements` is counted exactly, in a second query over the same conditions. So the list knows
its own total from every page. Every other number comes from the count endpoint, see
[Counting](#counting).

`POST /workflow/{id}/usertasks` is the exception: there the page size is 100 by default.

## Sorting

`sort` is a list of paths in the stored document, separated by commas. `sortAscending` holds for
all of them.

A path names either a field of the list or a key of the reported business data below `details.`.
The fields of the list are the fields of `UserTask` or `Workflow` in the description that the
server stores under the same name, for example `dueDate`, `title.de` or `assignee.sort`. `uiUri`,
`workflowModuleUri` and `read` are not among them, because the server computes them only when it
answers. Each part of a path consists of letters, digits and `_`. Below `details.` the hyphen is
allowed as well, because a workflow module may report a key like `order-id`. A space or a `$` is
allowed nowhere. If a replacement for the dot is configured for the business data, it may appear
below `details.`.

The server refuses every other path with 400:
`The request is not valid: 'sort' may only name fields of the list or keys below 'details.'. A part
of a path holds letters, digits and '_', and below 'details.' also '-'.` This holds for the list and
for the update.

For every new combination of paths the server creates an index in the database, but at most 30 per
collection (`business-cockpit.mongodb.sort-indexes-per-collection`). After that it sorts without
an index of its own and warns once per combination in the log. The result is the same, only slower
on a large collection. So send paths that come from a column.

The server adds its own order to what you send, so that paging stays stable. For user tasks this is
`dueDate`, `createdAt`, `id`, for cases `createdAt`, `id`. A field you named yourself is left out of
this addition. Because `id` is unique and comes last, the order is complete, and the same query
returns the same order twice.

Without `sort` the server sorts user tasks by `dueDate` and cases by `createdAt`.

A user task without a due date is not without a value. The server stores a point in the far future
for it and answers `null` in the API, so such a task ends up at the back when sorted by due date.

For every other field an empty value is empty, and where it ends up is decided by the database.
Expect rows without a value at the front when sorting ascending.

## Counting

The list shows a total next to every primary filter, and the toggle in the header shows one next to
each list, see [behaviour.md](behaviour.md#counts). The total of the chosen primary filter comes
with the page. Every other count comes from the count endpoint that prompt `1506` adds. It takes
the same conditions as the list (base path, primary filter, filters, `initialTimestamp`) and
answers a number, without rows and without sorting. Counting fast, with an index, is the job of
the backend. Never ask for a page only to read its `totalElements`.

## Filtering over reported business data

Each `SearchQuery` has three fields and becomes one condition:

```json
{ "path": "details.customer.name", "query": "Meier", "caseInsensitive": true }
```

`path` is the path in the document, the same as for `sort` and the same as `path` of a column. If
it is missing, the server searches the field `detailsFulltextSearch`. That is the full text the
workflow module has put together itself, see
[Reporting workflows and user tasks](https://github.com/vanillabp/business-cockpit/wiki/Reporting-workflows-and-user-tasks).
One full-text field and nothing more: what was not reported cannot be found.

The same rule as for a sort path holds for `path`, see [Sorting](#sorting): a field of the list or
a key below `details.`. The server refuses every other path with 400, for example
`The request is not valid: 'searchQueries.path' may only name fields of the list or keys below
'details.'. A part of a path holds letters, digits and '_', and below 'details.' also '-'.` So a
field the API does not show cannot be reached by a filter either.

`query` is a regular expression, not text. It is not anchored, so it also matches in the middle of
a value. A `.` or a `*` from the user's input is read as a special character. When your user
interface passes an input on, escape it, see
[rough-edges.md](rough-edges.md#a-search-pattern-goes-into-the-database-unescaped).

`caseInsensitive` switches on the option `i`. Otherwise upper and lower case are told apart.

Several `SearchQuery` entries are joined with AND. Do not send two entries with the same `path`.
When the user sets a filter on a path, remove the old entry for that path first, see
[rough-edges.md](rough-edges.md#two-filters-on-one-path).

A field that looks like a filter and is not one: `businessIds` of `WorkflowsRequest` is an exact
match on the field `businessId` and not a search pattern. It only works on `POST /workflow` and not
on the update with `PUT`, see
[rough-edges.md](rough-edges.md#businessids-is-lost-in-the-update).

## Suggestions for a search field

`OPTIONS /usertask` and `OPTIONS /workflow` return words and how often they occur. This is called
"keyword in context", KWIC for short. A user interface uses them for the suggestions below a search
field.

The query takes `query` and, if wanted, `path` as parameters, and the filters already set as
`searchQueries` in the body. It answers with whole words that contain `query`, each with a count.
`path` and the paths in `searchQueries` follow the same rule as a sort path, otherwise the server
answers 400. For `path` the message names `'path'`.

What you need to know:

Below three characters the answer is empty, and it is no error. So let the field ask only from
three characters on.

The search is always case-insensitive here, whatever a `SearchQuery` says.

The answer has at most 21 entries, and they are not the 21 most frequent ones, but the first 21 the
database finds. They are sorted by count ascending, and by word for the same count. When 21 entries
come, show a note that there are more than twenty matches instead of the list, see
[rough-edges.md](rough-edges.md#the-suggestions-are-not-the-most-frequent).

The count counts occurrences of the word, not rows. A word that appears twice in one user task
counts twice.

The suggestions keep to the user's visibility and to the `searchQueries` that are set, but not to
the primary filter. They always look at open user tasks and active cases only. So a list that shows
closed tasks gets suggestions that do not fit it.

## The primary filter: which rows come at all

Two things decide which rows come: the mode your user interface sends, and the visibility the
server determines for the user. The mode is called the primary filter. The user picks it in the
status line of the list, see [behaviour.md](behaviour.md#the-task-lists), and this skill uses
this term everywhere.

| `mode` of a task list      | What comes |
|----------------------------|------------|
| `All`                      | everything, open and closed |
| `OpenTasksWithFollowUp`    | everything open, with and without a follow-up date |
| `OpenTasksWithoutFollowUp` | open, without a follow-up date or with one that is due |
| `OpenTaskOnlyFollowUp`     | open, with a follow-up date in the future |
| `ClosedTasksOnly`          | only what has an end |

The mode `OpenTasks` is gone since prompt `1509`. It returned the same rows as
`OpenTasksWithFollowUp`. So the open modes are a pair, with and without follow-up, plus the
follow-ups alone.

| `mode` of a case list | What comes |
|-----------------------|------------|
| `All`                 | everything |
| `Active`              | what has no end |
| `Inactive`            | what has an end |

Without `mode` the server takes the default of the cockpit since prompt `1509`:
`OpenTasksWithFollowUp` for a task list and `Active` for a case list, for `POST` and `PUT` alike.
Still always send the primary filter yourself, so the list never depends on a default. The dev shell
always sends `All`, see [dev-shell.md](dev-shell.md#what-it-shows).

An unknown value is a server error and not silently ignored.

The visibility is the server's business. Your user interface does not ask for it and cannot get
around it. Which rule the runnable cockpit applies is in the wiki under
[Who sees which workflow](https://github.com/vanillabp/business-cockpit/wiki/Security#who-sees-which-workflow)
and [Who sees which user task](https://github.com/vanillabp/business-cockpit/wiki/Security#who-sees-which-user-task).
The same rule holds for the list, for a single row, for the suggestions and for every action.

`POST /workflow/{id}/usertasks` has a switch of its own for this, the parameter `llatcup`. With
`true` come the user tasks this user may work on, with `false` all user tasks of the case. A page
that shows the state of a case usually wants `false`, a page that gives work to do wants `true`.

## The timestamp that keeps a list stable

`initialTimestamp` is the moment the user started to look at this list. It has an effect at only
one place, and that place matters: in the primary filters for open user tasks and active cases, a
row still comes along if it ended after this moment.

Without it the list would move under the reader. A task that ends on page one would disappear, and
everything after it would move up by one row. When paging, the user would never see one row.

So:

1. The first request of a list sends no `initialTimestamp`. The server puts in the current time.
2. The answer returns this value as `serverTimestamp`. It is the value put in, not a new look at
   the clock.
3. Your user interface keeps it and sends it as `initialTimestamp` with every further request of the
   same list. Also when paging, sorting, filtering and asking for suggestions.
4. Only when the user loads the list again does the timestamp start again.

The same timestamp is the base for the colour of a row, see
[behaviour.md](behaviour.md#live-updates).

`requestId` in `POST /workflow` and `PUT /workflow` is returned by the server unchanged and not
read. `UserTasks` has no such field. What the field was meant for is said neither by the code nor
by the OpenAPI description. Matching an answer to its request would be the obvious use, but do not
rely on it.

## The second form of the same query

Next to `POST`, each list has a `PUT` with `size` and the ids the client already has. It answers
with the first page of `size` rows, in the same order as the list, and it returns

* a row the client already knows only as a shell with its id,
* a row it does not know yet in full.

So your user interface learns the whole current order and loads only what is new. A shell can be
told by the field `version`: it carries `0`, a real row never does.

Use it like this: after a wake-up call from the update stream, ask with `PUT` for as many rows as
you have loaded, plus a reserve, and leave out the ids of the rows you know have changed. Exactly
these come back in full.

Two things are different for `PUT`: `page.number` is always 0 and `page.size` is the number of rows
returned, and there is no `businessIds`.

`PUT` has no offset. It always starts at the first row. That fits a window of loaded rows at the top
of the list. For a window further down, `PUT` would return every row above it in full, so read the
pages of the window again with `POST` and the same `initialTimestamp` instead, see
[rough-edges.md](rough-edges.md#the-update-form-has-no-offset).

## The update stream

`GET /gui/api/v1/updates` is a `text/event-stream` (server-sent events). There is one stream per
browser tab. An event in it is a wake-up call. It names the kind of entity, its id and what
happened to it. It does not say what changed. Your user interface asks again.

The path is in the `official-gui-api` since prompt `1511`, so the types of the events come from the
generated client. The connection does not: open it with `EventSource`, or with `fetch` and a reader
on the body. A generated call waits for the end of the answer, and a stream has none.

What comes through it:

| Name of the event | Data |
|-------------------|------|
| `ping`            | an object with `id` and `name`, nothing else |
| `UserTask`        | a JSON array; each entry carries an `event` object with `id`, `name` and `type` |
| `Workflow`        | the same for cases |

`type` is `INSERT`, `UPDATE`, `DELETE`, `FOLLOWUP` or `RELOAD`. `id` is the id of the user task or
the case. `RELOAD` has an empty id and means: load the lists of this kind again.

### Who gets an event

Each stream decides for itself, with the same visibility rules the lists use.
A stream gets a change if one of its views lets it through: the widest view of its person, or a view
a list of that person was answered with. A stream also gets a change of an entity its browser
already shows. That is how a browser learns that it has to drop a row its user may no longer see.
So the stream sends nothing the user may not know, and an id from the stream is a reason to ask, not
an answer.

### What a stream remembers

The stream remembers the ids the lists of its person were answered
with, at most `business-cockpit.gui-sse.max-known-ids-per-stream` (2,000 by default). When it has to
forget ids, it sends a `RELOAD`. A new stream always starts with a `RELOAD` for every kind, because
the page may have loaded a list before the stream was open. So treat `RELOAD` as "load the list
again", with the same primary filter, filters and sorting.

### Timing

Changes are collected and filtered every 250 milliseconds
(`business-cockpit.gui-sse.filtering-interval`, `business-cockpit.gui-sse.collecting-interval`).
A browser gets at most one delivery per kind every `business-cockpit.gui-sse.update-interval`
milliseconds, 500 by default, with at most `business-cockpit.gui-sse.max-items-per-update`
entries, 100 by default. What is above that waits for the next time. So a burst of changes becomes
one update instead of hundreds.

### Pings

About 300 milliseconds after connecting comes a `ping`, and then one every 27 seconds.
The first one tells the browser that the request has an answer, so a client that waits for the first
byte stops waiting. The later ones keep the connection open; a stream that carries nothing is closed
on the way.

### When a stream ends

A stream ends when the login it was opened with expires. The browser
connects again, with the cookie it has at that moment, and gets a stream with the rights of the new
login. That is how a change of rights, for example a new substitute, reaches the stream. The stream
itself does not renew the login. When the server shuts down, it closes all streams. So a stream that
closes by itself is normal and no error.

What your user interface does about it:

- When the server ended the stream in order, connect again right away.
- When the connection was lost, connect again with a growing pause, for example 1, 2, 4 seconds, up
  to 15 seconds at most.
- When connecting again answers 401, stop trying and handle it like any other 401: send the
  browser to the login.
- Before every ordinary request, connect a stream that is waiting for its next attempt right away.
  Otherwise a change the request causes falls into the gap.
- Whatever was missed in a gap comes back through the `RELOAD` of the new stream.

## How a row ended

A user task and a case have `endedAt`, the moment they ended, and `endReason`, the reason. Prompt
`1504` adds `endReason` to the case and brings it to the GUI API for both. The values are
`COMPLETED` and `CANCELLED`. An open row has neither field. Data stored before `1504` is converted
by the backend, so your user interface reads `endReason` only and guesses nothing from other
fields.

## User settings

Prompt `1505` gives the user a map `customSettings` and adds endpoints to the GUI API that read and
write one entry of it. Every setting of a user is kept there, so it follows the user into every
browser. Your user interface never keeps a setting in `localStorage`, `sessionStorage` or a cookie
of its own.

`GET /gui/api/v1/app/current-user/settings/{key}` answers the value of one entry, and `PUT` on the
same path stores it. The value is any JSON.

Your user interface talks to these endpoints through one global settings service, see
[behaviour.md](behaviour.md#user-settings). No other code calls them.

## What the cockpit serves

The cockpit is not only an API, it also serves files, and two paths belong to the contract.

Under `/wm/<workflowModuleId>/**` is every registered workflow module. The cockpit forwards the
request to the address the module reported. So two things run over the cockpit's own origin: the
parts of a module and every request a form makes to its own backend. The session cookie travels
along. The routes change while the application runs, because a module registers while the
cockpit is running.

Under `/` is the user interface itself, as a single-page application. The jar of the library brings
none. Your jar puts its static files there, see
[project.md](project.md#the-ui-jar).

`uiUri` of a user task or a case is the `uiUriPath` the workflow module reported, unchanged. The
server builds no address from it and does not read `uiUriType`. Your user interface does two things
with it.

First it builds the address, and that follows one rule:

- A value that starts with `http://` or `https://` is a whole address. Use it as it is.
- Every other value is a path of the workflow module. Put `/wm/<workflowModuleId>/` in front of it,
  because the cockpit serves the module there.

Then `uiUriType` decides what happens with the address. `EXTERNAL` opens it in a window or tab of
its own. `NATIVE_MF` loads the parts of the module from it through native federation. The address
alone never decides this, see
[module-federation.md](module-federation.md#which-data-finds-the-part). `workflowModuleUri` is the
path to the module itself, without a file. It serves the requests of a form to its own backend.
