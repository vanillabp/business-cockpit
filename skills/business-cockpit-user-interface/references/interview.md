# The interview

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Ask these questions before you generate anything. Ask every one of them. Where a default is given,
propose it, and let the user decide. Write every answer into `INTERVIEW.md` in the generated
project, with the question id.

Each question has an id, for example `Q-FOLLOW-UP`. The checklist
[reference-1.0.0.md](reference-1.0.0.md) names these ids, so you can see which items depend on
which answer. Ids are never reused.

1. [The stack](#the-stack)
1. [The look and the words](#the-look-and-the-words)
1. [The screens](#the-screens)
1. [The lists](#the-lists)
1. [A user task and a case](#a-user-task-and-a-case)
1. [Live updates](#live-updates)
1. [Language](#language)
1. [Extension points](#extension-points)
1. [Backend and outputs](#backend-and-outputs)
1. [Tests](#tests)

## The stack

| Id                    | Question                                                                                                                                  | Default the agent proposes |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-FRAMEWORK`         | Which UI framework does the cockpit use, and which version?                                                                               | none, the user has to say |
| `Q-DESIGN-LIBRARY`    | Which component or design library, and which version? For example PrimeNG, PrimeVue, MUI, Tailwind.                                       | none, the user has to say |
| `Q-MODULE-FRAMEWORKS` | Which frameworks, and which versions, do the workflow modules use for their forms and pages? This decides which dev shells are generated. | the same as `Q-FRAMEWORK` |
| `Q-BUNDLER`           | Which build tool builds the cockpit? It does not matter for the federation, because the host only needs the runtime orchestrator.         | the default of the framework |

## The look and the words

| Id               | Question                                                                                                                                                                                                                                                                | Default the agent proposes |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-THEME`        | Colours, fonts, logo, spacing. Also the colours of a row that is new, changed or removed while the user watches, and the one colour of an inactive row. They also go into the dev shell base pages.                                                                     | the defaults of the design library |
| `Q-THEME-TOKENS` | Shall the theme be handed to module parts as CSS custom properties, so a form can look like the cockpit inside its shadow DOM?                                                                                                                                          | yes |
| `Q-WORDING`      | Which word does the user interface use for a business case? In English `Case` or `Workflow`, in German `Vorgang` or `Fall`. Pick one per language and never mix. Tell the user that in VanillaBP a workflow is the process instance, the run of a process for one case. | `Case` in English, `Vorgang` in German |

Use the word from `Q-WORDING` in every text of the generated user interface: the toggle in the
header, the list, the page, the messages, the menu. This description says "case" throughout.

## The screens

| Id                 | Question                                                                                                                                                                                                                                                           | Default the agent proposes |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-SCREENS`        | Which screens exist beyond the minimum? The minimum is `/tasks` (all user tasks the user may see), `/my-tasks` (those where the user is the assignee) and one case list. The header always has the toggle `My tasks` / `All tasks` / `Cases`, each with its count. | the minimum plus a dashboard |
| `Q-DASHBOARD`      | What does the dashboard show, if there is one? For example one pie chart of the open user tasks by task type and one of the open cases by kind. A click opens the list, narrowed.                                                                                  | two pie charts |
| `Q-SUB-LISTS`      | Are there sub-lists, narrowed to a role or to some task types? For example one card per role on the start page, with the number of visible and of claimed tasks of that role.                                                                                      | none |
| `Q-NOTIFICATIONS`  | Is there a screen where the user sets up notifications?                                                                                                                                                                                                            | yes |
| `Q-ROUTES`         | Which paths do the screens have, and are they translated? The pages of a workflow module always have the path `/<workflowModuleId>/<pageId>`.                                                                                                                      | English paths, not translated |
| `Q-VISIBLE-GROUPS` | Do some groups of users see other screens or menu entries? Which groups?                                                                                                                                                                                           | everybody sees the same |

## The lists

| Id                   | Question                                                                                                                                                                                             | Default the agent proposes |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-PRIMARY-FILTERS`  | Which primary filters does the task list offer, and which is the default? Possible: `OpenTasksWithFollowUp` (all open tasks), `OpenTasksWithoutFollowUp`, `OpenTaskOnlyFollowUp`, `ClosedTasksOnly`. | all four, default `OpenTasksWithFollowUp` |
| `Q-WORKFLOW-FILTERS` | Which primary filters does the case list offer, and which is the default? Possible: `All`, `Active`, `Inactive`.                                                                                     | `Active` and `Inactive`, default `Active` |
| `Q-FOLLOW-UP`        | Is there a follow-up feature (put a task aside until a date)?                                                                                                                                        | yes |
| `Q-CLOSED-TASKS`     | Are closed user tasks shown? They document with which input a task was completed.                                                                                                                    | yes |
| `Q-TASK-COLUMNS`     | Which fixed columns does the task list have, and in which order? For example id, case kind, title, assignee, candidate users, created, due, follow-up, ended, notes.                                 | id, title, assignee, candidate users, created, due, follow-up, ended, notes |
| `Q-WORKFLOW-COLUMNS` | Which fixed columns does the case list have, and in which order?                                                                                                                                     | id, title, created, ended, notes |
| `Q-MODULE-COLUMNS`   | Do the workflow modules add columns of their own (dynamic columns)? Some organisations want fixed columns only.                                                                                      | yes |
| `Q-BULK-ACTIONS`     | Which actions are offered for several selected tasks or cases? Claim, propose to a person (candidate user), mark as read or unread, follow-up, notes. A case list offers notes only.                 | claim, propose, read and unread, follow-up, notes |
| `Q-NOTES`            | Can user tasks and cases carry notes? A note is written per row in the list, for all selected rows in the list, and in the header of an open task or case. The endpoints come with prompt `1507`.    | yes |
| `Q-CLAIM`            | Is claiming informal, or hard (the form cannot be edited until the user claims the task)?                                                                                                            | hard |
| `Q-PAGE-SIZE`        | How many rows does one page of a list load? A list never holds all rows, only the pages around the visible area, see [behaviour.md](behaviour.md#paging-and-the-window-of-loaded-rows).              | 20 |

## A user task and a case

| Id                          | Question                                                                                                                                                                                                                                                                                                                                                    | Default the agent proposes |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-HEADER`                  | What does the application header show in the views where no user task is open?                                                                                                                                                                                                                                                                              | the title, the toggle of the lists, the user and the menu |
| `Q-TASK-HEADER-INFO`        | Which information about the open user task does the application header show? For example task kind, case kind, case number.                                                                                                                                                                                                                                 | task title, case title, business id |
| `Q-TASK-HEADER-ACTIONS`     | Which actions on the open user task does the application header offer? For example claim, follow-up, note, mark as unread, open as own window.                                                                                                                                                                                                              | claim, follow-up, note, mark as unread, open as own window |
| `Q-WORKFLOW-HEADER-INFO`    | Which information about the open case does the application header show? For example case kind and case number.                                                                                                                                                                                                                                              | case title, business id |
| `Q-WORKFLOW-HEADER-ACTIONS` | Which actions on the open case does the application header offer? For example note.                                                                                                                                                                                                                                                                         | note |
| `Q-OWN-WINDOW`              | Is there "open as own window"? The list opens in a new tab and the task stays in this one. The application header stays, because it holds the task actions. Only the burger menu goes, and a red "back to the list" button takes its place.                                                                                                                 | yes |
| `Q-PAGE-AREAS`              | Which areas does the cockpit lay out on the page of a user task and of a case? The convention is `body`, `information` (top of a column on the right) and `actions` (bottom of that column). A module may also return one web component instead and use the whole page, see [module-federation.md](module-federation.md#the-page-of-a-user-task-or-a-case). | `body`, `information`, `actions` |

## Live updates

| Id            | Question                                                                                                                                                                                                                                                                                                                                                                                            | Default the agent proposes |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-LIVE-MODE` | How do lists react to changes? `live`: rows update and are highlighted until the user acknowledges them with a button. `auto-acknowledge`: like `live`, but the highlight of a row goes away a few seconds after the mouse leaves it; this is an experiment to be validated. `refresh-button`: nothing updates by itself, a refresh button in the header becomes active when changes were reported. | `live` |

## Language

| Id                   | Question                                                                                                                                                                                            | Default the agent proposes |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-LANGUAGE-DEFAULT` | Which language is the default?                                                                                                                                                                      | English |
| `Q-LANGUAGE-SWITCH`  | Can the user switch the language? The switch sits in the menu, below the entries. The choice is kept as a user setting on the server, see [behaviour.md](behaviour.md#user-settings).               | no |
| `Q-LANGUAGES`        | Which languages are offered? The backend names them in `business-cockpit.additional-properties`, and `GET /app/info` returns them in `additionalProperties`, for example under the key `languages`. | only the default language |

## Extension points

| Id                   | Question                                                                                                                                                                                                                                                                                                                                                                  | Default the agent proposes |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-EXTENSION-POINTS` | Which extension points exist beyond the four minimum ones (the page of a user task, the page of a case, the columns of the task list, the columns of the case list)? An extension point gives modules a place for a function of their own. For example buttons in the header, which may open a modal dialog like a start form, or menu entries and the pages behind them. | header buttons |
| `Q-CELL-RENDERING`   | Shall workflow modules draw cells of a list themselves? This is an open question and not generated yet. Prompt `1500` works it out, see [module-federation.md](module-federation.md#cells-drawn-by-a-workflow-module).                                                                                                                                                    | no |
| `Q-UI-URI-TYPE`      | Which values of `ui-uri-type` does this user interface understand? The type decides how a row opens. They are a convention between this user interface and its workflow modules. VanillaBP knows no names for them. The types library lists them, and the types jar carries them as constants.                                                                            | `NATIVE_MF` for parts loaded through native federation, `EXTERNAL` for pages of a module's own, opened in a tab of their own |

## Backend and outputs

| Id                    | Question                                                                                                                                                                                                                                                                                                                             | Default the agent proposes |
|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-BACKEND`           | Which application serves the user interface? Usually an application of the organisation's own with the `business-cockpit` library, in a repository of its own. Which repository is it, and where is its `pom.xml`?                                                                                                                   | an application of the organisation's own, in a second repository |
| `Q-BACKEND-FRAMEWORK` | Which framework does the backend use? Today the cockpit is a Spring Boot application, and a Quarkus variant of the cockpit comes later. Read the answer from the `pom.xml` of the backend (`spring-boot` or `quarkus` in the parent, a BOM or the dependencies), and let the user confirm it. For Quarkus: is a native build wanted? | what the `pom.xml` says, else Spring Boot |
| `Q-LOGIN`             | How does a user log in? For example a login form of the backend, or OAuth2 through `/login`. Where does the user interface send a browser that has no login? Which paths does the login use, so the nginx image forwards them to the backend?                                                                                        | `/login`, `/logout`, `/oauth2/**`, `/login/oauth2/**` |
| `Q-COCKPIT-VERSION`   | Which version of the Business Cockpit does it run against? The GUI API description is taken from that version. It must have the prompts `1504` to `1507` and `1509`, see [rough-edges.md](rough-edges.md#what-needs-a-newer-cockpit).                                                                                                | the latest release |
| `Q-UI-JAR`            | Maven coordinates of the UI jar.                                                                                                                                                                                                                                                                                                     | `<organisation group>:business-cockpit-ui:<version>` |
| `Q-TYPES-JAR`         | Maven coordinates and Java package of the types jar, the small jar with the constants for `ui-uri-type`, and where it is built. It needs compiled Java, so it cannot be built in the npm-only UI repository. This is a proposal to be confirmed, see [project.md](project.md#the-types-jar).                                         | `<organisation group>:business-cockpit-ui-types:<version>`, package `<organisation group>.cockpit.ui`, a Maven module in the backend repository |
| `Q-MAVEN-REPOSITORY`  | Into which Maven repository is the UI jar uploaded? Which address takes releases, and which environment variables hold the credentials?                                                                                                                                                                                              | the company repository, for example Nexus or Artifactory |
| `Q-NPM-SCOPE`         | The npm scope and names of the published libraries (types library, dev shells), and the npm registry.                                                                                                                                                                                                                                | `@<organisation>/bc-types`, `@<organisation>/bc-dev-shell-<framework>` |

## Tests

| Id               | Question                                                                                                                                                                                                  | Default the agent proposes |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-E2E-TOOL`     | Which tool runs the end-to-end tests?                                                                                                                                                                     | Playwright |
| `Q-MEMORY-LIMIT` | How much JavaScript heap may the browser use after the memory test has scrolled through a list of 20,000 cases from top to bottom? See [behaviour.md](behaviour.md#paging-and-the-window-of-loaded-rows). | 150 MB |
