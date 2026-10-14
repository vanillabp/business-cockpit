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
1. [`OpenTasksWithFollowUp` is only another name](#opentaskswithfollowup-is-only-another-name)
1. [The suggestions are not the most frequent](#the-suggestions-are-not-the-most-frequent)
1. [An event says that something changed, not what](#an-event-says-that-something-changed-not-what)
1. [`filterable` is not read by the server](#filterable-is-not-read-by-the-server)
1. [The shell is not open today](#the-shell-is-not-open-today)
1. [The module side still suggests an old `uiUriType`](#the-module-side-still-suggests-an-old-uiuritype)
1. [What the GUI API does not have](#what-the-gui-api-does-not-have)

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

## `OpenTasksWithFollowUp` is only another name

The name sounds like a filter on the follow-up date. There is none: the primary filter is another
name for `OpenTasks` and returns all open tasks, with and without a follow-up date. This is decided,
and it stays so. In the user interface the difference is the follow-up column, see
[behaviour.md](behaviour.md#the-task-lists).

Open tasks with a follow-up date in the future come with `OpenTaskOnlyFollowUp`. Those without one,
or with one that is due, come with `OpenTasksWithoutFollowUp`.

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

Each stream only gets events its user may know of, or events about rows its browser already shows.
A `RELOAD` with an empty id means "load the lists of this kind again". It comes at the start of every
stream and whenever the stream had to forget ids. See
[gui-api.md](gui-api.md#the-update-stream).

## `filterable` is not read by the server

The field is in every column. The server does not know columns, so it never reads it. Whether a
column offers a filter is decided by your user interface from this flag.

## The shell is not open today

The single-page application must load without a login, see
[gui-api.md](gui-api.md#protection-and-login). The library's default security chain does not do that
yet. It opens `/assets/**`, `/static/**`, `/wm/**`, `app/info` and `app/current-user`, and asks for
basic authentication for everything else, the shell at `/` included. An application with its own
security chain (a bean named `guiHttpSecurity`) opens the shell itself. With the default chain, the
browser shows its basic authentication dialog before the shell loads.

## The module side still suggests an old `uiUriType`

When a workflow module starts without `ui-uri-type`, the message of the VanillaBP extension suggests
`EXTERNAL` and `WEBPACK_MF_REACT`. Any string is accepted. A module for a native federation cockpit
sets `NATIVE_MF`, or the value from `Q-UI-URI-TYPE`. Tell the module developers so.

## What the GUI API does not have

Some wishes of the interview need something the GUI API does not offer today:

- notes on a user task (`Q-NOTES`),
- a place to keep the user's preferences, such as the last primary filter; keep them in the
  browser's local storage,
- counts grouped by task type or case kind for a dashboard (`Q-DASHBOARD`); count with one request
  per type, or extend the backend.

Say so in the interview, and add a backend extension to the plan when the organisation wants them.
