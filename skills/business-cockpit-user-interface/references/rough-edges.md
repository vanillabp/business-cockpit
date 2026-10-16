# The rough edges

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Here is what brings a generated user interface down if nobody mentions it. Each point comes from the
code of the cockpit. Read this before the first screen, not when something does not work.

1. [Assigning makes a candidate](#assigning-makes-a-candidate)
1. [Claiming overwrites](#claiming-overwrites)
1. [Giving back somebody else's task answers 200](#giving-back-somebody-elses-task-answers-200)
1. [Closed tasks can still be claimed](#closed-tasks-can-still-be-claimed)
1. [Bulk actions do not say what they skipped](#bulk-actions-do-not-say-what-they-skipped)
1. [`current-user` answers 200 instead of 404](#current-user-answers-200-instead-of-404)
1. [The refresh token leads nowhere](#the-refresh-token-leads-nowhere)
1. [A search pattern goes into the database unescaped](#a-search-pattern-goes-into-the-database-unescaped)
1. [Two filters on one path](#two-filters-on-one-path)
1. [`businessIds` is lost in the update](#businessids-is-lost-in-the-update)
1. [The suggestions are not the most frequent](#the-suggestions-are-not-the-most-frequent)
1. [An event says that something changed, not what](#an-event-says-that-something-changed-not-what)
1. [`filterable` is not read by the server](#filterable-is-not-read-by-the-server)
1. [A module may report a type of another user interface](#a-module-may-report-a-type-of-another-user-interface)
1. [The update form has no offset](#the-update-form-has-no-offset)
1. [The bundler rewrites `import()`](#the-bundler-rewrites-import)
1. [`zone.js` in a module slows every part down](#zonejs-in-a-module-slows-every-part-down)
1. [A data entry pulls in the framework](#a-data-entry-pulls-in-the-framework)
1. [What needs a newer cockpit](#what-needs-a-newer-cockpit)

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

## Giving back somebody else's task answers 200

`claim?unclaim=true` only removes the assignee if it is the given person. If it hits nobody, nothing
changes, and the answer is still 200. So your user interface does not learn whether it worked. Read
the task again afterwards, or rely on the update stream.

One more thing about giving back: the server reads the task again afterwards without checking the
visibility, and returns it. That is on purpose. A task that is given back often disappears from the
very list it was given back from, and the user interface still needs it to redraw the row.

## Closed tasks can still be claimed

Only the follow-up checks whether a task has ended, and then answers 409. Claim, unclaim, propose,
remove candidate and mark as read do not check it. A closed task the user can still see takes these
actions and answers 200.

This is a bug and will be fixed. Until the server refuses it, your user interface disables these
actions itself when `endedAt` is set.

## Bulk actions do not say what they skipped

The form with a list of ids always answers 200. Ids this user may not see are left out without a
word. After a bulk action you do not know how many rows really changed.

The single form answers 404, both for a task that does not exist and for one this user may not see.
That is on purpose: the answer must not reveal that the task exists. So write nothing into your
message that does reveal it.

## `current-user` answers 200 instead of 404

The OpenAPI description says `GET /gui/api/v1/app/current-user` answers 404 when nobody is logged
in. The code answers 200 with an empty body. So check for an empty body, not for a status.

## The refresh token leads nowhere

The header `X-Refresh-Token` is in the description of `current-user`, and the controller takes it as
a parameter. The method does not use it, and nothing in the cockpit sets this header in an answer.
Do not build anything on it. It is a hook for a cockpit with an identity provider of its own, and
what a server should do with the header is written nowhere.

## A search pattern goes into the database unescaped

`query` of a `SearchQuery` is used as a regular expression, without escaping and without an anchor.
An input with `.` or `*` searches for something else than the user means, and a costly pattern
costs the server time. Escape what you pass on from an input field.

MongoDB offers no good search for this. A field is either searched with a regular expression, which
cannot use an index well for a match in the middle of a value, or with a full-text index, which
matches whole words and has one index per collection. Better ways are being worked on separately.

## Two filters on one path

The conditions of a query are joined with AND, but two conditions on the same field are not accepted
today. Your user interface does not offer it: when the user sets a filter on a path, remove the old
entry for that path first, then add the new one.

By intent this is no error, and a later version will allow it.

## `businessIds` is lost in the update

`businessIds` only works on `POST /workflow`. `WorkflowsUpdateRequest` has no such field. So a list
filtered by business ids gets cases in the update that do not belong to it. Filter them out yourself
if you use this filter.

## The suggestions are not the most frequent

The answer to a KWIC query has at most 21 entries, and the limit applies in the database before
sorting. So they are the first 21 found, not the 21 most frequent. They are then sorted by count
ascending, the rarest word first.

When 21 entries come, show a note "more than twenty matches" instead of the list. Otherwise you show
the user a random selection as if it were the best one. An improvement is planned.

The count counts occurrences of a word, not rows, and only within the rows that match the other
conditions.

## An event says that something changed, not what

An event of the update stream is a wake-up call: kind, id and type of change. It does not carry the
changed data, and it does not say who made the change. Never show anything from an event directly.
Ask the server, and take an empty answer as an answer: the row may have left the user's view.

Because the event does not say who made the change, the user interface cannot tell the user's own
change from somebody else's by the event. It remembers the ids of the rows the user just changed
through the cockpit, and does not mark the next change of such a row, see
[behaviour.md](behaviour.md#live-updates).

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

## What needs a newer cockpit

This description counts on prompts of the `business-cockpit` repository that are not all released
yet. The cockpit version of the interview (`Q-COCKPIT-VERSION`) must have them:

| Prompt | What it brings |
|--------|----------------|
| `1504` | `endReason` on user tasks and cases, so a cancelled row can be told from a completed one |
| `1505` | `customSettings` on the user, read and written one entry at a time with `GET` and `PUT /app/current-user/settings/{key}`, in the cockpit's own file `api/gui/v1.yaml` |
| `1506` | the count endpoint |
| `1507` | notes on user tasks and cases |
| `1509` | `OpenTasks` is gone; the open modes are `OpenTasksWithFollowUp` and `OpenTasksWithoutFollowUp`; without `mode` the server takes `OpenTasksWithFollowUp` for user tasks and `Active` for cases |
| `1511` | `GET /updates` and the types of its events in the cockpit's own file `api/gui/v1.yaml`, so these types are generated too |

Against an older cockpit the generated client lacks these endpoints and fields, and the build
fails. That is on purpose: better a failed build than a user interface that guesses.

Your user interface does not need prompt `1510`. Today the library still has a default security
chain with basic authentication, and `1510` moves it into `container`. The login on the cockpit side
is the business of the backend application.

One thing is still to come and has no prompt yet: the rights of the user and where they come from,
for the overview in the menu, see [behaviour.md](behaviour.md#the-header-and-the-menu).
