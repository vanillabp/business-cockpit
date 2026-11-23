# The rough edges

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Here is what brings a generated user interface down if nobody mentions it. Most points come from the
code of the cockpit, the last ones from libraries and tools a cockpit is built with. Read this
before the first screen, not when something does not work.

1. [Assigning makes a candidate](#assigning-makes-a-candidate)
2. [Claiming overwrites](#claiming-overwrites)
3. [Bulk actions do not say which ids they skipped](#bulk-actions-do-not-say-which-ids-they-skipped)
4. [A search in the middle of a value uses no index well](#a-search-in-the-middle-of-a-value-uses-no-index-well)
5. [Two filters on one path](#two-filters-on-one-path)
6. [An event names the entity, not its new values](#an-event-names-the-entity-not-its-new-values)
7. [`filterable` is not read by the server](#filterable-is-not-read-by-the-server)
8. [A module may report a type of another user interface](#a-module-may-report-a-type-of-another-user-interface)
9. [The update form has no offset](#the-update-form-has-no-offset)
10. [The bundler rewrites `import()`](#the-bundler-rewrites-import)
11. [`zone.js` in a module slows every part down](#zonejs-in-a-module-slows-every-part-down)
12. [A data entry pulls in the framework](#a-data-entry-pulls-in-the-framework)
13. [A shared chunk carries the framework into a data entry](#a-shared-chunk-carries-the-framework-into-a-data-entry)
14. [Vue in a shadow root](#vue-in-a-shadow-root)
15. [Angular in a shadow root](#angular-in-a-shadow-root)
16. [Removing a web component before its answer](#removing-a-web-component-before-its-answer)
17. [A component library keeps destroyed rows](#a-component-library-keeps-destroyed-rows)
18. [What needs a newer cockpit](#what-needs-a-newer-cockpit)
19. [The JDK client and the Vite dev server](#the-jdk-client-and-the-vite-dev-server)
20. [An open update stream keeps Playwright waiting](#an-open-update-stream-keeps-playwright-waiting)
21. [A MUI Snackbar closes on a click elsewhere](#a-mui-snackbar-closes-on-a-click-elsewhere)
22. [A toast outside a modal dialog cannot be clicked](#a-toast-outside-a-modal-dialog-cannot-be-clicked)

## Assigning makes a candidate

`PATCH /usertask/{id}/assign` does not set the assignee. It adds the person to the candidate users.
With `unassign=true` the person is removed again.

The assignee is set by `claim`. A user interface that builds a button "assign" and expects an
assignee builds something other than what the user reads. So name the action after what it does:
"propose to" a person.

## Claiming overwrites

`claim` sets the assignee, even if somebody else is already there. The server does not ask and
returns no conflict. A user interface that offers to claim without asking lets a user take a task
out of a colleague's hands. Ask first when the task already has another assignee.

Claiming a task you already have changes nothing and answers 200.

## Bulk actions do not say which ids they skipped

The form with a list of ids leaves out the ids this user may not see, without a word. After a bulk
action you do not know whether every id was changed. Ended tasks are not among the silent cases:
they make the whole request fail with 409, see [gui-api.md](gui-api.md#ended-tasks). Neither is
giving back a task of another user: that makes it fail with 400, see
[gui-api.md](gui-api.md#giving-back).

The single form answers 404, both for a task that does not exist and for one this user may not see.
That is on purpose: the answer must not reveal that the task exists. So write nothing into your
message that does reveal it.

## A search in the middle of a value uses no index well

`query` of a `SearchQuery` is plain text, and it matches anywhere in a value. The server escapes it
and turns it into a regular expression without an anchor. Your user interface sends the input as it
was typed. On a large list such a search reads many documents and takes time.

MongoDB offers no good search for this. A field is either searched with a regular expression, which
cannot use an index well for a match in the middle of a value, or with a full-text index, which
matches whole words and has one index per collection. Better ways are being worked on separately.

## Two filters on one path

Conditions on different paths are joined with AND. Two conditions on the same path are meant to be
joined with OR, but the server does not do that yet. The list and the count answer 500, and the
suggestions for a search field join the two with AND. Your user interface does not offer it for
now: when the user sets a filter on a path, remove the old entry for that path first, then add the
new one.

By intent this is no error, and a later version joins the two with OR.

## An event names the entity, not its new values

An event of the update stream is a wake-up call. It names the entity exactly: the kind, the id of
the user task or case, its workflow module, the type of change and who made it. It does not carry
the new values. Never show anything of the entity from an event directly.
Ask the server, and take an empty answer as an answer: the row may have left the user's view.

Who made the change is in the event, in `initiator` and `initiatorKind`. Take it from there, not
from the user task or case read again afterwards: a second change between the event and the answer
can have changed the initiator of the object. The lists use it to leave the user's own changes
unmarked, and the open page of a task or a case uses it to tell the user about changes by others,
see [behaviour.md](behaviour.md#live-updates) and [behaviour.md](behaviour.md#changes-by-others).

Each stream only gets events its user may know of, or events about rows its browser already shows.
A `RELOAD` with an empty id means "load the lists of this kind again". It comes at the start of every
stream and whenever the stream had to forget ids. See
[gui-api.md](gui-api.md#the-update-stream).

## `filterable` is not read by the server

The field is in every column. The server does not know columns, so it never reads it. Whether a
column offers a filter is decided by your user interface from this flag.

## A module may report a type of another user interface

VanillaBP knows no names for `ui-uri-type`. It requires the value and accepts any string. A workflow
module built for an older or another cockpit user interface keeps reporting the name that user
interface understood. Your user interface does not know it. Show the row, and tell the user that it
cannot be opened here. Tell the module developers to use the constants of your types jar, see
[project.md](project.md#the-types-jar).

## The update form has no offset

`PUT /usertask` and `PUT /workflow` always start at the first row. There is no `pageNumber`. For a
window of loaded rows further down the list, a `PUT` would have to ask for every row above the
window, and every row it does not know comes in full. That is the memory problem the window is
there to avoid. So read the pages of such a window again with `POST`, with the same
`initialTimestamp`. Use `PUT` only while the window starts at the top. See
[behaviour.md](behaviour.md#live-updates).

## The bundler rewrites `import()`

The loader calls `import()` with an address it only knows at runtime. The cockpit's bundler sees the
call at build time and tries to resolve it. Vite warns and may rewrite it, Webpack builds a context
of files that matches nothing. Mark the call as one to leave alone: `/* @vite-ignore */` for Vite,
`/* webpackIgnore: true */` for Webpack. Then the browser runs the plain `import()`. See
[module-loading.md](module-loading.md#one-registry-and-one-loader).

## `zone.js` in a module slows every part down

`zone.js` patches timers, promises and events for the whole page, not for one module. An Angular
module that brings it changes the timing of the cockpit and of every other module. A second Angular
module with its own copy patches everything once more. So no module and no cockpit loads `zone.js`,
and Angular runs zoneless, see [module-loading.md](module-loading.md#nothing-is-shared).

## A data entry pulls in the framework

One import is enough. A column function that imports a date helper from a file that also renders a
React component can bring React along, when the bundler cannot prove that the rest of that file has
no side effects. Then the task list loads the framework of every module before it draws a row. The
build of the module notices it with `bc-check-entries`, see
[module-loading.md](module-loading.md#data-entries-carry-no-framework). Keep the code of data
entries in files of their own, and mark the module's package with `"sideEffects": false` where that
is true.

The function of `UserTaskForm` is the likeliest place for this. It picks the form of a user task,
so it feels natural to import the forms there. Do not. It names the visual entry and the web
components as strings, and the forms stay in their own visual entries.

## A shared chunk carries the framework into a data entry

esbuild with `--splitting`, and Rollup or Vite as well, move code that two entries use into a
shared chunk. This is how the entries of one module share their framework. But the bundler decides
what goes into which chunk, not you. When a data entry and a form both use a small helper, the
helper may land in the same chunk as the framework. Then the data entry imports that chunk, and the
framework comes along, although the source of the data entry never imports it.

`bc-check-entries` finds it, because it follows every file a data entry imports, chunks included.
The way out is the same as above: data entries use no file that a visual entry uses. If that is not
possible, build the data entries in a build of their own, without splitting. The kit's example
modules build with splitting and pass the check, because their data entries share nothing with
their forms.

## Vue in a shadow root

Vue needs two things that React does not. The Vue blueprint `cockpit-user-task-frontend-vue` shows
both.

A `<style>` block of a single-file component does not reach the shadow root. The usual Vue build
puts it into the `<head>` of the page, so the component shows without its styles. Write no `<style>` block. Keep the module's style in a file of
its own, and let the web component write it into its own shadow root, in a `<style>` element in
front of the component. The blueprint's build fails on a `<style>` block, so
nobody adds one by mistake. With Vite, importing a stylesheet with `?inline` gives the same text.

`vue-tsc` with `strictTemplates` rejects every `data-testid`. That option turns on
`checkUnknownProps`, and the types of Vue for the DOM know no `data-*` attribute. So the type check
fails on the test ids the browser tests need. Leave `strictTemplates` off and turn on
`checkUnknownComponents`, `checkUnknownDirectives` and `checkUnknownEvents` in
`vueCompilerOptions` instead. A misspelled component, directive or event is still an error.

## Angular in a shadow root

Angular needs a few things that React does not. The Angular example module of the kit
(`examples/angular-module`) shows all of them.

Angular writes the styles of a component, `styles` or `styleUrl`, into the shadow root which holds
the host element of the component. So components may keep their styles as in any Angular project,
and the styles reach neither the cockpit nor another module. With the default encapsulation this
only works when the host element is in the shadow root before `createComponent` runs. A host element
which is not in the page yet sends the styles into the `<head>` of the page: the component shows
without them, and they reach the cockpit. Measured with Angular 22.2, with the default encapsulation
and with `ViewEncapsulation.ShadowDom`, also with several web components at once, after one was
removed and after one was moved.

One component created outside the page is enough to break this for the whole module. Angular
remembers every place it has written styles to, and writes each style it uses into all of them. A
component created through a `ViewContainerRef`, for example with `NgComponentOutlet`, gets a new
host element which is not in the page yet, so from then on every style of the module goes into the
`<head>` as well. The children of a template are not affected. A module which creates components
that way gives its components no styles. It writes its style into each shadow root itself, in a
`<style>` element in front of the component, as the Angular blueprint
`cockpit-user-task-frontend-angular` does. A browser test which counts the `<style>` elements of the
page finds such a leak.

`@angular/elements` does not fit the event protocol. It defines a web component class of its own,
so the module would have to build the protocol a second time: the functions in the properties, the
end of all subscriptions and the `DISCONNECTED` answers when the web component leaves the page.
Use `defineBcElement` instead, start one Angular application per module with `createApplication`
and `provideZonelessChangeDetection()`, and create the component in the shadow root with
`createComponent` and `hostElement`. Hand it all properties of the cockpit as one input, for
example `props`. Call `detectChanges()` once after the first `setInput`, so the component draws
completely at once and shows no empty text. After that, signals and the events of the template tell
Angular when to draw again.

The build needs Angular's own compiler. esbuild alone cannot compile Angular components. The
Angular compiler compiles them ahead of time into plain JavaScript, so the browser gets no template
compiler. Angular also publishes its own packages half compiled, and the Angular linker finishes
them while the bundler runs. The Angular CLI does both by itself, so build Angular entries with
it, see [module-loading.md](module-loading.md#angular-entries-with-the-angular-cli). Do not put the
compiler and the linker into an esbuild build of your own. It works, but it is a way nobody else
goes, and its bundle was three times as large in a measurement. The data entries are plain
JavaScript and need neither.

## Removing a web component before its answer

A web component that leaves the page rejects its open requests with `DISCONNECTED`. The dialog is
the trap. Its web component calls `cockpit.closeDialog`, and the obvious thing is to remove the
dialog right away. Then the request of `closeDialog` fails with `DISCONNECTED`, although the cockpit
did what was asked. Answer first, and remove the dialog in a later task, for example with
`setTimeout`. The same holds for any action whose effect removes the web component that asked. See
[module-loading.md](module-loading.md#timeout).

## A component library keeps destroyed rows

A component that adds a listener and later removes a different one never lets go. In a virtual
list that bug costs a whole row each time a row leaves the page. PrimeNG is an example. Up to
20.4.0, and in 21.0.x, its `BaseComponent` adds `callback.bind(this)` to the global `ThemeService`
and removes the unbound method when it is destroyed. The two references never match. So every
PrimeNG component in a row, with its view and its DOM, stays reachable. PrimeNG 20.5.1-lts and 21.1
fix it. Until a fixed version can be used, a patch at the start of the application helps, if it
removes exactly the bound handler that was added.

Reading the code does not find this kind of bug. The measurement in
[behaviour.md](behaviour.md#measuring-a-list) does: the heap grows with the distance scrolled, and
the retaining path of a detached DOM node leads to the library. See
[behaviour.md](behaviour.md#memory-that-comes-back).

## What needs a newer cockpit

This description counts on features of the `business-cockpit` repository that came with version
`1.0.0`. The cockpit version of the interview (`Q-COCKPIT-VERSION`) must be `1.0.0` or later. Until
`1.0.0` is released, build the snapshot of the branch `main`, with the version its `pom.xml` names,
and write into the report that the project moves to `1.0.0` once it is released. That move changes
the version property and drops the snapshot repository of Maven Central (`BE-024`), nothing else.
It is a step of the plan, not a question for the user. Version `1.0.0` brings:

- `endReason` on user tasks and cases, so a cancelled row can be told from a completed one,
- `customSettings` on the user, read and written one entry at a time with `GET` and
  `PUT /app/current-user/settings/{key}`, in the cockpit's own file `api/gui/v1.yaml`,
- the count endpoints `POST /usertask/count` and `POST /workflow/count`,
- notes on user tasks and cases,
- answers 409 for every change to an ended user task: claim, unclaim, propose, remove candidate,
  follow-up and note, the list form all or nothing; mark as read and unread stay allowed, see
  [gui-api.md](gui-api.md#ended-tasks),
- answers 400 for giving back a task which another user claimed, the list form all or nothing,
  and a plain text body which says why for this 400 and for the 409 of claim and unclaim, see
  [gui-api.md](gui-api.md#giving-back),
- the open modes of a task list `OpenTasksWithFollowUp` and `OpenTasksWithoutFollowUp`, without
  `OpenTasks`; without `mode` the server takes `OpenTasksWithFollowUp` for user tasks and the open
  cases for cases,
- the modes of a case list `All`, `InProgress` (not ended, suspended cases included) and `Done`
  (ended),
- `GET /updates` and the types of its events in the cockpit's own file `api/gui/v1.yaml`, so these
  types are generated too,
- `initiator` and `initiatorKind` in each event of `GET /updates`, who made the change,
- `GET /app/current-user/group-hierarchy` for the overview of the rights in the menu, see
  [behaviour.md](behaviour.md#where-the-data-comes-from),
- a library that brings no login: the application writes the security chains and
  `userDetailsProvider`,
- the user directories for Keycloak and LDAP in the library, switched on by
  `business-cockpit.user-directory.type`,
- a token without groups: the cockpit asks `UserDirectoryService#getUser` on every request, and an
  OIDC login gets a token,
- the client of the gateway: it speaks HTTP/1.1 and passes a compressed answer of a workflow module
  or a dev server on as it is, with its `Content-Encoding` (`EXT-059`).

Against an older cockpit the generated client lacks these endpoints and fields, and the build
fails. That is on purpose: better a failed build than a user interface that guesses.

The last four points matter for a backend the skill creates, not for the user interface. Since
`1.0.0` the library brings no login: the two security chains and the bean `userDetailsProvider` are
the application's, and the generated backend writes all three itself, see
[backend.md](backend.md#the-login). An older library has a BPMS API chain of its own under the same
bean name, and the application would not start next to it. With an older library the token also
carries the groups of the login, and the mapper cannot handle the principal of an OIDC login, so
that login gets no token at all. A library older than `1.0.0` answers
`GET /gui/api/v1/app/current-user/group-hierarchy` with 404, and the overview cannot show where a
right comes from.

The OIDC login of [backend.md](backend.md#oidc-the-default) follows the wiki page
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security) and the module `container`,
which tests it since `1.0.0`. A chain which was copied from the wiki before writes no cookie: its
`oauth2Login(login -> login.securityContextRepository(..))` has no effect, see
[backend.md](backend.md#oidc-the-default). The integration test of the login in the generated
backend repository is still the proof for the backend itself (`BE-007`).

## The JDK client and the Vite dev server

The backend passes the paths it does not know to the dev server of the user interface, in the
profile `ui-dev-server`, see [backend.md](backend.md#the-route-to-the-user-interface). Against a
real Vite dev server this can fail, while a test with a small HTTP server passes.

- Vite listens on `localhost`. Node.js may resolve that to the IPv6 address `::1` only, and Java
  tries `127.0.0.1` first, so the backend gets no connection. Set `server.host: '127.0.0.1'` in
  Vite, and name `http://127.0.0.1:<port>` in the backend.
- The JDK `HttpClient` asks for HTTP/2 over a plain connection, with the header `Upgrade: h2c`.
  Vite does not answer that upgrade the way the client expects, and the request fails or hangs.
  The client of the gateway which the library brings speaks HTTP/1.1 only, so this happens only
  with a library before `1.0.0`, or with an application which replaces that client by a bean of
  the type `ProxyExchange` of its own.

## An open update stream keeps Playwright waiting

`page.waitForLoadState('networkidle')` waits until no request has been open for half a second. The
update stream is a request that stays open as long as the page lives. So `networkidle` never comes
on a page of the cockpit, and the test runs into its timeout. Wait for what the user sees instead,
for example with `expect(locator).toBeVisible()` on the first row of the list, or wait for the
answer of the one request the test is about with `page.waitForResponse`.

## A MUI Snackbar closes on a click elsewhere

The `Snackbar` of MUI calls `onClose` with the reason `clickaway` when the user clicks anywhere
outside it. A toast that has to stay until the user clicks it away (`EXT-023`, `TF-011`) then goes
on the first click into the page. Ignore that reason in `onClose`, and close the toast only on its
own close button. Other libraries have such an option as well; check it for every toast that has to
stay.

## A toast outside a modal dialog cannot be clicked

A modal dialog makes the rest of the page unreachable. A native `<dialog>` opened with
`showModal()` lies in the top layer of the browser, above every `z-index`, and makes everything
outside it `inert`. The dialogs of many design libraries trap the focus and lay a backdrop over the
page. Either way, a toast drawn outside the dialog may show, but it cannot be clicked away while
the dialog is open, and a sticky toast keeps the user waiting.

So draw the toasts where the dialog does not block them, see
[behaviour.md](behaviour.md#toasts). With a native `<dialog>`, a popover does not help: everything
outside the modal dialog stays `inert`, also in the top layer. Draw the container of the toasts
inside the open dialog while it is open, or open the dialog with `show()` and a backdrop of your
own. With a dialog of a design library, use the library's own way to put an element above its
dialogs, or draw the toasts inside the open dialog as well. A test opens a dialog, shows a sticky
toast and clicks it away.
