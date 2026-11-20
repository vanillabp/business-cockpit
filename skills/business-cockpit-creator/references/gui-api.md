# The GUI API

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The GUI API is everything a cockpit user interface gets from the server. It lives under
`/gui/api/v1`. Two OpenAPI files describe it, and these two files are the truth:

- `apis/official-gui-api/openapi/v1.yaml`, the `official-gui-api`: the lists, a user task, a case,
  the workflow modules, the notifications and the user who is logged in, `GET /app/current-user`.
- `business-cockpit/src/main/resources/api/gui/v1.yaml`, the cockpit's own file: `GET /app/info`,
  where the rights of the user come from, the user settings and the update stream.

The rule between the two: the `official-gui-api` holds what the dev shell simulator fakes, and
what the simulator does not offer, such as the settings and the update stream, is in the cockpit's
own file. A workflow module asks the user who is logged in with the generated client of the
`official-gui-api` and the forwarded cookie, for example to tell whether another user claimed a
task.

What is written here is the behaviour behind them, which neither file says.

1. [Generating the client](#generating-the-client)
2. [Protection and login](#protection-and-login)
3. [Staying logged in](#staying-logged-in)
4. [The endpoints](#the-endpoints)
5. [Ended tasks](#ended-tasks)
6. [Giving back](#giving-back)
7. [Reading page by page](#reading-page-by-page)
8. [Sorting](#sorting)
9. [Counting](#counting)
10. [Filtering over reported business data](#filtering-over-reported-business-data)
11. [Suggestions for a search field](#suggestions-for-a-search-field)
12. [The primary filter: which rows come at all](#the-primary-filter-which-rows-come-at-all)
13. [The timestamp that keeps a list stable](#the-timestamp-that-keeps-a-list-stable)
14. [The second form of the same query](#the-second-form-of-the-same-query)
15. [The update stream](#the-update-stream)
16. [How a row ended](#how-a-row-ended)
17. [Suspended tasks and cases](#suspended-tasks-and-cases)
18. [User settings](#user-settings)
19. [What the cockpit serves](#what-the-cockpit-serves)

## Generating the client

Generate the client from the OpenAPI files. Never write it by hand.

The tool is [`@hey-api/openapi-ts`](https://heyapi.dev/openapi-ts/get-started), the generator the
Business Cockpit blueprints use. Use it in the same way. It is a dev dependency of the project, and
`package.json` pins it exactly to `0.99.0`. It is a Node program without install scripts, so the
build needs Node 24 or newer (`"engines": { "node": ">=24" }`) and nothing else: no Java, and no
download while it runs.

`openapi-ts.config.mjs` holds the options, with one job per OpenAPI file. The plugins and their
options are those of the blueprints:

```js
import { defineConfig } from '@hey-api/openapi-ts';

const client = (name) => ({
  input: `build/openapi/${name}.yaml`,
  output: `src/generated/${name}`,
  plugins: [
    // An answer which is not a success throws. The index also exports the client.
    { name: '@hey-api/client-fetch', throwOnError: true, exportFromIndex: true },
    '@hey-api/typescript',
    {
      name: '@hey-api/sdk',
      // One class per tag: the tag `officialTasklist` becomes OfficialTasklistApi.
      operations: { strategy: 'byTags', containerName: '{{name}}Api', methods: 'instance' },
      // A method takes one object with all parameters, the body under the name of its schema.
      paramsStructure: 'flat',
      // A method returns the data of the answer, not the answer.
      responseStyle: 'data',
    },
  ],
});

export default defineConfig([client('official-gui-api'), client('cockpit-gui-api')]);
```

The generated classes and types hide every REST call. A schema becomes a type of the same name, an
operation a method of the class of its tag. The first server of the file, `/gui/api/v1`, becomes
the base URL. Your code creates one instance per class and calls its methods. Nobody writes a
`fetch` call to the GUI API by hand.

The client uses the browser's `fetch`, in every framework, Angular included. So the interceptors of
Angular's `HttpClient` do not see these requests. Use the interceptors of the generated `client`
instead. The generated client throws only the body of an answer which is not a success, without
its status, so add an error interceptor which keeps the `Response`. The user interface needs the
status, for example to tell a 401 from other errors, see [Staying logged in](#staying-logged-in).

Why this tool: the generated classes and types carry the boilerplate, so your own code reads well.
Its version is still 0.x, and a minor version may break the generated code. That is accepted, and
it is the reason for the exact pin. An update is checked by hand.

Generate it at every build, for example in a `prebuild` script, into `src/generated/`, which Git
ignores. It is never committed. The same goes for every dev shell: it generates its own client from
the same files. The plugin `@hey-api/typescript` writes the TypeScript types of the data, and the
types library generates its types with the same tool and the same version, see
[types.md](types.md#data-from-the-gui-api).

How the npm project gets the two files: both ship inside jars of the cockpit. The jars lie in the
Maven repository the cockpit artifacts are deployed to. The interview names that repository and the
Maven coordinates of the cockpit (`Q-COCKPIT-VERSION`, `Q-COCKPIT-REPOSITORY`). By default the
group is `io.vanillabp.businesscockpit`.

|                             File                             |                   Jar                   | Path inside the jar |
|--------------------------------------------------------------|-----------------------------------------|---------------------|
| the `official-gui-api`                                       | `official-gui-api-server-<version>.jar` | `v1.yaml`           |
| the cockpit's own file: `app/info`, the settings, `/updates` | `business-cockpit-<version>.jar`        | `api/gui/v1.yaml`   |

A script downloads the two jars with HTTP `GET` from the path Maven uses,
`<repository>/<group as path>/<artifactId>/<version>/<artifactId>-<version>.jar`. It unpacks the two
files and runs the generator.

- By default the repository is Maven Central. The cockpit deploys its releases and its snapshots
  there. Releases come from `https://repo1.maven.org/maven2/`, snapshots from the snapshot
  repository of Maven Central, `https://central.sonatype.com/repository/maven-snapshots/`. Both
  can be read without a login, so the script needs no token.
- A company repository that mirrors Maven Central works as well, for example Nexus or Artifactory
  (`Q-COCKPIT-REPOSITORY`). If it asks for a login, the script reads the token from the environment
  variable the interview names, never from a file in the repository. If the same company
  repository also serves the npm packages, this can be the token `.npmrc` uses for it.
- A `-SNAPSHOT` version has a time stamp in its file name. The script first reads
  `<version>/maven-metadata.xml` and takes the newest jar it lists.
- Pin a release version where there is one. Then a new cockpit release never changes your client
  without a commit. A snapshot can change under the same version, and Maven Central deletes a
  snapshot about 90 days after it was published.

The other way is the raw file in the `business-cockpit` repository at the release tag, for example
`https://raw.githubusercontent.com/vanillabp/business-cockpit/<tag>/apis/official-gui-api/openapi/v1.yaml`.
That needs no unpacking, but it reads the source and not the released artifact. The jar is the
default.

The update stream `GET /updates` is in the cockpit's own file, together with
the types of its events, `UserTaskEvent` and `WorkflowEvent`. So the generated client brings these
types too. Do not use a generated call for the stream itself, because a stream never ends. Open the
stream with `EventSource`, or with `fetch` and a reader on the body, see
[The update stream](#the-update-stream).

### The base path is configuration

The base path of the GUI API is a configuration value of the generated user interface. Its default
is `/gui/api/v1`, the path of the cockpit. A test or a dev shell may point it somewhere else, for
example to `/official-api/gui/api/v1` of a simulator, where each simulated system has a prefix of
its own, see [dev-shell.md](dev-shell.md#the-dev-shell-simulator).

Read the value once at start and set it on both generated clients, with `setConfig({ baseUrl })`
of their `client`. The update stream takes the same base path. No other code of the user interface
names `/gui/api/v1`.

## Protection and login

Which paths stay open depends on where the user interface runs (`Q-HOSTING`, see
[`../SKILL.md`](../SKILL.md#where-the-user-interface-runs)).

With `embedded` and `behind` the login comes first. `index.html` and every deep link ask for a
login, so a browser without a login goes straight to the login of the backend, and the user
interface starts after it. The open matchers of the library stay open, as
[backend.md](backend.md#the-login) says: the proxy to the workflow modules under `/wm/**`, because
a module protects itself, and the files below `/assets/` and `/static/`. These files are the
bundle, with hashed names and without data of the organisation, and without `index.html` nothing
loads them. Do not close them in the chain: the matchers are the contract with the library.

With `in-front` the single-page application loads without a login and logs in afterwards, when
needed. So these stay open:

- the static files of the user interface,
- the shell, `index.html`, also as the answer to an unknown path,
- `GET /gui/api/v1/app/info` and `GET /gui/api/v1/app/current-user`,
- the proxy to the workflow modules under `/wm/**`. A module protects itself.

Everything else asks for a login. Login is the job of the backend application, not of the user
interface, see [`../SKILL.md`](../SKILL.md#how-your-user-interface-gets-into-the-cockpit). The
`business-cockpit` library sets no rules for it. The application decides which paths are open, and
the generated user interface never asks for a change of the library. The security chains of the
runnable `container` are an example of such an application, see
[rough-edges.md](rough-edges.md#what-needs-a-newer-cockpit).

A login answers with a cookie, and from then on the cookie travels with every request. The cookie
holds a JWT. How it is signed, how long it lives and how an identity provider is put in front is in
the wiki under [Security](https://github.com/vanillabp/business-cockpit/wiki/Security).

What follows for the start of the user interface:

1. `GET /gui/api/v1/app/info` fetches the title, the version and the build time of the
   application. It needs no user, so the frame can stand before anybody is logged in.
2. `GET /gui/api/v1/app/current-user` returns the user who is logged in, with their groups. If
   nobody is logged in, the answer is 200 with no body. That is the contract of the OpenAPI file,
   not an error: check for an empty body, not for a status. Then send the browser to the login of
   the backend (`Q-LOGIN`). The generated client has this call in the class
   `OfficialCurrentUserApi`, from the tag `officialCurrentUser`.
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

|         Base path          |                      What the list shows                       |
|----------------------------|----------------------------------------------------------------|
| `/gui/api/v1`              | everything the user may work on                                |
| `/gui/api/v1/current-user` | only what is assigned to the user personally                   |
| `/gui/api/v1/user-roles`   | what the user's groups may claim and nobody is assigned to yet |

Do not take these three paths as given. They are a choice of the runnable cockpit. An application of
the organisation's own may offer other lists, and then the paths have other names. `/tasks` uses
the first base path, and `/my-tasks` the second one, or the first with a filter on `assignee.id`.

The endpoints, all relative to the base path:

|                                   Call                                   |                                                  What for                                                  |
|--------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| `POST /usertask`                                                         | one page of the task list                                                                                  |
| `PUT /usertask`                                                          | the same query as an update, see below                                                                     |
| `OPTIONS /usertask`                                                      | suggestions for a search field over user tasks                                                             |
| `GET /usertask/{id}`                                                     | one user task, with `markAsRead` also marked as read                                                       |
| `PATCH /usertask/{id}/claim`, `PATCH /usertask/claim`                    | set the assignee, with `unclaim=true` give it back; 409 for an ended task, 400 for a task of another user  |
| `PATCH /usertask/{id}/assign`, `PATCH /usertask/assign`                  | add a person as candidate user, with `unassign=true` remove them; 409 for an ended task                    |
| `PATCH /usertask/{id}/mark-as-read`, `PATCH /usertask/mark-as-read`      | mark as read, with `unread=true` take it back                                                              |
| `PATCH /usertask/{id}/follow-up-date`                                    | set or delete the follow-up date                                                                           |
| `PATCH /usertask/{id}/note`, `PATCH /usertask/note`                      | set, change or clear the note of a user task or of several                                                 |
| `PATCH /workflow/{id}/note`, `PATCH /workflow/note`                      | set, change or clear the note of a case or of several                                                      |
| `POST /usertask/count`, `POST /workflow/count`                           | the number of rows of several lists, for the counts                                                        |
| `POST /user`                                                             | search users, to propose a task to a person                                                                |
| `POST /workflow`                                                         | one page of the case list                                                                                  |
| `PUT /workflow`                                                          | the same query as an update                                                                                |
| `OPTIONS /workflow`                                                      | suggestions for a search field over cases                                                                  |
| `GET /workflow/{id}`                                                     | one case                                                                                                   |
| `POST /workflow/{id}/usertasks`                                          | the user tasks of a case                                                                                   |
| `GET /workflow-module`, `GET /workflow-module/{id}`                      | the workflow modules the user may see                                                                      |
| `GET /notifications/media`, `/config`, `/recipient-config`, `/workflows` | the notification settings of the user; `PUT` on `/config` and `/recipient-config/{mediumType}` stores them |
| `GET /app/info`, `GET /app/current-user`                                 | the application and the user                                                                               |
| `GET /app/current-user/group-hierarchy`                                  | where each group and right of the user comes from, see `behaviour.md`                                      |
| `GET /app/current-user/settings/{key}`, `PUT` on the same path           | read and write one entry of `customSettings`                                                               |
| `GET /updates`                                                           | the update stream, see below                                                                               |

The last four rows are in the cockpit's own file, all the others in the `official-gui-api`. They
exist only under `/gui/api/v1`, not under the other two base paths.

The two forms of each action, one for one task and one for a list of ids, do the same. The list
form is there so that several selected rows do not cause twenty requests. For an ended task both
forms answer 409, see [Ended tasks](#ended-tasks).

Take the details of the note and count endpoints from the OpenAPI file of the pinned cockpit
version, not from this text. The path of the user settings is fixed, see
[User settings](#user-settings). For counts see [Counting](#counting).

`GET /workflow-module` answers the modules the user may see, each with `id`, `version` and `uri`.
`uri` is `/wm/<id>`, the path of the module behind the cockpit's proxy, or empty if the module
registered no address. It carries no `uiUriType`. Your user interface calls it once, when it
starts, and keeps the answer in memory for all its parts. One central loader then fetches the
parts of the modules from these addresses. No list and no page calls this endpoint itself,
see [module-loading.md](module-loading.md#one-registry-and-one-loader).

`GET /app/info` also returns `additionalProperties`, a free map. The backend fills it from
`business-cockpit.additional-properties`. Use it for what the user interface needs to know about
the installation, for example the languages it offers (`Q-LANGUAGES`):

```yaml
business-cockpit:
  additional-properties:
    languages: en,de
```

Write the languages as one string, separated by commas, and split it in the user interface. Do not
write a YAML list. The cockpit binds `additional-properties` as a map of objects, and Spring Boot
turns a list there into a map with the keys `0`, `1` and so on. So `GET /app/info` would return an
object, not an array, and the order of its keys is not certain.

## Ended tasks

A user task that has ended is a record: it shows with which input it was completed. The server
refuses every change to it with 409 and writes nothing: claim, unclaim (`claim?unclaim=true`),
assign, unassign, the follow-up date and the note. The same holds for the note of a case that has
ended. The list form is all or nothing. If one of the tasks this user may see has ended, the request
changes none of them and answers 409. Ids this user may not see are still left out without a word.

The answers come in this order: a task this user may not see, or one that does not exist, gets 404.
Only a task the user may see and which has ended gets 409.

Mark as read and mark as unread still work for an ended task, for one task and for a list, and so
does `GET /usertask/{id}?markAsRead=true`. "Read" only switches off the bold row in the list.

The 409 of claim and unclaim carries a plain text body which says that the task has ended.

So switch these actions off in your user interface when `endedAt` is set, and treat a 409 as "the
task ended or was suspended in the meantime": read the task again, redraw the row and tell the user,
see [Suspended tasks and cases](#suspended-tasks-and-cases).

## Giving back

Only the assignee can give a task back. `claim?unclaim=true` for a task which another user claimed
answers 400 and writes nothing. The list form is all or nothing: if another user claimed one of the
tasks this user may see, none of them is given back, and the answer is 400. Ids this user may not
see are still left out without a word. A task without an assignee answers 200, and nothing
changes.

The answers come in this order: 404 for a task this user may not see or which does not exist, 409
for a task which has ended, then 400. The 400 carries a plain text body which says that another
user claimed the task. It names no user.

So offer "give back" only for the user's own tasks. If a 400 comes anyway, somebody else claimed
the task in the meantime: read the task again, redraw the row and tell the user.

The forms of a workflow module answer a task another user claimed with 409, not 400. That is the
module's answer to sending a form, not the cockpit's answer to giving a task back.

After giving a task back, the server reads the task again without checking the visibility, and
returns it. That is on purpose. A task that is given back often disappears from the very list it
was given back from, and the user interface still needs it to redraw the row.

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

The server answers as many requests as you send, but the browser does not send them all at once.
Over HTTP/1.1 it opens six connections per host, and every other request waits. A drag of the scroll
bar that asks for every page it passes queues dozens of requests, and the pages the user stops at
come last. So ask only for the pages the list draws, each one once, see
[behaviour.md](behaviour.md#which-pages-load).

The answer carries a `page` with `number`, `size`, `totalPages` and `totalElements`.
`totalElements` is counted exactly, in a second query over the same conditions. So the list knows
its own total from every page. It shows this total also when it lists fewer rows because of
`Q-MAX-LIST-ENTRIES`, see [behaviour.md](behaviour.md#the-longest-list-q-max-list-entries). Every other number comes from the count endpoint, see
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
with the page. Every other count comes from the count endpoint. It takes
the same conditions as the list (base path, primary filter, filters, `initialTimestamp`) and
answers a number, without rows and without sorting. Counting fast, with an index, is the job of
the backend. Never ask for a page only to read its `totalElements`.

## Filtering over reported business data

Each `SearchQuery` has three fields and becomes one condition:

```json
{ "path": "details.customer.name", "query": "Meier", "caseInsensitive": true }
```

`path` is the path in the document, the same as for `sort` and the same as `path` of a column. If
it is missing, the server searches two fields, and a row matches when one of them does. The first is
`detailsFulltextSearch`, the full text the workflow module has put together itself, see
[Reporting workflows and user tasks](https://github.com/vanillabp/business-cockpit/wiki/Reporting-workflows-and-user-tasks).
The second is `note`, the note the users keep on the user task or case. Nothing else is searched:
what was neither reported nor written into a note cannot be found.

The same rule as for a sort path holds for `path`, see [Sorting](#sorting): a field of the list or
a key below `details.`. The server refuses every other path with 400, for example
`The request is not valid: 'searchQueries.path' may only name fields of the list or keys below
'details.'. A part of a path holds letters, digits and '_', and below 'details.' also '-'.` So a
field the API does not show cannot be reached by a filter either.

`query` is plain text. The server escapes it itself, so a `.`, a `*` or a `(` finds exactly that
character. Send the user's input as it was typed, and do not escape it: an escaped input would
search for the backslashes too. The text matches anywhere in a value, also in the middle. On a large
list that is slow, see
[rough-edges.md](rough-edges.md#a-search-in-the-middle-of-a-value-uses-no-index-well).

`caseInsensitive` switches on the option `i`. Otherwise upper and lower case are told apart.

Entries with different paths are joined with AND. Entries with the same `path` are meant to be
joined with OR, but the server does not do that yet. Today the list and the count answer 500 for
two entries with the same `path`, and the suggestions for a search field join them with AND. Two
entries without a `path` count as the same path. So send one entry per path for now. When the user sets a filter on a path, remove the old entry for
that path first, see [rough-edges.md](rough-edges.md#two-filters-on-one-path).

A field that looks like a filter and is not one: `businessIds` of `WorkflowsRequest` is an exact
match on the field `businessId` and not a search pattern. `POST /workflow`, `PUT /workflow` and
`POST /workflow/count` all take it in the same way.

`workflowModuleIds` is an exact match as well. Every list takes it, for user tasks and for cases, in
`POST`, in `PUT`, in the count and in the suggestions for a search field. The list then shows only
the rows of these workflow modules. Without the field, or with an empty list, it shows the rows of
all modules. The field only narrows what the user may see anyway. It is meant for a page that
belongs to one application and shows only its user tasks and cases.
`POST /workflow/{workflowId}/usertasks` does not read it.

## Suggestions for a search field

`OPTIONS /usertask` and `OPTIONS /workflow` return words and how often they occur. This is called
"keyword in context", KWIC for short. A user interface uses them for the suggestions below a search
field.

The query takes `query` and, if wanted, `path` as parameters, and the filters already set as
`searchQueries` in the body, and for a list limited to workflow modules also `workflowModuleIds`.
It answers with whole words that contain `query`, each with a count.
`path` and the paths in `searchQueries` follow the same rule as a sort path, otherwise the server
answers 400. For `path` the message names `'path'`.

What you need to know:

Below three characters the answer is empty, and it is no error. So let the field ask only from
three characters on.

The search is always case-insensitive here, whatever a `SearchQuery` says. `query` is plain text
here as well, so do not escape it either.

The answer has at most 21 entries. They are the 21 most frequent words, the most frequent first.
For the same count they are in alphabetical order, upper case before lower case. Show them in this
order. When 21 entries come, there may be more. Whether you also show a note then is up to you.

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

|   `mode` of a task list    |                       What comes                       |
|----------------------------|--------------------------------------------------------|
| `All`                      | everything, open and closed                            |
| `OpenTasksWithFollowUp`    | everything open, with and without a follow-up date     |
| `OpenTasksWithoutFollowUp` | open, without a follow-up date or with one that is due |
| `OpenTaskOnlyFollowUp`     | open, with a follow-up date in the future              |
| `ClosedTasksOnly`          | only what has an end                                   |

The mode `OpenTasks` is gone. It returned the same rows as
`OpenTasksWithFollowUp`. So the open modes are a pair, with and without follow-up, plus the
follow-ups alone.

| `mode` of a case list |                What comes                 |
|-----------------------|-------------------------------------------|
| `All`                 | everything                                |
| `InProgress`          | what has no end, suspended cases included |
| `Done`                | what has an end                           |

A suspended case has not ended. So it is in progress, and `InProgress` shows it.

Without `mode` the server takes its own default:
`OpenTasksWithFollowUp` for a task list and `InProgress` for a case list, for `POST` and `PUT` alike.
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

For `PUT`, `page.number` is always 0 and `page.size` is the number of rows returned. The filters are
the same as for `POST`: `searchQueries`, `mode`, `workflowModuleIds` and, for cases, `businessIds`.
Send them just as for the first page. Otherwise the update returns rows that do not belong to the
list.

`PUT` has no offset. It always starts at the first row. That fits a window of loaded rows at the top
of the list. For a window further down, `PUT` would return every row above it in full, so read the
pages of the window again with `POST` and the same `initialTimestamp` instead, see
[rough-edges.md](rough-edges.md#the-update-form-has-no-offset).

## The update stream

`GET /gui/api/v1/updates` is a `text/event-stream` (server-sent events). There is one stream per
browser tab. An event in it is a wake-up call. It says exactly which entity it is about: the name of
the event is the kind (`UserTask` or `Workflow`), `event.id` is the id of that user task or case,
`event.type` says what happened, and `workflowModuleId` names its workflow module. The new values
are not in it. Your user interface reads the entity again.

The path is in the cockpit's own file, so the types of the events come from the
generated client. The connection does not: open it with `EventSource`, or with `fetch` and a reader
on the body. A generated call waits for the end of the answer, and a stream has none.

What comes through it:

| Name of the event |                                      Data                                       |
|-------------------|---------------------------------------------------------------------------------|
| `ping`            | an object with `id` and `name`, nothing else                                    |
| `UserTask`        | a JSON array; each entry carries an `event` object with `id`, `name` and `type` |
| `Workflow`        | the same for cases                                                              |

`type` is `INSERT`, `UPDATE`, `DELETE`, `FOLLOWUP` or `RELOAD`. `id` is the id of the user task or
the case. `RELOAD` has an empty id and means: load the lists of this kind again.

The `event` object of `UserTask` and of `Workflow` also says who made the change:

|      Field      |                                              Meaning                                              |
|-----------------|---------------------------------------------------------------------------------------------------|
| `initiatorKind` | `USER` (a person), `SYSTEM` (reported by the workflow module) or `COCKPIT` (a job of the cockpit) |
| `initiator`     | the id of the user, a string; set when `initiatorKind` is `USER`                                  |

Take the initiator from the event, not from the object read again afterwards. A second change
between the event and the answer can have changed the initiator of the object. When one batch holds
several changes of the same user task or case, each of them names who made that change. Only on Azure
Cosmos DB for MongoDB do they all name the initiator of the last one. `RELOAD`, and
an event for a row the cockpit no longer holds, carry neither field. Marking a user task as read or
unread, also by opening it, is no change of the task, and no event comes for it. See
[behaviour.md](behaviour.md#live-updates) for the lists and
[behaviour.md](behaviour.md#changes-by-others) for an open page.

Next to `event`, each entry carries `workflowModuleId`, the workflow module of the user task or the
case. A list that shows only some modules skips the events of the others with it. The stream itself
is not limited to modules, because a request does not say which tab it comes from. When the field is
missing, the cockpit no longer knows the module, for example because it no longer holds the row.
Such an event can concern any list, so treat it like one of your own modules.

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

A user task and a case have `endedAt`, the moment they ended, and `endReason`, the reason. The
values are `COMPLETED` and `CANCELLED`. An open row has neither field. Data stored by an older
cockpit, without `endReason`, is converted by the backend, so your user interface reads `endReason`
only and guesses nothing from other fields.

## Suspended tasks and cases

The BPMS can suspend a process instance and activate it again later. A user task of a suspended
process instance carries `suspendedAt`, the moment of the suspension, and `suspendedBy`, who
suspended it, where the BPMS names somebody. A case whose own process instance is suspended
carries the same two fields. A suspended called process does not make its case suspended. When the
process instance is activated again, both fields are gone. The change comes as an ordinary update
event on the update stream, so a list and an open page read the row again.

A suspended task stays in every list, and a suspended case is in `InProgress`. Both lists can be
sorted and filtered by `suspendedAt` like by any other field.

`endedAt` decides first. A task or a case that was cancelled while it was suspended keeps
`suspendedAt` after its end. So it is suspended only while `suspendedAt` is set and `endedAt` is
not.

The server refuses claim, unclaim, assign and unassign (propose and remove candidate) for a
suspended task with 409 and writes nothing, as for an ended task. The list form is all or nothing:
one suspended task among those the user may see, and none is changed. This holds from the cockpit
version with that check on. The follow-up date, the note and mark as read and unread still work. The
BPMS refuses to complete a suspended task, and the module's form gets that answer from its own
backend, not from the cockpit. See [behaviour.md](behaviour.md#suspended-tasks) for what the user
interface does.

## User settings

The cockpit gives the user a map `customSettings` and has endpoints that read and write one entry
of it. They are in the cockpit's own file, `api/gui/v1.yaml` in the `business-cockpit` jar, next to
`app/info`. Every setting of a user is kept there, so it follows the user into every
browser. Your user interface never keeps a setting in `localStorage`, `sessionStorage` or a cookie
of its own.

`GET /gui/api/v1/app/current-user/settings/{key}` answers the value of one entry, and `PUT` on the
same path stores it. The value is any JSON.

Your user interface talks to these endpoints through one global settings service, see
[behaviour.md](behaviour.md#user-settings). No other code calls them. The dev shell simulator answers
the current user but has no settings, so in a dev shell the service keeps the settings in memory
only.

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
its own. `ES_MODULES` reads the module's manifest from it and loads its parts with `import()`. The
address alone never decides this, see
[module-loading.md](module-loading.md#which-data-finds-the-part). `workflowModuleUri` is the path
to the module itself, without a file. It serves the requests of a form to its own backend.
