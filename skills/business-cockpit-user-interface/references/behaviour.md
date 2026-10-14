# What the user expects

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Here is what a cockpit user interface does, not what it looks like. The look comes from the
interview. What follows is the behaviour users know and workflow modules rely on. Where a point
depends on an interview answer, the question id is named.

1. [The whole](#the-whole)
1. [The screens](#the-screens)
1. [The task lists](#the-task-lists)
1. [How the columns of a list come about](#how-the-columns-of-a-list-come-about)
1. [The columns a workflow module brings](#the-columns-a-workflow-module-brings)
1. [The actions on a user task](#the-actions-on-a-user-task)
1. [Opening and working on a user task](#opening-and-working-on-a-user-task)
1. [The task bar](#the-task-bar)
1. [The workflow list](#the-workflow-list)
1. [The page of a case](#the-page-of-a-case)
1. [Search, filter, sort, page](#search-filter-sort-page)
1. [Live updates](#live-updates)
1. [Notifications](#notifications)
1. [Language and texts](#language-and-texts)
1. [Header and extension points](#header-and-extension-points)
1. [Who sees what](#who-sees-what)

## The whole

The Business Cockpit is the central user interface of a company. On one side, it gives employees
the user tasks that the digitised business processes create for them. On the other side, it lets
them look up the current state of a business process and, where needed, act on it apart from the
tasks.

For the company a business process is usually a business case. That can be an external case, like
"open an account" at a bank, or an internal one, like approving an employee's leave request. So the
user interface rarely says "process" or "workflow". It says "case" or the organisation's own word
(`Q-WORDING`). In this description "case" and "workflow" mean the same thing.

The cockpit is often used across the whole company. So it can also carry company-wide functions that
have little or nothing to do with processes and would otherwise need a web application of their own.
An example is the rule for a substitute during an absence. Such functions come in as pages of a
workflow module, see [Header and extension points](#header-and-extension-points).

Many developers find module federation unusual, because it is not how web applications are usually
built. The dev shell is the answer for the modules' developers, see [dev-shell.md](dev-shell.md).

## The screens

Which screens exist is decided per organisation (`Q-SCREENS`). The minimum is:

| Screen        | Path              | What it shows |
|---------------|-------------------|---------------|
| task list     | `/tasks`          | all user tasks the user may see |
| my tasks      | `/my-tasks`       | the user tasks where the user is the assignee |
| workflow list | `/workflows`      | the cases; there is exactly one workflow list |
| a case        | `/workflows/<id>` | the page from the workflow module |
| a user task   | `/tasks/<id>`     | the form from the workflow module, in the frame of the cockpit |

The paths are a proposal (`Q-ROUTES`). What matters is that every screen has an address that can be
shared and bookmarked, and that a link into the middle works.

Often there is more:

- A dashboard as the start page (`Q-DASHBOARD`). What it shows is up to the organisation. An example:
  a pie chart of all open user tasks by task type, and a second one of all open cases by kind. A
  click on a slice opens the matching list, narrowed to that type. Two buttons on the left of the
  header open the two lists without narrowing. The GUI API has no endpoint that counts by type. The
  user interface counts with one request per type, with `pageSize` 1, or the backend gets an
  endpoint of its own.
- Sub-lists (`Q-SUB-LISTS`), narrowed to a role or to some task types. An example: a company has a
  list of committees a user can be a member of. The start page has one card per committee (that is,
  per role), with the number of tasks the user can see and the number the user has claimed. A user
  without the role sees 0/0. Which views exist is always the organisation's choice, because the
  task query allows many kinds of filtering.
- The notification settings (`Q-NOTIFICATIONS`), see [Notifications](#notifications).
- Pages that a workflow module delivers (`Q-EXTENSION-POINTS`).

## The task lists

Users find their user tasks and complete them in time. Closed tasks are documentation:
the user can see with which input a task was completed back then (`Q-CLOSED-TASKS`).

### Which tasks

A task list shows all user tasks the user may see. Which these are is decided on
the server, roughly "candidate groups plus candidate users plus assignee, minus excluded candidate
users, plus admitted users". The user interface never computes it.

### Claiming

A user takes a task actively (claim) and becomes its assignee. The server does not
check a claim before work on a task. Some organisations want claiming informal, others hard: the
form can only be edited after the user has claimed the task (`Q-CLAIM`).

### The status line

On the left, the status line has a group of buttons to choose the primary
filter (`Q-PRIMARY-FILTERS`). Possible are `OpenTasks`, `OpenTasksWithoutFollowUp` (the default),
`OpenTasksWithFollowUp`, `OpenTaskOnlyFollowUp` and `ClosedTasksOnly`. The buttons show a short,
translated label, for example "Open" or "Closed". Next to the label, except for `ClosedTasksOnly`,
the total number of rows stands in brackets. The total of the chosen primary filter comes with the
rows. Each other total needs a request of its own, with the same filters and only the primary
filter changed, see [gui-api.md](gui-api.md#sorting). The primary filter last chosen is kept as a
preference of the user and used again next time. The GUI API has no place for preferences, so keep
it in the browser's local storage, per user. On the right, the status line shows a colour legend
for the states of a row, for example new, changed, ended, removed.

`OpenTasksWithFollowUp` returns the same rows as `OpenTasks`. The difference in the user interface
is the follow-up column, see below.

### A row

Every user task has one row. The columns are there as the task defines them: filled,
sortable and filterable. The list keeps itself up to date, see [Live updates](#live-updates).

The user sees the context of each task, that is, the columns this kind of task needs. So the columns
shown depend on what is in the list. Usually a user does not see all possible columns, because their
area of work limits the kinds of tasks. And predefined lists sometimes show only tasks with the same
or similar columns.

A row whose task the user has not read yet is bold, the whole row. The user can mark tasks as read or
unread, in the list and on the task page. Opening a task marks it as read.

### Fixed columns

Some columns every task has (`Q-TASK-COLUMNS`). The organisation picks them and
their order, for example id, case kind, title, assignee, candidate users, created (`createdAt`), due
(`dueDate`), follow-up (`followUpDate`), ended (`endedAt`). The follow-up column only shows for
`OpenTasksWithFollowUp` and `OpenTaskOnlyFollowUp`. The ended column only shows for
`ClosedTasksOnly`. Some organisations want fixed columns only and no dynamic ones, because the
cockpit is used in one area where the list holds known values (`Q-MODULE-COLUMNS`).

The standard columns behave and look different from the columns filled with a module's data:

- `id`: a checkbox to select the row for bulk actions. The id itself is not shown.
- `title`: the text is a link that opens the full view. It is a real `<a>` element, so "open in new
  tab" from the right mouse button works.
- `assignee`: the short form of the person. On mouse-over the long form, the user id and the email
  address. On mouse-over also a checkbox appears that claims the task. If the current user has
  claimed the task, a vertical bar stands before the short form, and the names to the right of the
  bar stay aligned with all other names. In that case the mouse-over shows a checked checkbox that
  gives the task back.
- `candidateUsers`: the column is as wide as an icon. Its header is an icon of several people. With
  one person the cell shows an icon of one person, with several the icon of several people. On
  mouse-over a list of all persons appears, each with a bin icon that removes the person. On
  mouse-over of a person, the same details as for the assignee.
- `createdAt`, `dueDate`, `followUpDate`, `endedAt`: the localised date if it is not today, with the
  time without seconds on mouse-over. If the date is today, the time instead of the date, with the
  date on mouse-over.

### Bulk actions

The selected tasks can be claimed, proposed to another person (they become
candidate users), marked as read or unread, and put on follow-up (`Q-BULK-ACTIONS`).

### Notes

If there is a notes column (`Q-NOTES`), notes can be edited in the list, or for all
selected tasks at once through the header area. The GUI API has no endpoint for notes today. This
choice needs an extension of the backend.

### Follow-up

A user can put a task on follow-up until a date (`Q-FOLLOW-UP`). It then leaves
`OpenTasksWithoutFollowUp` until the date has come.

## How the columns of a list come about

A list shows user tasks of several workflow modules at the same time, and each module says which
columns it wants for a row. The list must be drawn once with all columns. A list that first shows
the standard columns and then adds the module columns flickers, and users dislike that.

### The recommended way

When the list opens, do two things in parallel:

1. Ask `GET /gui/api/v1/workflow-module` and load the part `./UserTaskList` (or `./WorkflowList`)
   of every module the user may see, see
   [module-federation.md](module-federation.md#loading-a-part). A part that is already loaded is
   not loaded again.
2. Ask for the first page of rows.

Draw the list once both are there. Then the columns of each row come from calling the module's
column function for that row, without waiting, because the module's part is loaded. Merge the
columns of all rows on the first page into the header.

This loads the list parts of modules that may have no row in the list. Each part is loaded once per
session, so this costs little. A module without a federated user interface is known after the
first try and not asked again.

### The proven fallback

First fetch 20 rows, about one screen. Learn from them which modules and
which definitions appear, load the list parts of exactly those modules, and then draw. This also
avoids flicker, at the price of one extra request before the first draw. Use it when the list of
permitted modules is large and most of them never appear in a list.

In both ways:

- A definition is the pair of workflow module and `taskDefinition` for user tasks, and the pair of
  workflow module and `bpmnProcessId` for cases. Call the column function with one row of each
  definition.
- The answers are merged by `path`. Two definitions with the same `path` mean the last one seen
  wins, with all its content. Nothing is added up.
- The cockpit adds its fixed columns where no module has taken the `path`.
- The columns are sorted by `priority`, the smallest number first. A column with `show: false` is
  not drawn.
- Rows loaded later, while scrolling, do not change the header. If they bring columns the header
  does not have, the cells stay empty until the next reload. That keeps the list from jumping.
- A module whose part does not load must not keep the whole list empty. Its rows show the fixed
  columns and a note in the cells the module would have filled.

## The columns a workflow module brings

A user sees, in as little space as possible, the information about each task or case
needed to judge the row.

A module defines the columns with two functions, one for user tasks and one for cases, see
[types.md](types.md#column). The value of a cell is `column.path`, resolved against the row with dots
as separators. So `details.customer.name` reaches into the reported business data.

Each `type` expects a different structure at `path`, and the cockpit draws it like this:

| `type`      | Value at `path`                                       | How it is drawn |
|-------------|-------------------------------------------------------|-----------------|
| `value`     | a single value                                        | localised: an integer with thousands separators, a decimal number with the local decimal mark, a boolean as "Yes" or "No" in the user's language. Text left-aligned, everything else right-aligned, decimal numbers aligned at the decimal mark |
| `i18n`      | a map with language codes as keys and texts as values | the text in the language of the user interface, falling back to the default language |
| `person`    | a `Person`                                            | `displayShort`, else `email`, else `id`. On mouse-over: `display`, an empty line, `id`, `email` |
| `date`      | a string `YYYY-MM-DD`                                 | the localised date |
| `date-time` | a string in ISO 8601 extended format                  | the localised date and, next to it, the localised hour and minute. On mouse-over also the seconds |
| `time`      | a string `hh:mm[:ss]`                                 | the localised hour and minute. On mouse-over also the seconds, if present |

Without `type` the cell is drawn like `value`. An object at the path is shown as a short note that
this column needs a type, not as `[object Object]`.

What is sent to the server as `sort` depends on the `type` of the column. This is one of the least
visible places of the whole contract:

| `type` of the column | What is sent as `sort` |
|----------------------|------------------------|
| `i18n`               | `<path>.<language>` |
| `person`             | `<path>.sort,<path>.id` |
| everything else      | `<path>` |

The reason for the second row: a person is stored in the document with the fields `id`, `fulltext`
and `sort`, and `sort` is the value to sort by. The application builds it itself, for example from
last name and first name. The general rule, which holds for filtering too: a path reaches into the
document the server stores, not into the JSON you got. For `details.*` both are the same. For
`assignee` they are not: the API returns `display`, `displayShort`, `email` and `avatar`, and the
stored fields are `id`, `fulltext` and `sort`.

Cells drawn by the module itself are an open question, see
[module-federation.md](module-federation.md#cells-drawn-by-a-workflow-module). More column types out
of the box, for example a currency, are a wish for later.

## The actions on a user task

For one row or for several selected rows:

| Action           | What it means |
|------------------|---------------|
| Claim            | the user becomes the assignee |
| Unclaim          | the assignee is removed |
| Propose          | a person the user searched for becomes a candidate user |
| Remove candidate | a person is removed from the candidate users |
| Mark as read     | sets `read`, the row is no longer bold |
| Mark as unread   | takes `read` back |
| Follow-up        | sets or deletes the follow-up date |

To propose a task, the user searches for persons in a field, and the user interface calls
`POST /user` with a maximum number of hits for that.

What these actions change stays. The report that creates a user task sets assignee, candidate users,
candidate groups and excluded candidate users. After that no report of the workflow module changes
these fields any more. Only claiming, unclaiming, proposing and removing in the cockpit change the
assignee and the candidate users. So a task does not vanish from a user's list because the workflow
module gives it to somebody else. One exception is a task that names nobody and is therefore shown
to everybody: it takes the names from the first later report that has some. The admitted users
(`admittedUsers`) are the other exception: every report may set them.

The update stream takes some time. So show the effect of the user's own action as soon as the server
has accepted it, for example the read marker or the new assignee. The stream confirms it a moment
later. A user interface without live updates (`Q-LIVE-MODE` is `refresh-button`) reads the row again
after every action.

Closed tasks: disable these actions when `endedAt` is set, see
[rough-edges.md](rough-edges.md#closed-tasks-can-still-be-claimed).

## Opening and working on a user task

The user works on the task to complete it in business terms.

The form comes from the workflow module, not from the cockpit. Around the form the cockpit shows a
frame with general functions, see [The task bar](#the-task-bar).

### Opening

The title in the list is a link to the task page. A click opens it in the same tab, and
"open in new tab" works too. For the type `EXTERNAL` the reported address opens in a window of its
own. From a task to its case always leads to the cockpit's own case page, not to the address of the
task. The task says where the task is shown, and that is not where the case is shown.

### Open as own window (`Q-OWN-WINDOW`)

The task page has a button for it. What really happens: the
application header disappears (the application header is not the task bar), and the list opens in a
new tab. So the user does not have to close a form being filled in, and loses no input, when they
need another area of the cockpit. The use case: a customer calls and wants to know about their case
while the user is in the middle of a form.

### What the page does


1. It loads the user task and marks it as read.
2. It loads the part `./UserTaskForm` of the module.
3. While one of the two is running, it shows a loading state. On an error, a note with a button that
   tries again. There are two different errors: the task could not be loaded, or the part of the
   module could not be loaded.
4. It creates the module's element and sets its properties, see
   [types.md](types.md#what-a-part-gets).

### The frame

A long form scrolls in its own area, and the task bar stays in place. The content of
a task must never break the frame of the cockpit.

### The barrier

If claiming is hard (`Q-CLAIM`), the form area cannot be edited before the user has
claimed the task. Usually a half-transparent layer lies over it. How it looks is up to the
organisation.

### Everything inside the form is the module's

All requests that are needed to show data or to
complete the form go through the proxy `/wm/` directly to the workflow module. The cockpit knows
nothing about them. Whether a form saves a draft automatically, by hand with a save button, or not at
all, the form decides.

### The column on the right

Usually a column on the right of the form shows basic information about
the task at the top, for example the case number and the customer, and the completing actions at the
bottom, usually as buttons like "Accept" and "Reject". The column fills the whole visible height and
does not scroll with a long form, so the action buttons are always visible. If the area above the
actions is too small, that area scrolls.

The use of the main parts must always be the same: functions and information in the same place,
the task bar and the right column with information and action buttons. The cockpit would
ideally enforce this itself. That is hard, because then a task would have to be offered in several
pieces (body, information, action buttons). Until this is solved, the column is the module's job,
and the dev shell may offer the same layout as a template (`Q-INFO-COLUMN`). Experience shows that
when several teams build modules, the results differ and usability suffers. A solution would be
several parts per task (information, buttons, form) that the cockpit arranges. Then the buttons
would have to ask the form to submit by messages, and that has to feel natural in the module's
framework. This is open.

### Changes by others

When somebody else changes the task while the user has it open, the user is
told right away. For example: "User X has claimed this task. Please talk to them before you save or
complete, to avoid losing data." The message is a toast that the user has to click away.

## The task bar

It shows basic information (task kind, case kind, case number) and offers claim,
unclaim, propose to a person and follow-up. It also offers "mark as unread", because an opened task
counts as read.

It is a header or a footer of the task page (`Q-TASK-BAR`). Whichever it is, it stands in the same
place for all kinds of tasks, so it is a visual anchor for the user. Its content can never break it:
text that is too long is cut with "...". Which information it shows is up to the organisation
(`Q-TASK-BAR-INFO`). The controls of the actions are always in the same place
(`Q-TASK-BAR-ACTIONS`).

The task bar belongs to the cockpit, not to the workflow module.

## The workflow list

Technically the same holds as for the task lists. Only the differences are listed here.

Users find cases, get a first idea of their state from the columns, and open the
details page when needed.

### Which cases

The server selects the cases from `accessibleToUsers` and `accessibleToGroups`.
These two fields are not shown as columns.

### Primary filters

`All`, `Active` and `Inactive` (`Q-WORKFLOW-FILTERS`).

### Rows

Every case has one row. Modules define their own columns for the workflow list, apart from
the task columns. The workflow list has its own fixed columns (`Q-WORKFLOW-COLUMNS`). There is no
read marker and no read function. A workflow list without bulk actions has no selection column.

Some organisations want fixed columns only, as for the task lists (`Q-MODULE-COLUMNS`).

## The page of a case

The user looks at the details of a case. The workflow module may also offer actions
apart from the regular flow of the process here, for example "cancel case", which the BPMN process
handles in an event-based sub-process.

Different from the task page:

- There is no button to open the case page in a window of its own. "Open in new tab" with the right
  mouse button works.
- There is no task bar, because there are no functions across all cases.
- There is a column with information and actions, the same as on the task page.

Everything on this page is the module's. Long content must not break the frame of the cockpit. The
page loads the case and the part `./WorkflowPage`, with the same loading and error states as the
task page, and gives the module `getUserTasks` for the tasks of the case.

## Search, filter, sort, page

The user copes well with many rows.

### Full-text search

Both lists have it. The input has an x button that clears it. The field offers
suggestions: it asks from three characters on, 300 milliseconds after the last key, shows at most 20
suggestions, and shows a note instead when there are more than twenty. A suggestion is taken with a
click, the input with the Enter key. The search ignores upper and lower case. Escape the input, see
[gui-api.md](gui-api.md#filtering-over-reported-business-data).

### A filter per column

For each column the user can set a search term that filters on the server,
and remove it again. Only one filter per path, see
[rough-edges.md](rough-edges.md#two-filters-on-one-path). A button resets all searches.

### Sorting

A click on a column header makes it the sort column. A second click turns the direction,
a third goes back to the default order of the list. The sort mark appears only on a column with
`sortable`.

What a column allows is set per task and case kind, by the column flags `sortable`, `filterable` and
`resizeable`.

### Paging

The list loads in blocks (`Q-PAGE-SIZE`), and the next block comes when the user reaches
the end of the drawn rows. The page number is computed from the rows the server delivered, not from
rows the list keeps only on its own (see [Live updates](#live-updates)). The loading sign shows on
the first load, not when loading more.

A new sort or a new search loads the list again, with a new start timestamp.

A wish of customers: sorting by value instead of by what is shown. The values are stored in MongoDB
and sorted there. It would need a small structure with value and text, like `i18n`, and sorting by
the value. Cells drawn by the module would make that easy, because then the path would always be the
value. This is open.

## Live updates

Changes to the data shown become visible soon, without the user doing anything.

### How it looks (`Q-LIVE-MODE` is `live`)

A new task appears in the list, highlighted in one
colour. A change shows right away, highlighted in another colour. A completed task turns grey. These
markers stay, so a user who comes back to the screen sees what happened. A button acknowledges the
changes. Then the markers and the inactive rows go away.

### Experiment (`Q-LIVE-MODE` is `auto-acknowledge`)

The marker of a row goes away by itself, for
example three seconds after the mouse has left the row. This is still to be validated.

### Refresh button only (`Q-LIVE-MODE` is `refresh-button`)

Some organisations want no live update
at all. The header has a refresh button that becomes active when changes were reported. Nothing else
moves.

### The rules


- A list with a primary filter for active rows still contains rows that became inactive while the
  user watched, the ones reported as ended. Scrolling must still work correctly.
- The totals shown next to the primary filters are updated when something changes.
- A row never disappears under the user's hands. It is coloured and stays, until the user
  acknowledges.

### How it works


1. When the list is built, keep the timestamp the server returned. That is the `initialTimestamp`
   from [gui-api.md](gui-api.md#the-timestamp-that-keeps-a-list-stable).
2. Listen on the update stream to the name of the list's kind, `UserTask` or `Workflow`. Take the id
   from each event. Every type leads to the same action, except `RELOAD`, which loads the whole list
   again.
3. Ask with `PUT` for as many rows as are loaded, plus a reserve of two blocks, and leave out the ids
   of the changed rows. They come back in full.
4. Set the state of each returned row:
   * ended after the start timestamp: `ENDED`,
   * otherwise created after the start timestamp: `NEW`,
   * otherwise changed after the start timestamp: `UPDATED`,
   * otherwise `INITIAL`.
5. A row the server no longer returns stays in place and becomes `REMOVED_FROM_LIST`.
6. The user's selection stays.
7. The totals are asked again, see [gui-api.md](gui-api.md#sorting).
8. Acknowledging loads the list again with a new start timestamp. That drops the markers and the
   inactive rows.

The five states, `INITIAL`, `NEW`, `UPDATED`, `ENDED` and `REMOVED_FROM_LIST`, each have a colour
from the theme (`Q-THEME`), and the legend in the status line explains them.

An event of the type `FOLLOWUP` is the reason a list must not only ask about rows it knows. The
server sends it when a follow-up date has come. The task was filtered out of the list until then. It
only appears because of this event.

An open task page listens too. When its task changes, it reads the task again, sets the property of
the module's element again, and shows the toast described in
[Opening and working on a user task](#opening-and-working-on-a-user-task).

## Notifications

Users learn about new tasks they could take, even when the cockpit is not open in their
browser.

A menu entry of its own leads to the settings (`Q-NOTIFICATIONS`). The user chooses, for all cases
and per kind of case, whether they want notifications and on which channel. The endpoints are
`GET /notifications/media`, `GET` and `PUT /notifications/config`, `GET /notifications/recipient-config`,
`PUT /notifications/recipient-config/{mediumType}` and `GET /notifications/workflows`.

A link in a notification mail points to the task page. It must work without a detour over the start
page, also when the user has to log in first.

## Language and texts

All texts are translated, with a default language chosen in advance (`Q-LANGUAGE-DEFAULT`). Whether
there are more languages and whether the user can switch is asked (`Q-LANGUAGES`,
`Q-LANGUAGE-SWITCH`).

The texts are in separate files, one per language. None is written into the code.

The chosen language must reach every place where a text per language is drawn: the column header,
the cell, the title of a row, and every part of a module, through the property `language`.

Decide once what happens when a language is missing, and keep to it everywhere: take the default
language, and if that is missing too, the first language there is. A header, a cell of type `i18n`
and the title of a row behave the same.

The server has the key `business-cockpit.default-locale`. It does not set the language of the user
interface.

## Header and extension points

Workflow modules hook into the cockpit with functions of their own.

At places the organisation chooses, workflow modules show extensions of the user interface
(`Q-EXTENSION-POINTS`). Examples: the header, the menu.

The rules:

- Every workflow module the user may see is asked.
- Which extension points exist is decided per organisation. It is a module federation contract
  between the cockpit and the workflow modules, and it holds for all modules. The types library
  writes it down, see [types.md](types.md#the-extension-points).
- A module that does not fill an extension point causes no error. It is ignored.

## Who sees what

Different groups of users may get different views (`Q-VISIBLE-GROUPS`). How is up to the
organisation. The groups are taken from the first call of `GET /app/current-user`, because the JWT
may carry fewer groups, or none.

This only shapes the user interface. What a user may see and do is decided by the server for every
request.
