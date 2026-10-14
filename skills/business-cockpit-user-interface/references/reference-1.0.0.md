# Reference checklist 1.0.0

*Part of the description in [`../SKILL.md`](../SKILL.md).*

This is the base checklist of a generated Business Cockpit user interface, version 1.0.0. The
generated project copies it into `CHECKLIST.md` and adds the customer's own items with the prefix
`CUST-`, see [`../SKILL.md`](../SKILL.md#phase-2-generate). Every end-to-end test names the id of the
item it proves.

Each item is one sentence that a test can prove. The column "Applies" says whether the item always
holds or depends on an interview answer, see [interview.md](interview.md).

Ids are stable. They are never reused and never renumbered. A later version of this list adds items
with new ids, changes the text of an item only when its meaning stays, and marks an item as removed
instead of deleting it. So an upgrade is the diff between two versions of this file.

| Prefix  | Area |
|---------|------|
| `PKG-`  | build outputs of the project |
| `AUTH-` | loading, login and staying logged in |
| `NAV-`  | screens, routes and the frame |
| `TL-`   | task lists |
| `COL-`  | columns and cells |
| `ACT-`  | actions on user tasks |
| `TF-`   | the page of a user task |
| `WL-`   | the workflow list |
| `WP-`   | the page of a case |
| `SRCH-` | search, filter, sort and paging |
| `LIVE-` | live updates |
| `EXT-`  | module parts and extension points |
| `I18N-` | language and texts |
| `NOTI-` | notifications |
| `DS-`   | dev shell |

## PKG: build outputs

| Id        | Item                                                                                                                     | Applies |
|-----------|--------------------------------------------------------------------------------------------------------------------------|---------|
| `PKG-001` | One npm workspace builds the user interface, the types library and every dev shell with one command.                     | always |
| `PKG-002` | The GUI API client is generated at build time from the OpenAPI files of the pinned cockpit version and is not committed. | always |
| `PKG-003` | The UI jar has the coordinates from the interview and holds `static/index.html` and the bundle below `static/assets/`.   | always |
| `PKG-004` | Put on the class path of a cockpit application, the UI jar makes `/` show the user interface.                            | always |
| `PKG-005` | The runnable jar starts with `java -jar` and serves the cockpit APIs and the user interface.                             | `Q-RUNNABLE-JAR` = yes |
| `PKG-006` | The types library is published with no dependencies and no peer dependencies.                                            | always |
| `PKG-007` | Each dev shell package is published with its framework as a peer dependency and the types library as a dependency.       | always |
| `PKG-008` | `INTERVIEW.md` and `CHECKLIST.md` are in the project and name the version of this reference.                             | always |
| `PKG-009` | The bundle contains no part of any workflow module.                                                                      | always |

## AUTH: loading and login

| Id         | Item                                                                                                      | Applies |
|------------|-----------------------------------------------------------------------------------------------------------|---------|
| `AUTH-001` | Without a login, the shell and its static files load, and the frame shows the title from `GET /app/info`. | always |
| `AUTH-002` | When `GET /app/current-user` answers without a body, the browser is sent to the login of the backend.     | always |
| `AUTH-003` | After the login, the user interface shows the user from `GET /app/current-user`.                          | always |
| `AUTH-004` | A 401 on any later request sends the browser to the login, and no error page is shown.                    | always |
| `AUTH-005` | The user interface has no login form, no user management and no rights check of its own.                  | always |
| `AUTH-006` | The user interface makes no request in the background only to stay logged in.                             | always |
| `AUTH-007` | `/logout` ends the session and shows the start page.                                                      | always |
| `AUTH-008` | The groups used to shape the screens come from `GET /app/current-user`, not from the token.               | always |

## NAV: screens and frame

| Id        | Item                                                                                                             | Applies |
|-----------|------------------------------------------------------------------------------------------------------------------|---------|
| `NAV-001` | `/tasks` shows all user tasks the user may see.                                                                  | always |
| `NAV-002` | `/my-tasks` shows the user tasks where the user is the assignee.                                                 | always |
| `NAV-003` | There is exactly one workflow list.                                                                              | always |
| `NAV-004` | A deep link to any screen, opened in a new browser, shows that screen after the login.                           | always |
| `NAV-005` | The dashboard shows what the interview asked for, and a click on it opens the matching list, narrowed.           | `Q-DASHBOARD` |
| `NAV-006` | The header has buttons that open the task list and the workflow list without narrowing.                          | `Q-DASHBOARD` |
| `NAV-007` | Each sub-list shows its count of visible and of claimed tasks, and 0/0 for a role the user does not have.        | `Q-SUB-LISTS` |
| `NAV-008` | Users of the configured groups see the screens and menu entries meant for them, and others do not.               | `Q-VISIBLE-GROUPS` |
| `NAV-009` | The user interface calls a case by the word from the interview, and never "process" or "workflow" in user texts. | always |

## TL: task lists

| Id       | Item                                                                                                        | Applies |
|----------|-------------------------------------------------------------------------------------------------------------|---------|
| `TL-001` | The status line offers the primary filters from the interview, with short translated labels.                | always |
| `TL-002` | The default primary filter is the one from the interview.                                                   | always |
| `TL-003` | Each primary filter except `ClosedTasksOnly` shows its total in brackets.                                   | always |
| `TL-004` | The totals of the primary filters not chosen are asked with `pageSize` 1 and no `sort`.                     | always |
| `TL-005` | The primary filter last chosen is used again the next time the user opens the list.                         | always |
| `TL-006` | The status line shows a colour legend for new, changed, ended and removed rows.                             | `Q-LIVE-MODE` ≠ `refresh-button` |
| `TL-007` | A row whose task the user has not read is bold.                                                             | always |
| `TL-008` | The fixed columns are the ones from the interview, in its order.                                            | always |
| `TL-009` | The follow-up column shows only for `OpenTasksWithFollowUp` and `OpenTaskOnlyFollowUp`.                     | `Q-FOLLOW-UP` = yes |
| `TL-010` | The ended column shows only for `ClosedTasksOnly`.                                                          | `Q-CLOSED-TASKS` = yes |
| `TL-011` | The `id` column is a checkbox that selects the row, and the id is not shown.                                | `Q-BULK-ACTIONS` not empty |
| `TL-012` | The title is a real link that opens the task page, and "open in new tab" works on it.                       | always |
| `TL-013` | The assignee cell shows the short form, and on mouse-over the long form, the user id and the email address. | always |
| `TL-014` | On mouse-over of the assignee cell a checkbox claims the task, or, if the user has it, gives it back.       | always |
| `TL-015` | A task the current user has claimed shows a bar before the name, and the names stay aligned.                | always |
| `TL-016` | The candidate users column is icon-wide and shows a one-person or a several-people icon.                    | always |
| `TL-017` | On mouse-over the candidate users column lists all persons, each with a bin icon that removes the person.   | always |
| `TL-018` | A date column shows the localised date, or the time if the date is today, and the other one on mouse-over.  | always |
| `TL-019` | The selected tasks can be acted on together with the bulk actions from the interview.                       | `Q-BULK-ACTIONS` not empty |
| `TL-020` | Notes can be edited in the list and for all selected tasks through the header area.                         | `Q-NOTES` = yes |
| `TL-021` | A task put on follow-up leaves `OpenTasksWithoutFollowUp` until its date has come.                          | `Q-FOLLOW-UP` = yes |
| `TL-022` | Closed tasks can be listed, and opening one shows the input it was completed with.                          | `Q-CLOSED-TASKS` = yes |

## COL: columns and cells

| Id        | Item                                                                                                              | Applies |
|-----------|-------------------------------------------------------------------------------------------------------------------|---------|
| `COL-001` | The list is drawn once with all columns of the first page; the header does not change after the first draw.       | `Q-MODULE-COLUMNS` = yes |
| `COL-002` | The list parts of the modules load in parallel with the first page of rows.                                       | `Q-MODULE-COLUMNS` = yes |
| `COL-003` | Columns of different modules with the same `path` appear once.                                                    | `Q-MODULE-COLUMNS` = yes |
| `COL-004` | Columns are ordered by `priority`, the smallest first, and a column with `show: false` is not drawn.              | `Q-MODULE-COLUMNS` = yes |
| `COL-005` | A module whose list part fails to load does not keep the list empty; its rows show the fixed columns.             | `Q-MODULE-COLUMNS` = yes |
| `COL-006` | A `value` cell is localised, text left-aligned, other values right-aligned, decimals aligned at the decimal mark. | `Q-MODULE-COLUMNS` = yes |
| `COL-007` | A boolean `value` cell shows "Yes" or "No" in the user's language.                                                | `Q-MODULE-COLUMNS` = yes |
| `COL-008` | An `i18n` cell shows the text in the language of the user interface, or in the default language if missing.       | `Q-MODULE-COLUMNS` = yes |
| `COL-009` | A `person` cell shows `displayShort`, else `email`, else `id`, and on mouse-over `display`, `id` and `email`.     | `Q-MODULE-COLUMNS` = yes |
| `COL-010` | A `date` cell shows the localised date.                                                                           | `Q-MODULE-COLUMNS` = yes |
| `COL-011` | A `date-time` cell shows the localised date with hour and minute, and the seconds on mouse-over.                  | `Q-MODULE-COLUMNS` = yes |
| `COL-012` | A `time` cell shows the localised hour and minute, and the seconds on mouse-over if present.                      | `Q-MODULE-COLUMNS` = yes |
| `COL-013` | A cell whose value is an object shows a short note, not `[object Object]`.                                        | `Q-MODULE-COLUMNS` = yes |
| `COL-014` | No list shows a module column when the organisation chose fixed columns only.                                     | `Q-MODULE-COLUMNS` = no |

## ACT: actions on user tasks

| Id        | Item                                                                                 | Applies |
|-----------|--------------------------------------------------------------------------------------|---------|
| `ACT-001` | Claim makes the user the assignee, and the row shows it.                             | always |
| `ACT-002` | Claiming a task that has another assignee asks the user first.                       | always |
| `ACT-003` | Unclaim removes the user as assignee, and the row shows it.                          | always |
| `ACT-004` | "Propose to" a person found by search makes the person a candidate user.             | always |
| `ACT-005` | Removing a candidate user takes the person off the task.                             | always |
| `ACT-006` | Mark as read and mark as unread change the bold state of the row.                    | always |
| `ACT-007` | Setting and deleting a follow-up date works for one task and for the selected tasks. | `Q-FOLLOW-UP` = yes |
| `ACT-008` | The effect of the user's own action shows as soon as the server has accepted it.     | always |
| `ACT-009` | The actions are disabled for a task whose `endedAt` is set.                          | always |
| `ACT-010` | A failed single action tells the user, without revealing whether the task exists.    | always |

## TF: the page of a user task

| Id       | Item                                                                                                         | Applies |
|----------|--------------------------------------------------------------------------------------------------------------|---------|
| `TF-001` | Opening a task loads it, marks it as read and shows the module's form.                                       | always |
| `TF-002` | While the task or the form loads, a loading state shows.                                                     | always |
| `TF-003` | A task that fails to load and a form that fails to load show two different notes, each with a retry button.  | always |
| `TF-004` | The task bar is a header or a footer as the interview says, and it is in the same place for every task kind. | always |
| `TF-005` | The task bar shows the information from the interview and cuts text that is too long with "...".             | always |
| `TF-006` | The task bar offers the actions from the interview, always in the same place.                                | always |
| `TF-007` | "Open as own window" hides the application header and opens the list in a new tab.                           | `Q-OWN-WINDOW` = yes |
| `TF-008` | A long form scrolls in its own area, and the task bar stays in place.                                        | always |
| `TF-009` | The form cannot be edited before the user has claimed the task.                                              | `Q-CLAIM` = hard |
| `TF-010` | The form can be edited without a claim.                                                                      | `Q-CLAIM` = informal |
| `TF-011` | When somebody else changes the open task, a toast tells the user, and it stays until clicked away.           | `Q-LIVE-MODE` ≠ `refresh-button` |
| `TF-012` | A task of type `EXTERNAL` opens the reported address in a window of its own, and the list stays intact.      | always |
| `TF-013` | The link from a task to its case opens the cockpit's case page.                                              | always |
| `TF-014` | A link from a notification mail opens the task page directly, after a login if needed.                       | `Q-NOTIFICATIONS` = yes |

## WL: the workflow list

| Id       | Item                                                                                          | Applies |
|----------|-----------------------------------------------------------------------------------------------|---------|
| `WL-001` | The workflow list offers the primary filters from the interview, `All`, `Active`, `Inactive`. | always |
| `WL-002` | Its fixed columns are the ones from the interview, in its order.                              | always |
| `WL-003` | Modules add their own workflow columns, apart from the task columns.                          | `Q-MODULE-COLUMNS` = yes |
| `WL-004` | There is no read marker in the workflow list.                                                 | always |
| `WL-005` | `accessibleToUsers` and `accessibleToGroups` are not shown as columns.                        | always |
| `WL-006` | The title opens the case page, and "open in new tab" works on it.                             | always |

## WP: the page of a case

| Id       | Item                                                                              | Applies |
|----------|-----------------------------------------------------------------------------------|---------|
| `WP-001` | Opening a case loads it and shows the module's workflow page.                     | always |
| `WP-002` | The case page has no task bar and no "open as own window".                        | always |
| `WP-003` | The workflow page gets `getUserTasks`, and it returns the user tasks of the case. | always |
| `WP-004` | Long content of the workflow page does not break the frame of the cockpit.        | always |
| `WP-005` | A case page that fails to load shows a note with a retry button.                  | always |

## SRCH: search, filter, sort and paging

| Id         | Item                                                                                                      | Applies |
|------------|-----------------------------------------------------------------------------------------------------------|---------|
| `SRCH-001` | Both lists have a full-text search with an x button that clears it.                                       | always |
| `SRCH-002` | Suggestions are asked from three characters on, 300 milliseconds after the last key.                      | always |
| `SRCH-003` | At most 20 suggestions show, and a note shows instead when the server returns 21.                         | always |
| `SRCH-004` | Special characters typed by the user are escaped before they go to the server.                            | always |
| `SRCH-005` | A filter can be set and removed per column, and only one filter per path is sent.                         | always |
| `SRCH-006` | A button resets all searches and filters.                                                                 | always |
| `SRCH-007` | A click on a sortable header sorts by it, a second turns the direction, a third goes back to the default. | always |
| `SRCH-008` | An `i18n` column sorts by `<path>.<language>`, a `person` column by `<path>.sort,<path>.id`.              | always |
| `SRCH-009` | Only columns with `sortable` offer sorting, and only columns with `filterable` offer a filter.            | always |
| `SRCH-010` | The list loads more rows in blocks when the user reaches the end.                                         | always |
| `SRCH-011` | Every request of a list after the first sends the `initialTimestamp` of the first answer.                 | always |
| `SRCH-012` | Every list request sets `pageSize` and the primary filter.                                                | always |

## LIVE: live updates

| Id         | Item                                                                                                   | Applies |
|------------|--------------------------------------------------------------------------------------------------------|---------|
| `LIVE-001` | A task created by somebody else appears in the open list without a reload, highlighted as new.         | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-002` | A changed task is updated in place and highlighted as changed.                                         | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-003` | A task completed while the user watches turns grey and stays in the list.                              | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-004` | A row the server no longer returns stays in place, marked as removed.                                  | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-005` | Scrolling and loading more rows work correctly while ended and removed rows are still shown.           | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-006` | The totals of the primary filters update after a change.                                               | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-007` | The acknowledge button removes all highlights and the inactive rows.                                   | `Q-LIVE-MODE` = `live` |
| `LIVE-008` | The highlight of a row goes away a few seconds after the mouse has left it.                            | `Q-LIVE-MODE` = `auto-acknowledge` |
| `LIVE-009` | Nothing updates by itself, and a refresh button becomes active when changes were reported.             | `Q-LIVE-MODE` = `refresh-button` |
| `LIVE-010` | A `RELOAD` event loads the list again with the same primary filter, filters and sorting.               | always |
| `LIVE-011` | A task whose follow-up date has come appears in `OpenTasksWithoutFollowUp` without a reload.           | `Q-FOLLOW-UP` = yes |
| `LIVE-012` | When the server ends the stream, the user interface connects again within one second.                  | always |
| `LIVE-013` | After a lost connection, the user interface connects again with a growing pause of at most 15 seconds. | always |
| `LIVE-014` | When connecting again answers 401, the user interface stops trying and sends the browser to the login. | always |
| `LIVE-015` | The selection of the user stays when the list updates.                                                 | `Q-BULK-ACTIONS` not empty |

## EXT: module parts and extension points

| Id        | Item                                                                                                            | Applies |
|-----------|-----------------------------------------------------------------------------------------------------------------|---------|
| `EXT-001` | Parts are loaded with the native federation orchestrator from `/wm/<workflowModuleId>/remoteEntry.json`.        | always |
| `EXT-002` | Each pair of module and exposed name is loaded at most once per page.                                           | always |
| `EXT-003` | A row with the `uiUriType` from the interview loads its parts through native federation.                        | always |
| `EXT-004` | A row with an unknown `uiUriType` is shown, and the user is told it cannot be opened here.                      | always |
| `EXT-005` | Every visual part renders inside a shadow DOM, and the cockpit's styles do not change it, nor it the cockpit's. | always |
| `EXT-006` | The host defines each module's element once, under a tag name the host chooses.                                 | always |
| `EXT-007` | A `bc-request` from a part is answered with exactly one `bc-response` with the same `requestId`.                | always |
| `EXT-008` | An unknown action is answered with the error code `UNKNOWN_ACTION`.                                             | always |
| `EXT-009` | An action the server refuses is answered with `REFUSED` and the HTTP status.                                    | always |
| `EXT-010` | Every extension point that is not about one row asks every module the user may see.                             | always |
| `EXT-011` | A module that does not expose an extension point causes no error and no message to the user.                    | always |
| `EXT-012` | The extension points from the interview work with a blueprint module that fills them.                           | `Q-EXTENSION-POINTS` |
| `EXT-013` | The theme reaches the module's shadow DOM as the CSS custom properties named in the types library.              | `Q-THEME-TOKENS` = yes |
| `EXT-014` | When the language changes, every visible part gets the new `language` property.                                 | `Q-LANGUAGE-SWITCH` = yes |
| `EXT-015` | A part that fails to load shows a note and a retry button.                                                      | always |

## I18N: language and texts

| Id         | Item                                                                                                | Applies |
|------------|-----------------------------------------------------------------------------------------------------|---------|
| `I18N-001` | All texts of the user interface are in separate files, one per language.                            | always |
| `I18N-002` | The user interface starts in the default language from the interview.                               | always |
| `I18N-003` | The user can switch the language, and the choice is kept.                                           | `Q-LANGUAGE-SWITCH` = yes |
| `I18N-004` | A missing translation falls back to the default language the same way in headers, cells and titles. | always |
| `I18N-005` | Numbers and dates are formatted for the chosen language.                                            | always |

## NOTI: notifications

| Id         | Item                                                                                               | Applies |
|------------|----------------------------------------------------------------------------------------------------|---------|
| `NOTI-001` | A menu entry leads to the notification settings.                                                   | `Q-NOTIFICATIONS` = yes |
| `NOTI-002` | The user chooses, for all cases and per kind of case, whether to be notified and on which channel. | `Q-NOTIFICATIONS` = yes |
| `NOTI-003` | The settings are stored with `PUT` and shown again after a reload.                                 | `Q-NOTIFICATIONS` = yes |

## DS: dev shell

| Id       | Item                                                                                                | Applies |
|----------|-----------------------------------------------------------------------------------------------------|---------|
| `DS-001` | There is one dev shell package for every framework from `Q-MODULE-FRAMEWORKS`.                      | always |
| `DS-002` | The dev shell imports the module's components directly, and the dev server swaps them while typing. | always |
| `DS-003` | The dev shell shows real data from the dev shell simulator.                                         | always |
| `DS-004` | The dev shell switches between parts, user tasks, cases and users.                                  | always |
| `DS-005` | A user task form and a workflow page look the same in the dev shell as in the cockpit.              | always |
| `DS-006` | A toast a part asks for is shown, also one that has to be clicked away.                             | always |
| `DS-007` | The developer can choose the language.                                                              | always |
| `DS-008` | A function the simulator does not answer tells the developer so instead of doing nothing silently.  | always |
| `DS-009` | `getUserTasks` returns the real user tasks of the case.                                             | always |
| `DS-010` | The dev server forwards `/official-api` and `/dev-shell` to the simulator.                          | always |
| `DS-011` | The dev shell has no symbol view and no copied list row.                                            | always |
