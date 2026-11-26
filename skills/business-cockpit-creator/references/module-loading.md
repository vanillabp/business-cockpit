# Loading the parts of a workflow module

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The cockpit shows lists. What a user sees after a click on a row comes from the workflow module.
The module builds its parts as plain ES modules, and the cockpit loads them into its own pages at
runtime with `import()`. Nothing else is needed: no import map, no shim and no federation library.

The wiki page
[Loading the parts of a workflow module](https://github.com/vanillabp/business-cockpit/wiki/Loading-the-parts-of-a-workflow-module)
is the short version of this description for the authors of workflow modules, and the cockpit
frontend blueprints link it. Here is the full description of what the host does. The host is your
user interface. When you change a rule here, change the wiki page with it.

1. [Self-contained ES modules](#self-contained-es-modules)
2. [The manifest](#the-manifest)
3. [Building the entries](#building-the-entries)
4. [The rules](#the-rules)
5. [Extension points](#extension-points)
6. [Data entries carry no framework](#data-entries-carry-no-framework)
7. [Which data finds the part](#which-data-finds-the-part)
8. [One registry and one loader](#one-registry-and-one-loader)
9. [Ask, load, draw](#ask-load-draw)
10. [A new build of a module](#a-new-build-of-a-module)
11. [Nothing is shared](#nothing-is-shared)
12. [What loading costs](#what-loading-costs)
13. [A visual part is a web component](#a-visual-part-is-a-web-component)
14. [The names of the web components](#the-names-of-the-web-components)
15. [The page of a user task or a case](#the-page-of-a-user-task-or-a-case)
16. [The other extension points](#the-other-extension-points)
17. [The event protocol](#the-event-protocol)
18. [Cells drawn by a workflow module](#cells-drawn-by-a-workflow-module)

## Self-contained ES modules

An entry is one part a module offers, for example `UserTaskList`. Every entry is an ES module that
brings everything it needs. It imports only files of its own module, by relative paths. It has no
bare import like `import 'react'` that the page would have to resolve, and it expects nothing from
the cockpit or from another module.

The browser loads such a file with `import(url)` and caches it by its address. That is all the
cockpit uses. So the cockpit and each workflow module pick their own framework, their own version
and their own build tool. A Vue cockpit shows a React form and an Angular case page on the same
page, and a module upgrades its framework without asking anybody.

The entries of one module may share files. The bundler splits the framework into one chunk, and
every entry of the module imports that chunk. The browser loads the chunk once, because it is the
same address. So a module's framework is loaded once per module, not once per form, per page or per
cell. Such a shared chunk must never be imported by a data entry, see
[rough-edges.md](rough-edges.md#a-shared-chunk-carries-the-framework-into-a-data-entry).

The address of an entry lies below `/wm/<workflowModuleId>/`, where the cockpit serves the module.
It is the same origin as the cockpit, so a Content Security Policy needs nothing beyond
`script-src 'self'`.

## The manifest

A module describes its entries in one small file, `bc-manifest.json`, at the root of its web path.
The cockpit reaches it at `/wm/<workflowModuleId>/bc-manifest.json`. The proposal:

```json
{
  "manifestVersion": 1,
  "buildVersion": "1.4.0",
  "buildTimestamp": "2026-10-16T09:30:00Z",
  "entries": {
    "UserTaskList": "entries/user-task-list-3f9a1c2b.js",
    "WorkflowList": "entries/workflow-list-77e0d415.js",
    "UserTaskForm": "entries/user-task-form-8b21de90.js",
    "WorkflowPage": "entries/workflow-page-c40a7f13.js",
    "Header": "entries/header-1d9e5a60.js",
    "AssessRiskForm": "entries/assess-risk-form-5c0e7a21.js",
    "ConfirmRideForm": "entries/confirm-ride-form-a93d40b6.js",
    "RidePage": "entries/ride-page-0f6b2c85.js",
    "HeaderElements": "entries/header-elements-e2b81c4f.js"
  }
}
```

|       Field       |                                        Meaning                                         |
|-------------------|----------------------------------------------------------------------------------------|
| `manifestVersion` | the version of this format; `1` today; a cockpit refuses a higher one it does not know |
| `buildVersion`    | the version of the module's build; the loader compares it to notice a new build        |
| `buildTimestamp`  | when the module was built; the loader compares it the same way                         |
| `entries`         | entry name to file; the file is relative to the manifest                               |

A field this version does not know is allowed and ignored. A file in `entries` must stay below the
web path of the module. So it is a relative path: no whole address with a scheme like `https:`, no
address that starts with `//`, no path that starts with `/`, and no `..` in it. `?v=...` or `#...`
at its end is allowed. The kit's `checkManifest` and your loader apply the same rules, and the
loader refuses a manifest that breaks them, with a note that names the module.

Cache busting works through the file names. The bundler puts a hash of the content into every file
name, so a new build has new addresses, and the browser cannot take an old file for a new one. A
bundler that cannot do that appends `?v=<build id>` to the file in the manifest, with the build id
of [The names of the web components](#the-names-of-the-web-components). `buildVersion` is not
enough for this, because a version like `1.4.0-SNAPSHOT` stays the same over many builds. Both ways
give every build new addresses, and that is what lets the cockpit load a new build without a reload
of the page, see [A new build of a module](#a-new-build-of-a-module). The manifest itself has a
fixed name, so it is never cached: the module serves it with `Cache-Control: no-cache`, and the
loader asks for it with `fetch(url, { cache: 'no-cache' })`.

The entry names are the keys. They have no `./` in front. That prefix comes from Webpack's module
federation and means nothing here. The names of the data entries are fixed, and the types library
names them as constants, see [types.md](types.md#the-extension-points). The names of the visual
entries, here `AssessRiskForm`, `ConfirmRideForm`, `RidePage` and `HeaderElements`, are the
module's choice. A data entry names them, see
[Data entries carry no framework](#data-entries-carry-no-framework). The names of the web
components a visual entry defines are not free. They start with the module id, see
[The names of the web components](#the-names-of-the-web-components).

## Building the entries

Any bundler that writes ES modules works. Two things the build must do: write one file per entry
with the content hash in the name, and write `bc-manifest.json` from the bundler's own list of
outputs. A short script of ten lines does the second, for every bundler below. Nothing is marked as
external, because the entries share nothing with the page.

|    Framework     |                                                                                                                                                                                                                                                                                                                    Recipe                                                                                                                                                                                                                                                                                                                     |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| React            | Vite in library mode: `build.lib.entry` is an object of entry name to source file, `formats: ['es']`, `build.manifest: true`. Set `define: { 'process.env.NODE_ENV': '"production"' }`, because library mode leaves it alone, and React reads it. Or esbuild: `--bundle --format=esm --splitting --entry-names=[name]-[hash] --metafile=meta.json`.                                                                                                                                                                                                                                                                                           |
| Vue              | esbuild as for React, with a small plugin that hands every `.vue` file to `vue/compiler-sfc`: `parse`, then `compileScript` with `inlineTemplate: true`, and the TypeScript it returns goes back to esbuild. The compiler comes with the package `vue`, so the build needs no other dependency, and `--metafile` works as for React. The Vue blueprint `cockpit-user-task-frontend-vue` builds this way. Vite in library mode with `@vitejs/plugin-vue` works as well, but it writes no metafile. Check the types with `vue-tsc`, and see [rough-edges.md](rough-edges.md#vue-in-a-shadow-root) for its `strictTemplates` and for the styles. |
| Angular          | The Angular CLI with the `application` builder of `@angular/build`, as every Angular project builds, see [Angular entries with the Angular CLI](#angular-entries-with-the-angular-cli). One build for all entries: an entry point with one `import()` per entry, `index: false`, no polyfills, `statsJson: true` for the list of outputs. Zoneless, see [Nothing is shared](#nothing-is-shared). Do not build Angular with plain esbuild: it cannot compile Angular components, and adding the Angular compiler and linker by hand is a build of your own which nobody on the web knows.                                                      |
| Plain JavaScript | esbuild or Rollup with `output.format: 'es'`. This is the recipe for every data entry, whatever the framework of the module's forms.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |

The data entries need no framework compiler, see
[Data entries carry no framework](#data-entries-carry-no-framework). So a module can build them with
plain esbuild, apart from its forms. A module whose forms are Angular may also build them with the
Angular CLI, together with the forms, as below.

### Angular entries with the Angular CLI

The Angular CLI builds one application from one entry point. It also writes every module the
application loads with `import()` as a file of its own, with the hash of its content in the name, as
it does for a lazy loaded route. The code these files share, Angular itself, goes into a shared
chunk. So an entry point which imports each entry with `import()` gives one ES module per entry. The
cockpit never loads the entry point itself, only the entries the manifest names:

```ts
// src/entries.ts, the "browser" entry point in angular.json
export const entries = {
  UserTaskForm: () => import('./data/user-task-form'),
  AssessRiskForm: () => import('./forms/assess-risk-form'),
};
```

The options of the `application` builder in `angular.json`:

```json
{
  "builder": "@angular/build:application",
  "options": {
    "browser": "src/entries.ts",
    "index": false,
    "polyfills": [],
    "outputPath": { "base": "dist/micro-frontends", "browser": "" },
    "outputHashing": "all",
    "namedChunks": true,
    "statsJson": true
  }
}
```

`statsJson` writes `browser-stats.json`, the metafile of the esbuild inside the CLI. Its `outputs` name
each file relative to the output folder, and `entryPoint` names the source file of an entry. The
script after `ng build` takes the file of each entry from there and writes `bc-manifest.json`, the
same ten lines as for esbuild. Move `browser-stats.json` out of the output folder, and pass it to
`bc-check-entries` with `--metafile`. The CLI compiles ahead of time, checks the templates, and
reads the `paths` of `tsconfig.json`, so the build needs nothing of its own for these. The files
land in one folder, without the `entries/` and `chunks/` of the esbuild recipe. The Angular
blueprint `cockpit-user-task-frontend-angular` builds this way.

Measured with Angular 22.2 on a form with one field and one button: the build with the Angular CLI
runs ahead of time and zoneless, and it loads through the manifest with `import()` as the esbuild
builds do. Its shared chunk was a third of the size of
the same form built with the Angular compiler, the linker and esbuild by hand, because the CLI also
runs Angular's own optimisations. Plain esbuild without the Angular compiler fails in the browser:
Angular then needs its compiler at runtime, and even with it, signal inputs do not work.

What the ways of loading cost in time, size and memory, for a module in React, Vue and Angular,
was measured with a neutral host, see [What loading costs](#what-loading-costs). How the framework
of the cockpit changes the numbers is still open. Cells drawn by a module need a measurement of
their own, see [Cells drawn by a workflow module](#cells-drawn-by-a-workflow-module).

## The rules

Every entry is loaded once for each build of its module. A part is the pair of a workflow module
and an entry name. The host keeps one promise per pair and hands out the same one to every caller,
also to a caller that comes while the load is still running. Only a new build of the module starts
again, see [A new build of a module](#a-new-build-of-a-module).

A part never comes from the cockpit's own bundle. The cockpit never contains a user interface part
of a business process, see [`../SKILL.md`](../SKILL.md#what-a-cockpit-user-interface-never-does).

A data entry exports plain JavaScript functions. A function that stands for something visible
returns the names of web components and the visual entry that defines them. A visual entry exports
nothing. When the browser evaluates it, it defines its web components, and each of them draws into
its own shadow DOM.

Every part comes into the page in the same three steps: ask, load and draw, see
[Ask, load, draw](#ask-load-draw).

Nothing is shared between the host and the modules, except the module kit and DOM events, see
[Nothing is shared](#nothing-is-shared).

## Extension points

An extension point is a place in the cockpit where a workflow module may put a part. The general
functions of a cockpit, like the lists, claiming or the notifications, are built into the cockpit's
frontend and backend. They are no extension points. An extension point gives a module a place for
something only this module has, outside the page of a user task and the page of a case.

Four extension points are the bare minimum, and every generated cockpit has them. They are the
four data entries of the VanillaBP kit `@vanillabp/bc-module-kit`, with the same names of their
exports, see [types.md](types.md#the-vanillabp-kit-is-the-minimum):

|     Entry      |                               What the module exports                                | Kind | Where the cockpit uses it |
|----------------|--------------------------------------------------------------------------------------|------|---------------------------|
| `UserTaskList` | `userTaskListColumns`, a function from a user task to its columns                    | data | the task lists            |
| `WorkflowList` | `workflowListColumns`, a function from a case to its columns                         | data | the case list             |
| `UserTaskForm` | `userTaskForm`, a function from a user task to a visual entry and its web components | data | the page of a user task   |
| `WorkflowPage` | `workflowPage`, a function from a case to a visual entry and its web components      | data | the page of a case        |

An organisation may add more (`Q-EXTENSION-POINTS`). The interview proposes `Header`, buttons in
the header, because every customer so far has had it. It proposes no `Pages`, pages of a module
with their own menu entries, because no customer has had them yet. An example of such a page is
one to keep the master data a process needs. Both may open modal dialogs, see
[The other extension points](#the-other-extension-points). For the cockpit there is no difference
between the four and the others. They all come from the same organisation's generated user
interface. The cockpit and the modules of one organisation agree on the list, and it holds for
every module. Write it into the types library, each entry with the name of its function in
`EXPORT_NAMES`, and the module kit exports it again, see
[types.md](types.md#the-extension-points).

An extension point outside the lists, the page of a user task and the page of a case, like the
header, asks every module the user may see. The four minimum ones only ask the module of the row,
the user task or the case. A module whose manifest does not name the entry is no error. It is
ignored, without a message to the user. Only an entry that is named and fails to load is worth a
note.

The other way round holds too. A manifest may name an entry this cockpit does not know, for example
`Header` in a cockpit without a header. The cockpit never asks or loads it, and shows no error. So
a blueprint module that offers `Header` works in every cockpit, and in a cockpit without that
extension point only the header part is missing. The same holds for areas, see
[The page of a user task or a case](#the-page-of-a-user-task-or-a-case).

The entry name is the contract, and so are the names of the exports. The host takes the name of
the function from `EXPORT_NAMES` of the types library, never from a name written into its own
code. A module that names
`UserTaskForm` but calls its export differently is loaded and shows nothing. Report that in the
console with the module id.

## Data entries carry no framework

There are two kinds of entries:

- A data entry tells the cockpit something: the columns of a row, which form a user task gets,
  which page a case gets, the pages of a module and their menu entries, the buttons of the header.
  `UserTaskList`, `WorkflowList`, `UserTaskForm` and `WorkflowPage` are data entries, and so are
  `Header` and `Pages` in a cockpit that has them.
- A visual entry defines web components that draw something: the forms of the user tasks, the
  pages of the cases, the buttons of the header, the pages of a module and the dialogs. The name of
  the visual entry is the module's choice, and a data entry names it together with its web
  components. The names of the web components must start with the module id, see
  [The names of the web components](#the-names-of-the-web-components).

A data entry is minimal JavaScript without a framework. It loads without the framework files of its
module. That matters, because the cockpit loads data entries of many modules at once: the columns
for every module in a list, the header and the pages for every module the user may see. A task list
with rows of ten modules must not load ten frameworks before it draws its first row.

`UserTaskForm` and `WorkflowPage` are data entries for a second reason. A module has many forms,
usually one per process and task definition. The function of `UserTaskForm` decides which one a
user task gets: it looks at `bpmnProcessId` and `taskDefinition` and names the visual entry of that
form. So every form is a visual entry of its own and a file of its own, and the cockpit loads only
the form the user opens. Lazy loading is built in, and the module does not need a component of its
own that picks the form at runtime. A `WorkflowPage` does the same per process.

```js
// UserTaskForm: plain JavaScript, it imports no form
import { names } from '../element-names.js';

export const userTaskForm = (userTask) => {
  if (userTask.bpmnProcessId !== 'TaxiRide') return undefined;
  switch (userTask.taskDefinition) {
    case 'assessRisk':
      return {
        entry: 'AssessRiskForm',
        element: {
          body: names.userTask('TaxiRide', 'assessRisk', 'form'),
          information: names.userTask('TaxiRide', 'assessRisk', 'information'),
          actions: names.userTask('TaxiRide', 'assessRisk', 'actions'),
        },
      };
    case 'confirmRide':
      return { entry: 'ConfirmRideForm', element: names.userTask('TaxiRide', 'confirmRide', 'page') };
    default:
      return undefined;
  }
};
```

The function names web components by their names, as strings. It never imports a form. An import
would pull the form and its framework into the data entry. `names.userTask` builds the whole name
from the module id, the build, the BPMN process id, the task definition and the part, see
[The names of the web components](#the-names-of-the-web-components). It is plain JavaScript and
keeps the data entry small.

Tree-shaking keeps a data entry small. The bundler drops every import the entry does not use. A
helper that a data entry imports by mistake from a file that also imports React drops out as long
as the helper itself does not need React. A real dependency on the framework stays, and that is
what the check below finds.

The check runs in the build of the module. The module kit ships a small script without
dependencies, `bc-check-entries`, the same as the one in the VanillaBP kit. It reads
`bc-manifest.json` and fails the build when:

- the manifest breaks a rule of [The manifest](#the-manifest), or an entry it names has no file,
- a data entry, together with every file it imports, is larger than the limit from the interview
  (`Q-DATA-ENTRY-LIMIT`, by default 32 KB before compression; a kilobyte is 1024 bytes),
- a data entry, or a file it imports, imports a bare name like `react`. An entry imports only
  files of its own module, by relative paths,
- a data entry, or a file it imports, imports a file outside the folder of the manifest, or a file
  that does not exist,
- a data entry, or a file it imports, contains code of a framework package.

The script follows static imports and dynamic imports with a fixed string, so a chunk which a data
entry loads later counts as well. It finds a framework in two ways:

- By the markers a framework leaves in a bundle, also in a minified one. These are
  `@license React`, `react.transitional.element` and the two internal names of React which start
  with `__CLIENT_INTERNALS_` and `__SECRET_INTERNALS_`, `__vue_app__` and `__v_isRef` for Vue,
  `@license Angular` and `ɵɵdefineComponent` for Angular, `__zone_symbol__` for `zone.js`, and
  `litHtmlVersions` for Lit.
- By the inputs of the bundler's metafile, when the build passes it with `--metafile`. An input
  from one of the packages `react`, `react-dom`, `scheduler`, `vue`, `@vue/...`, `@angular/...`,
  `zone.js`, `preact`, `svelte`, `solid-js`, `lit`, `lit-html`, `lit-element` or `@lit/...` fails
  the check. esbuild writes such a file with `--metafile`, the Angular CLI with `statsJson`.

The limit holds for the data entry only, not for a form. A form is a visual entry, and it may be as
large as its framework makes it. A visual entry is only checked for its file.

The end-to-end tests of the cockpit check the same from the other side: loading the data entries of
an example module of the kit fetches no file of the module's framework.

The types library has one list of the data entries: `EXPORT_NAMES`, which maps each data entry to
the function its file exports. `DATA_ENTRIES` is only the keys of it, see
[types.md](types.md#the-extension-points). The module kit exports both again. The module host
reads the name of the function it calls from `EXPORT_NAMES`, and the script takes its data entries
from the same `EXPORT_NAMES`, so the script and the cockpit always agree. A module needs
`--data-entry <name>` only for a data entry of its own that no extension point asks. A blueprint
built against the VanillaBP kit names `Header` that way, because the `EXPORT_NAMES` of that kit has
only the four minimum ones.

## Which data finds the part

Three fields of each user task and each case from the GUI API say what to load. They have the same
names in both objects:

|       Field        |                                        Meaning                                         |
|--------------------|----------------------------------------------------------------------------------------|
| `workflowModuleId` | the workflow module the row comes from                                                 |
| `uiUriType`        | a type agreed between this user interface and the module; it decides how the row opens |
| `uiUri`            | the `uiUriPath` the module reported, unchanged                                         |

First the address. It is built from `uiUri` by one rule, see
[gui-api.md](gui-api.md#what-the-cockpit-serves):

- A `uiUri` that starts with `http://` or `https://` is a whole address and stays as it is.
- Every other `uiUri` is a path of the module. The address is `/wm/<workflowModuleId>/` plus this
  path.

Then `uiUriType` decides what happens with the address. Only the type decides, never the form of
the address. A convention like "a whole address opens in a new tab" breaks sooner or later, and a
type is safer. The server stores the value and never compares it to anything. VanillaBP knows no
names for it. The values are a convention between this user interface and its workflow modules
(`Q-UI-URI-TYPE`). The types library lists them, and the types jar carries them as Java constants,
see [project.md](project.md#the-types-jar). The defaults:

- `ES_MODULES`: the address is the manifest of the module. Usually the module reports the path
  `/bc-manifest.json`, so the cockpit loads the parts named in
  `/wm/<workflowModuleId>/bc-manifest.json`.
- `EXTERNAL`: the address is a page of the module's own. The cockpit opens it in a window or tab of
  its own and loads nothing into its own page. The list stays where it is.

An unknown `uiUriType` is no error of the server. It is a module built for another user interface.
Show the row anyway, and tell the user that this row cannot be opened here.

## One registry and one loader

No list, no page and no header loads a module part on its own. One central place does it, the
module host `<scope>/bc-module-host` of the UI repository, see
[project.md](project.md#the-module-host). The user interface wraps it in the code of its framework.
The tests of a workflow module use the same package, so a module is tested the way the cockpit loads
it.

1. The registry. After `GET /app/current-user`, the user interface calls
   `GET /gui/api/v1/workflow-module` once. It lists the modules the user may see. The answer stays in
   memory, for every part of the user interface. A module whose `uri` is empty has no web path and
   nothing to load.
2. The loader. It is the only code that calls `import()` for a module. A component only asks it:
   "give me the entry `UserTaskForm` of the module `taxi-ride`". The loader answers with a promise
   of the entry's exports. For a visual entry the promise only says that the entry was evaluated,
   because a visual entry exports nothing.

What the loader does:

1. It reads the manifest of a module when a part of that module is first needed, not before:
   `fetch('/wm/<id>/bc-manifest.json', { cache: 'no-cache' })`. Each module is read on its own, so
   that a module that fails to answer does not stop the others. A module without a manifest (404)
   has no parts to load. The loader remembers that and does not ask again in this session.
2. A row of a module that is not in the registry yet, because the module registered after the page
   loaded, makes the loader call `GET /gui/api/v1/workflow-module/{id}` for that one module. It adds
   the module to the registry and reads its manifest the same way.
3. `import(new URL(file, manifestUrl).href)` returns the entry's exports. The loader keeps a map
   per module, from entry name to the promise of the load. It puts the promise into the map before
   the file is fetched, in the same synchronous step that calls `import()`. So a second caller,
   also one that comes while the first load still runs, gets the same pending promise, and the file
   is fetched once. The manifest read of step 1 is shared the same way: one pending read per
   module.
4. Before it asks a part of a module, it reads the manifest of that module again when its last read
   is older than a minute, and drops what it loaded from an older build, see
   [A new build of a module](#a-new-build-of-a-module).

The address of an `import()` is only known at runtime, so the cockpit's own bundler must leave it
alone. Write the hint of the bundler next to it, `/* @vite-ignore */` for Vite or
`/* webpackIgnore: true */` for Webpack. esbuild and the Angular CLI leave such an import alone by
themselves.

The browser keeps a loaded ES module for the life of the page. A module that is deployed again
while a tab is open is still loaded fresh, without a reload of the page, because the new manifest
names new files, see [A new build of a module](#a-new-build-of-a-module).

### Every group which works with a module may see it

The registry holds only the modules the user may see, and the loader loads parts only of modules in
the registry. A row of a module outside it makes the loader ask
`GET /gui/api/v1/workflow-module/{id}`, and that answers 404 for a user who may not see the module.
So a user who is a candidate of a task, but may not see the task's module, finds the task in the
list and gets no form. Nothing in the cockpit is wrong then. The module named too few groups.

The rule for a workflow module: every candidate group of one of its user tasks, and every group
which sees one of its cases (`accessibleToGroups` of the case), is an access group of the module.
The module names its access groups in `getAccessibleToGroups` of its `WorkflowModuleDetailsProvider`.
A group which stands for an access group in the group hierarchy of the module counts as well.

Do not confuse an access group with a feature role. The group whose members see the button which
starts a process is a feature role: the button checks it, and the endpoint behind the button checks
it again. If the module registers only this role, every other user loses its forms. The blueprint
`cockpit-header-extension-backend` keeps the two apart: one access group in `getAccessibleToGroups`,
and a group hierarchy in which the feature role stands for the access group.

Prove it when you connect a module: for each candidate group and each group of a case, a test logs
in a user who is only in that group, opens a user task or a case of the module and sees its form or
page drawn. The dev shell shows the same before any cockpit runs: choose such a user, and the start
page must list the module (`DS-024`).

## Ask, load, draw

Every part of a module comes into a page in three steps. The steps do not depend on each other.
Each one has its own loading state and its own error. They are the same everywhere: on the page of a
user task, on the page of a case, in the header, on a page of a module and in a dialog.

1. Ask. The cockpit asks the loader for the data entry, for example `UserTaskForm`, and calls its
   function with the user task, the case or the current user. The answer is `undefined`, or the
   names of web components together with the visual entry that defines them, see
   [types.md](types.md#the-extension-points). Nothing visible is loaded yet. The cockpit checks
   every name of the answer before it goes on. A name that is no valid name of a custom element, or
   that does not start with the prefix of this module, is refused, see
   [The names of the web components](#the-names-of-the-web-components).
2. Load. First the cockpit asks the browser with `customElements.get(name)` whether every web
   component it is going to draw is defined already, from the build of the module the loader holds
   now. The loader remembers which build defined which name: it notes the names once
   `whenDefined` resolved for them after it loaded a visual entry. If all of them are defined, the
   cockpit skips this step and draws at once, without a loading state, without `import()` and
   without waiting. This is the usual case for the second task of the same kind. Otherwise the
   cockpit asks the loader for the visual entry the answer names. The loader imports it through
   the manifest, the same way as a data entry. When the browser evaluates the entry, the
   entry calls `customElements.define` for its web components. The cockpit then waits for each
   web component it is going to draw with `customElements.whenDefined(name)`, together with a
   time limit, by default 10 seconds. An entry may define its web components a little later, for
   example after its own setup, and that is fine within the limit.
3. Draw. The cockpit creates the element with `document.createElement(name)` in its area, sets the
   properties and listens for its requests, see [What a part gets](types.md#what-a-part-gets). For a
   user task it hands over the plain data, and the wrapper of the module turns it into `BcUserTask`.

What the user sees in each step:

| Step |           Loading state           |                   Error                    |                    What the user sees on the error                     |
|------|-----------------------------------|--------------------------------------------|------------------------------------------------------------------------|
| ask  | the data entry is being loaded    | the data entry fails to load               | a note and a button that loads again                                   |
| ask  |                                   | the function answers `undefined`: not mine | see below; no error                                                    |
| ask  |                                   | a name breaks the rule of the prefix       | a note that names the module, the name and the prefix                  |
| load | the visual entry is being loaded  | `import()` of the visual entry fails       | a note and a button that loads again                                   |
| load | the cockpit waits for the element | a web component is not defined in time     | a note that names the module, the entry and the web component          |
| load |                                   | an earlier build defined the name already  | a note that the module was updated, and a button that reloads the page |
| draw | none; the part shows its own      | creating the element throws                | a note that names the module and the web component                     |

"Not mine" is no error. On the page of a user task and the page of a case only the module of the
row is asked, so the page says that this module has no form for it. In the header and on the pages
the cockpit asks the next module, and a module that answers `undefined` adds nothing. A data entry
whose manifest does not exist, or an `EXTERNAL` row, never reaches the first step: the cockpit
shows only what it knows itself.

A failed load stays failed until the user tries again. Retrying asks the loader again for this
pair of module and entry. The loader drops the failed promise, reads the manifest again and calls
`import()` again. The browser does not cache a failed `import()`, so a second try really asks the
server. If it fails again, offer to reload the page. A web component that was not defined in time
is a bug of the module, not of the network. Write the module id, the entry and the name of the web
component to the console as well, so the author of the module finds it.

The cockpit runs the steps only when the part is about to be shown. It does not preload: not when
the mouse rests on a row of a list, and not when a row gets the focus. The page of a user task or
a case loads its entries when it opens, and the short wait that this causes is fine. What was
loaded is used again. The loader keeps the promise per pair of module and entry, so a part that is
shown a second time, for example the next task of the same kind, loads no file again.

No screen needs preloading today. Because the steps stand alone, a cockpit could run the first two
early, for example when the mouse rests on a row, through the same loader and the same promises.
That example only shows the principle. Do not build it unless a screen really needs it.

To know that a part is loaded, the cockpit needs neither `buildVersion` nor `buildTimestamp`. The
loader uses them to notice a new build, see [A new build of a module](#a-new-build-of-a-module).

## A new build of a module

A workflow module is deployed again while users have the cockpit open. That happens with every
update of a module. The cockpit then drops what it loaded from the module and loads the new build
when a part of it is needed next. The user does not have to reload the page.

### How the cockpit notices a new build

The manifest is the only place that names the build of a module. The cockpit server does not know
it: the registration of a module (`POST /bpms/api/v1_1/workflow-module/{id}`) carries its address
and its groups, nothing about its build, and the update stream sends no event when a module
registers again. So the loader reads the manifest again.

- Before it asks a part of a module, the loader looks at when it read the module's manifest last.
  If that is longer ago than `MANIFEST_RECHECK_MS`, a named constant of the module host with the
  default of 60 seconds, it reads the manifest again and waits for the answer. Within that time it
  does not ask again, however many parts it loads.
- It asks with `fetch(url, { cache: 'no-cache' })`. The module answers `304 Not Modified` when the
  file did not change, as long as it sends an `ETag` or a `Last-Modified` header. So a check costs
  one small request.
- It compares `buildVersion` and `buildTimestamp` with those of the manifest it loaded from. If one
  of them differs, the module has a new build. A manifest without both is compared by its `entries`
  instead, because their files carry the hash of their content.
- If the read fails, the loader goes on with the manifest it has and tries again at the next ask.

A cockpit on which the user does nothing that asks a part notices the new build at the next ask.
That is enough, because a part nobody uses does nothing.

### What the cockpit does then

1. The loader drops everything it holds for this module: the old manifest and every promise of its
   entries, data entries and visual entries. It keeps the new manifest.
2. Web components already on the page stay as they are until their page is left. A form the user
   is filling in is not swapped while they type. Rows drawn already keep their columns until the
   list asks for them again.
3. The loader tells its users that the module has a new build. The header and the menu ask their
   data entries `Header` and `Pages` again and draw again.
4. The next ask goes to the new build. The loader imports the data entry from its new address, the
   function answers with the new names, and the loader imports the new visual entry, which defines
   the new names. The old web components stay defined in the browser until the page is reloaded.
   They cost a little memory and do nothing.

### Why the names of the web components carry the build

Two things of the browser decide how this can work:

- `import()` keeps a module per address for the life of the page, and nothing empties that cache.
  A new build therefore needs new addresses. The hash in the file names gives it them, and so does
  `?v=<build id>` for a bundler without hashes, see [The manifest](#the-manifest).
- A name of a custom element can be defined once per page. `customElements.define` throws a
  `NotSupportedError` for a name that is taken, and there is no way to remove a definition. The new
  visual entry of a new build therefore needs new names.

That is why the cockpit asks for names that hold the build id after the prefix of the module, see
[The names of the web components](#the-names-of-the-web-components). It works in React, Vue and
Angular alike, because `defineBcElement` takes the name as a plain string, whatever the framework
draws inside. It also works with both ways of the manifest, file names with a hash and `?v=`, and
in the tests of a module that load its parts with the module host.

Other ways do not fit:

- A wrapper element of the cockpit that imports the module again and swaps an inner element. The
  inner element still needs a name of its own for each build, so the wrapper adds a layer and solves
  nothing.
- A cockpit that changes `customElements.define` to rename the elements while a visual entry loads.
  When several modules load at the same time, it cannot tell which call belongs to which module.
  And a module that uses its own web components inside its forms would break.
- Scoped registries (`new CustomElementRegistry()`). They allow the same name twice, but not every
  browser supports them yet, and every framework would have to define its elements in such a
  registry.

### A module whose names carry no build

The cockpit cannot force a module to build its names with the build id. A module may write its
names by hand, for example. The blueprints and the example modules of the VanillaBP kit build them
with `createElementNames`, so their names carry the build. A module without the build id in its
names works as long as it is not deployed again.

After a new build, the data entry of such a module answers with a name the loader knows from the
earlier build. Loading the new visual entry would throw in `customElements.define`, and the old
class would still answer for the name. So the cockpit does not load it. It shows a note instead:
the module was updated, and the page has to be reloaded to show its new version, with a button
that reloads the page. It writes the module id and the name of the web component to the console.
A name that the earlier build never defined in this page loads as usual.

### The same names as the VanillaBP host

The VanillaBP host `@vanillabp/bc-module-host` does all of this, so a module can test it. Its option
`manifestRecheckMs` sets the time after which it reads the manifest again, `MANIFEST_RECHECK_MS` by
default, and a test sets it to a few hundred milliseconds. Its callback `onNewBuild(moduleId,
manifest, previous)` tells its users that a module has a new build. For a name an earlier build
defined, it throws a `BcHostError` with the code `ALREADY_DEFINED`. For a name the cockpit refuses,
see [The cockpit checks every name](#the-cockpit-checks-every-name), it throws one with the code
`WRONG_ELEMENT_NAME`. The generated module host keeps these names, so a module tested with the
VanillaBP host meets the same behaviour in the cockpit.

## Nothing is shared

The host shares nothing with the modules, and the modules share nothing with each other. The only
things that cross the line are the module kit, with the types library below it, and DOM events.
Every module brings its own framework in its own version. That is what lets a Vue cockpit load a
React form and an Angular case page, and a module upgrade its framework without asking anybody.

It costs little, because every entry is loaded once, and the entries of one module share their
framework chunk, see [Self-contained ES modules](#self-contained-es-modules) and
[What loading costs](#what-loading-costs).

Shadow DOM isolates the DOM and the styles of a web component, not its JavaScript. Every module
runs in the same `window`, with the same `customElements` registry and the same global objects. So
a module does not put anything on `window` that another part reads, registers no global CSS, and
loads no polyfill that changes a global API.

The most common of these polyfills is `zone.js`, and so Angular modules run zoneless, with
`provideZonelessChangeDetection()`. `zone.js` patches timers, promises and events of the whole
page. Two Angular modules with `zone.js` patch them twice, and the second copy fails or slows every
part down. An Angular cockpit runs zoneless as well. Zoneless change detection is stable since
Angular 20.2. A form that relies on `zone.js` to notice changes uses signals instead, or calls
`markForCheck()`.

CSS frameworks are the hard part. A global stylesheet of the cockpit changes the look of a form, and
the stylesheet of a form changes the look of the cockpit. That is why every web component of a
module renders into a shadow DOM.

## What loading costs

The ways of loading in this description were measured with a neutral host, not with a cockpit.
The host was the host of the module kit, `@vanillabp/bc-module-host` 0.1.0-snapshot.4, in headless
Chromium 159. The workflow modules were the three cockpit frontend blueprints of VanillaBP, in
React 19.3, Vue 3.5 and Angular 22.2, each with two forms of a user task, built as the blueprints
build them: esbuild for React and Vue, the Angular CLI for Angular. nginx served the files with the
headers of the blueprints' nginx image, once without compression, as that image does, and once with
gzip. Every step ran ten times. The numbers below are medians, rounded. Nearly every run lay
within ten percent of the median. The runs used a development container with shared CPUs, so the
absolute times are rough. The differences between the frameworks and between the ways are what
counts.

"Local" means no network delay: the time is the work of the browser alone. "Slow line" means a
network emulated in the browser with 10 Mbit/s and a round trip of 40 ms. Every time ends when the
form shows the data it asked its own backend for, so it holds that request as well.

How the framework of the cockpit changes these numbers is still open. That is the measurement in
real cockpits, a Vue cockpit with a React and an Angular module and so on.

The first form of a module, opened directly with an empty cache:

| Module  | Transferred, without / with gzip | Local | Slow line, without / with gzip | JS heap after it |
|---------|----------------------------------|-------|--------------------------------|------------------|
| React   | 246 KB / 79 KB                   | 40 ms | 520 ms / 370 ms                | 0.9 MB           |
| Vue     | 90 KB / 37 KB                    | 25 ms | 370 ms / 320 ms                | 0.65 MB          |
| Angular | 149 KB / 51 KB                   | 30 ms | 425 ms / 345 ms                | 1 MB             |

It loads five files: the manifest, the data entry `UserTaskForm`, the small chunk the entries
share, the visual entry and the framework chunk. Nearly all the bytes are the framework chunk.

The ways a part comes into the page, on the slow line with gzip, local in brackets:

|              Way               |                      What is fetched                       |    React    |     Vue     |   Angular   |
|--------------------------------|------------------------------------------------------------|-------------|-------------|-------------|
| the columns of a list          | manifest, data entry and small chunk, 2.5 KB, no framework | 135 ms (4)  | 135 ms (4)  | 135 ms (4)  |
| the first form, after the list | data entry, visual entry, framework chunk                  | 290 ms (37) | 230 ms (21) | 250 ms (27) |
| a form of another kind         | its visual entry, about 1 KB                               | 95 ms (7)   | 95 ms (7)   | 95 ms (7)   |
| a form of the same kind again  | nothing                                                    | 50 ms (6)   | 50 ms (5)   | 50 ms (6)   |
| after a reload of the page     | the manifest, answered with 304; the rest from the cache   | 130 ms (39) | 115 ms (22) | 120 ms (28) |
| a new build, without a reload  | see below                                                  | 360 ms (33) | 225 ms (10) | 230 ms (10) |

What the numbers show:

- The framework is the cost, and a module pays it once. A second form of the same module fetches
  about 1 KB. The same form again fetches nothing, and the 50 ms on the slow line are the request
  of the form to its own backend.
- Data entries are cheap. The columns of a module cost 2.5 KB, three short requests and 60 KB of
  memory, and no framework. That is why a data entry carries no framework, see
  [Data entries carry no framework](#data-entries-carry-no-framework).
- Without network delay, a reload is hardly faster than the first load, because the browser spends
  the time running the framework and drawing, not fetching. On a slow line the HTTP cache saves
  most of the time: one request of 0.2 KB for the manifest, every other file from the cache.
- Compression matters on a slow line. gzip sends a third of the bytes, and the first form shows
  about 15 to 30 percent sooner. nginx does not compress by default, and neither does the image of
  the blueprints.
- Round trips add up. The first form waits for about six requests one after the other: the
  manifest, the data entry, its chunk, the visual entry, the framework chunk and the request of the
  form. On a line with 40 ms round trip, that is a quarter of a second before the size counts.
- Every module framework takes its own memory: 0.65 to 1 MB for the first form, 0.1 to 0.2 MB for
  each further form. A page with forms of a React, a Vue and an Angular module holds the sum.

A new build without a reload of the page. The host of the module kit in this version reads the
manifest only once, so it shows the old build until the page is reloaded: no request, the old names
of the web components. The loader of this description reads the manifest again, see
[A new build of a module](#a-new-build-of-a-module). The measurement renewed the host the way such
a loader drops everything it holds, and then:

- Vue and Angular fetched the manifest, the two entries and the small chunk, 3 KB with gzip. The
  framework chunk kept its file name and came from the cache.
- React fetched everything again, 79 KB with gzip. Its framework chunk imports helper code of
  esbuild from the small chunk, and the small chunk also holds the build id. So every build gives
  the framework chunk a new file name as well, although React did not change.
- The old build stays in memory until the page is reloaded: 0.15 MB for Vue, 0.2 MB for Angular,
  0.65 MB for React.

What to use for which case:

|               Case                |                                                                                               Way                                                                                               |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| every part shown in the cockpit   | ES modules through `bc-manifest.json` and `import()`, as described here; it is the only way that loads into the cockpit's page                                                                  |
| columns, header, pages            | data entries without a framework; the measurement backs the rule                                                                                                                                |
| the page of a user task or a case | load it when it opens, without preloading; with gzip the first form shows within 0.4 s on the slow line, every further form within 0.1 s                                                        |
| a page of the module's own        | `EXTERNAL`; the cockpit loads nothing, so there is nothing to measure in the cockpit                                                                                                            |
| the web server of a module        | compress JavaScript and JSON, send an `ETag` so that a check of the manifest is a 304, and cache the files with a hash in their name for good; the proxy of the cockpit passes these headers on |
| the build of a module             | build twice without a change and compare the file names: only the files that hold the build id may get new names, never the framework chunk                                                     |

Preloading would save at most the first load of a framework, once per module and page. That is a
few hundred milliseconds on a slow line, so the rule of [Ask, load, draw](#ask-load-draw) stays: no
preloading until a screen really needs it.

## A visual part is a web component

The module defines its web components itself, with `customElements.define`, when its visual entry
is evaluated. When the function of a data entry returns a name, the cockpit checks only that it
follows the rule of [The names of the web components](#the-names-of-the-web-components). Then it
loads the visual entry and waits for the name with `customElements.whenDefined`, see
[Ask, load, draw](#ask-load-draw).

What a web component does:

- It attaches an open shadow root and renders everything into it, its styles included.
- It reads its input from properties the host sets. On the page of a user task, `userTask`; on the
  page of a case, `workflow`; for every web component, `language` and `currentUser`. The values are
  the plain data of the GUI API, see [types.md](types.md#what-a-part-gets).
- It renders again when the host sets a property again. The host does that when the update stream
  says the user task or the case changed.
- It asks the cockpit for anything through the [event protocol](#the-event-protocol).
- It frees its framework's resources in `disconnectedCallback`.

The theme reaches the shadow DOM as CSS custom properties. The host sets them on its root, for
example `--bc-color-primary` or `--bc-font-family`, and they are inherited into every shadow tree
(`Q-THEME-TOKENS`). Their names are part of the types library, their values come from the theme
library, see [project.md](project.md#the-theme-library).

The module kit offers a helper, `defineBcElement`, that builds and registers such a web
component from three plain functions of the module: `mount`, `update` and `unmount`, see
[types.md](types.md#the-event-protocol). It is the same helper as in the VanillaBP kit. A React
module writes `mount = (root, props) => createRoot(root).render(<Form {...props} />)`. A Vue module
makes one small application per web component with `createApp`. It hands the properties to the
application with `provide` as one `shallowRef`, and its components read them with a small helper,
for example `useBcElement()`. `update` only sets the new properties on that ref, and Vue draws
again. An Angular module uses `createApplication` once per module and `createComponent` with
`hostElement` per web component. So no module has to write the shadow root and the protocol by
hand. The example modules of the VanillaBP kit show all three. Vue and Angular have their own
traps, see [Vue in a shadow root](rough-edges.md#vue-in-a-shadow-root) and
[Angular in a shadow root](rough-edges.md#angular-in-a-shadow-root).

### A BPMN diagram is drawn with bpmn-js

A part which shows a process, for example the page of a case with the diagram and the steps done so
far, draws the BPMN file with [bpmn-js](https://bpmn.io/toolkit/bpmn-js/). Never draw it from the
`BPMNDI` part of the file yourself: bpmn-js draws every element as the modeller shows it, and a
drawing of your own is worse and gets old with every new element in a model.

- Take the `Viewer`, or the `NavigatedViewer` when the user should move and zoom the diagram. A
  part never needs the `Modeler`.
- Show the current step and the steps done with markers (`canvas.addMarker`) and a few CSS rules,
  and numbers or badges with overlays (`overlays.add`). The ids are the ids of the BPMN elements,
  for example the task definition of a user task.
- The styles of bpmn-js, `bpmn-js/dist/assets/diagram-js.css` and `bpmn-js.css`, go into the shadow
  root together with the module's style. Selectors of the page do not reach the diagram, see
  [What a web component does](#a-visual-part-is-a-web-component).
- Import bpmn-js only in the visual entry of the part which shows the diagram, or load it there with
  `import()`. Then the bundler gives it a chunk of its own, and the cockpit loads it only when the
  diagram is about to be shown. No data entry and no other part imports it.
- Take the package `bpmn-js` from npmjs. If the npm registry of the organisation (`Q-NPM-SCOPE`)
  does not mirror it, point only the scope of the organisation at that registry in `.npmrc` and
  leave the rest at `https://registry.npmjs.org`. Do not draw the diagram yourself because a
  registry lacks the package.
- Keep the bpmn.io logo in the corner of the diagram. bpmn-js is under the bpmn.io license, which
  allows its use in a product but forbids removing or hiding that logo.

A picture of a diagram which a build makes, for example for a README or a document, comes from the
same renderer: [`bpmn-to-image`](https://github.com/bpmn-io/bpmn-to-image) draws a BPMN file into a
PNG or an SVG.

## The names of the web components

All modules define their web components in the one `customElements` registry of the page. Two
modules must never use the same name, and a new build of a module must not use the names of the
old one. So every name has three parts:

```text
taxi-ride-mvaro4w0-taxi-ride--assess-risk--form
└ prefix ┘└ build ┘└ short name ────────────┘
```

- The prefix comes from the module id. It keeps the modules apart.
- The build id is new for every build. It keeps the builds of one module apart, see
  [A new build of a module](#a-new-build-of-a-module).
- The short name holds the BPMN ids of the workflow or the user task and the name of the part. So
  it is unique within the module, see [The short name](#the-short-name).

The examples in this description leave out the build id, so they stay short.

### From the module id to the prefix

The HTML standard allows a custom element name only when it starts with a lower case letter `a` to
`z`, contains a hyphen, has no upper case letter, and is none of the reserved names such as
`font-face` or `annotation-xml`. A module id can be any string, because the BPMS API sets no rule
for it. So one function turns the id into the prefix, `elementPrefix(workflowModuleId)` of the
module kit, and the module host uses the same function:

1. Put a hyphen between a lower case letter or a digit and an upper case letter that follows it.
2. Make every letter lower case.
3. Replace each run of other characters than `a` to `z`, `0` to `9` and `-` with one hyphen, and
   remove hyphens at the start and at the end.
4. If the result is empty or does not start with a letter, put `wm-` in front of it.

|     Module id     |      Prefix       |
|-------------------|-------------------|
| `taxi-ride`       | `taxi-ride`       |
| `TaxiRide`        | `taxi-ride`       |
| `loan_approval.2` | `loan-approval-2` |
| `4711`            | `wm-4711`         |

The whole name is the prefix, a hyphen and the rest, so it always contains a hyphen.

### The short name

The short name is made of segments. A segment has lower case letters and digits, with one hyphen
between two words. It never contains `--` and never starts or ends with a hyphen. Two hyphens, `--`,
go between two segments. There are three kinds of short names:

|    Kind     |         Short name          |            Example             |
|-------------|-----------------------------|--------------------------------|
| a workflow  | `<process>--<part>`         | `taxi-ride--page`              |
| a user task | `<process>--<task>--<part>` | `taxi-ride--assess-risk--form` |
| custom      | `<short name>`, no `--`     | `price-chart`                  |

- `<process>` is the BPMN process id, `<task>` the task definition of the user task. They become
  segments by the rules of the prefix above, with two changes. In step 3 a run of hyphens becomes
  one hyphen as well. And step 4 does not apply, because the segment never starts the name: an id
  that has no letter and no digit is an error. `TaxiRide` gives `taxi-ride`, `assessRisk` gives
  `assess-risk`.
- `<part>` says which web component of the workflow or the user task it is, for example `form`,
  `page`, `information`, `actions` or `list-item`. It must be a segment already. It is not changed.
- A custom short name is for anything that is not the part of one workflow or one user task, for
  example a button of the header or a page of the module. It must be a segment already as well.

So a short name of a workflow has one `--`, one of a user task two, and a custom one none. Split at
`--`, the short name gives back the process, the task and the part. That is why two different
processes, tasks or parts never get the same name. A single hyphen would not be enough:
`assessRisk` with the part `form` and `assess` with the part `risk-form` would both give
`assess-risk-form`. `--` is allowed in the name of a custom element.

Two BPMN ids that differ only in case or in characters that become a hyphen give the same segment.
`assess_risk` and `assessRisk` both give `assess-risk`. If a module has both in one process, the
second `defineBcElement` throws, because its name is defined already. One of the two then needs
another id.

### What the module's build does

The module kit has a helper for the names, `createElementNames(workflowModuleId, buildId)`. It
returns an object with three functions, `workflow`, `userTask` and `custom`, which build the whole
names. It throws a `TypeError` for an id that gives an empty segment, and for a part or a custom
short name that is not a segment. The module puts it into one small file of plain JavaScript, which
its data entries and its visual entries import:

```js
// src/element-names.js
import { createElementNames } from '<scope>/bc-module-kit';
import { WORKFLOW_MODULE_ID, BUILD_ID } from './bc-build.js';

export const names = createElementNames(WORKFLOW_MODULE_ID, BUILD_ID);
// names.userTask('TaxiRide', 'assessRisk', 'form') === 'taxi-ride-mvaro4w0-taxi-ride--assess-risk--form'
// names.workflow('TaxiRide', 'page')               === 'taxi-ride-mvaro4w0-taxi-ride--page'
// names.custom('price-chart')                       === 'taxi-ride-mvaro4w0-price-chart'
```

The visual entry registers its web components with the same functions, for example
`defineBcElement(names.userTask('TaxiRide', 'assessRisk', 'form'), { mount, update, unmount })`.
The dev shell gets its components by the same names, through a map of its own, see
[dev-shell.md](dev-shell.md#the-entry-point). A visual entry never imports that map. It defines its
own components, so the bundler gives each visual entry its own chunk.

The build script writes `bc-build.js` before it starts the bundler:

- `WORKFLOW_MODULE_ID` is the id the module registers with at the cockpit. It comes from the
  module's own configuration, for example a field in its `package.json`. If it differs from the id
  of the registration, the cockpit refuses every name of the module.
- `BUILD_ID` is the time of the build in milliseconds, written in base 36:
  `Date.now().toString(36)`. For the build of the manifest above, `2026-10-16T09:30:00Z`, that is
  `mvaro4w0`. It has only lower case letters and digits.

After the bundler, the same script writes `bc-manifest.json` with the same time as
`buildTimestamp`, so the manifest and the names speak of one build.

### The cockpit checks every name

The build of a module cannot be forced to do this. A module may write its names by hand instead of
using `createElementNames`. So the cockpit checks every name it is
going to draw, after the ask step and before it loads anything. The same check runs for the name
of a dialog that `cockpit.openDialog` asks for. A name is refused when

- it is not a valid name of a custom element, or it is a reserved one,
- it does not start with the prefix of its module followed by a hyphen,
- or it starts with the prefix of another module of the registry that is longer than the module's
  own prefix. A module `taxi` may not use the name `taxi-ride-confirm`, when there is a module
  `taxi-ride`.

A refused name is not loaded and not drawn. The cockpit shows a note that names the module, the
name and the prefix the name has to start with, and writes the same to the console. It is a bug of
the module, so the note is for the author of the module as much as for the user.

The check does not ask for the build id, and it does not look at the short name. A name without
the build id, like `taxi-ride-taxi-ride--assess-risk--form`, is accepted, and the module then needs a reload of the page after a new build, see
[A module whose names carry no build](#a-module-whose-names-carry-no-build). The blueprints and the
example modules of the VanillaBP kit do not use names of this kind: they build their names with
`createElementNames`.

Two different module ids can have the same prefix, for example `TaxiRide` and `taxi-ride`. Their
names still do not clash as long as both carry a build id. The cockpit writes a warning to the
console when two modules of the registry have the same prefix.

## The page of a user task or a case

The module does not export a component for these pages. Its data entries `UserTaskForm` and
`WorkflowPage` export a function each, see
[Data entries carry no framework](#data-entries-carry-no-framework):

```ts
userTaskForm: (userTask: UserTask) => BcPageParts | undefined;
workflowPage: (workflow: Workflow) => BcPageParts | undefined;

interface BcPageParts {
  entry: string;                // the visual entry that defines the web components
  element: string | PageAreas;  // one web component, or one per area
}
type PageAreas = { [area: string]: string };   // area -> name of a web component
```

It has the same shape as the `{ element, entry }` of the header and the pages, see
[The other extension points](#the-other-extension-points). Only `element` may also be an object.
The result has three forms:

- `undefined`: the function does not know this user task or this case. It is not the module's.
- `element` is a string: the name of one web component, for the area `all`. It is the all-in-one
  form. The cockpit gives it the whole area, draws no column and sets its property `area` to `all`.
- `element` is an object: per area, the name of the web component that fills it. It is the
  laid-out form. It may name a web component for `all` as well.

```ts
{ entry: 'ConfirmRideForm', element: 'taxi-ride-taxi-ride--confirm-ride--page' }

{ entry: 'AssessRiskForm',
  element: { all: 'taxi-ride-taxi-ride--assess-risk--page',
             body: 'taxi-ride-taxi-ride--assess-risk--form',
             information: 'taxi-ride-taxi-ride--assess-risk--information',
             actions: 'taxi-ride-taxi-ride--assess-risk--actions' } }
```

The area `all` is part of the minimum, and every cockpit knows it. Everything else about areas is
the organisation's choice. So the cockpit picks what to draw like this:

1. It takes the areas of the object that it knows itself, and ignores the others without an error.
   If there is at least one, it draws these as the laid-out form.
2. Otherwise it draws the web component of `all`, as the all-in-one form. A cockpit without areas
   of its own always ends up here.
3. If the object has neither, the module has no form for this cockpit. The page says so, the same
   way as for `undefined`.

So a module that names `all` and areas runs in a cockpit with areas and in one without. The example
modules of the VanillaBP kit do exactly that. The cockpit draws either the areas or `all`, never both.

All web components of one page come from the one visual entry the result names. The cockpit loads
it once and waits for every web component it is going to draw, see [Ask, load, draw](#ask-load-draw).

Which areas exist is a convention, not a law (`Q-TASK-PAGE-AREAS` for the page of a user task,
`Q-WORKFLOW-PAGE-AREAS` for the page of a case). An organisation may also choose no areas at all,
for one page or for both; then its cockpit shows only `all` there. The default convention, the same
for both pages:

|     Area      |                              Where the cockpit puts it                              |
|---------------|-------------------------------------------------------------------------------------|
| `body`        | the main area; a long form scrolls in it                                            |
| `information` | the top of a column on the right, for example the case number and the customer      |
| `actions`     | the bottom of that column, for example "Accept" and "Reject"; it never scrolls away |

An object with `body` only is still the laid-out form: `body` sits in the main area, and the column
stays empty. A module that wants the whole page returns a string. So each module chooses between
the laid-out form and the all-in-one form, and the cockpit decides where the parts sit. This keeps
the usability the same across modules when several teams build them.

The areas of one page often have to talk to each other. For example, "Complete" in `actions` only
becomes active when the form in `body` has its required fields. For this `BcUserTask` and
`BcWorkflow` have `sendTo(target, message)` and `receiveMessage(handler)`, see
[types.md](types.md#bcusertask-and-bcworkflow). The host carries the message from the web component
of one area to the web component of the area named in `target`, as a DOM event. Nothing else is
shared between the areas. An all-in-one form has one web component and needs no messages.

## The other extension points

Every further extension point follows the same pattern as the page of a user task: the module
names a data entry in its manifest, and the data entry exports a function. The function says what
there is, and a visual entry defines the web components. So the cockpit can build the menu and the
routes for every module without loading a single framework. Proposals for the usual ones:

|      Entry       |                              What the module exports                              |  Kind  | Where the cockpit uses it |
|------------------|-----------------------------------------------------------------------------------|--------|---------------------------|
| `Header`         | `header: (currentUser) => Array<{ element, entry }> \| undefined`                 | data   | the application header    |
| `Pages`          | `pages: (currentUser) => Array<{ id, title, element, entry, menu }> \| undefined` | data   | the router and the menu   |
| `HeaderElements` | nothing; evaluating it defines the web components of the header buttons           | visual | the application header    |
| `PageElements`   | nothing; evaluating it defines the web components of the pages                    | visual | a page of a module        |

`element` is the name of a web component, and `entry` names the visual entry of the same manifest
that defines it. The cockpit runs the three steps of [Ask, load, draw](#ask-load-draw). It loads the
visual entry only when the web component is about to be shown: the header elements when the
header draws, the elements of a page when its route opens. `HeaderElements` and
`PageElements` are only examples. A module may use other names for its visual entries; only
`Header` and `Pages` are fixed.

A page of a module has the route `/<workflowModuleId>/<id>`, always. The router of the cockpit
adds a route for every page of every module in the registry. `title` is a `Translatable`, and
`menu: true` adds a menu entry. The cockpit's own routes come
first, so a module id like `tasks` cannot hide a screen of the cockpit.

A module may open a modal dialog, for example a header button "Start" that shows a start form. The
button asks the cockpit with the action `cockpit.openDialog` and names the web component of the
dialog and its visual entry, the same `{ element, entry }` as above. The button has already asked,
so the cockpit starts with the second step: it loads the entry, waits for the web component and
draws it in a modal layer above everything. Often the entry is the one of the button and is loaded
already. The dialog closes itself with
`cockpit.closeDialog` and a result, and the promise of `cockpit.openDialog` resolves with that
result, see [Actions](#actions). The cockpit answers `cockpit.closeDialog` first and removes the
dialog afterwards, in a later task. If it removed the web component first, the web component would
reject its own request with `DISCONNECTED`.

Dialogs are part of the minimum. The event protocol of the VanillaBP kit has `cockpit.openDialog` and
`cockpit.closeDialog`, and a form on the page of a user task may open a dialog too. So every
cockpit offers them, also one without further extension points.

## The event protocol

A part of a module gets functions, for example `claim()` on a user task. The part calls them like
ordinary functions that return a promise. Below that, a wrapper on the module's side turns the call
into a DOM event on the web component. The cockpit runs it and answers with an event on the same
element. The wrapper then resolves or rejects the caller's promise. The helper of the module kit is
that wrapper.

### Request

The wrapper dispatches on the web component itself:

```ts
element.dispatchEvent(new CustomEvent('bc-request', {
  bubbles: false,
  composed: false,
  detail: {
    requestId: crypto.randomUUID(),   // the correlation id
    action: 'userTask.claim',
    args: { userTaskId: '4711' },
  },
}));
```

`crypto.randomUUID()` exists only in a secure context: https, or plain http on `localhost`. On a
page served over plain http from another host the wrapper takes a counter with a random part
instead. The id only has to be unique on its element. The host treats it as an opaque string.

### Answer

The host listens for `bc-request` on every web component it puts into the page. It answers every
request exactly once, on the same element:

```ts
element.dispatchEvent(new CustomEvent('bc-response', {
  detail: { requestId, ok: true, result },
}));
// or
element.dispatchEvent(new CustomEvent('bc-response', {
  detail: { requestId, ok: false, error: { code: 'REFUSED', status: 409, message: '...' } },
}));
```

### Messages between areas

Sending and receiving are both actions, so the host knows who listens.

Sending: `sendTo(target, message)` is the request `page.sendTo` with `target` and `message`.

Receiving: `receiveMessage(handler)` sends the request `page.subscribe`. The host answers with a
`subscriptionId`. From then on the host dispatches every message for this area on the web component
of the area, with this id:

```ts
targetElement.dispatchEvent(new CustomEvent('bc-message', {
  detail: { subscriptionId, from: 'body', message },
}));
```

The wrapper calls the handler for every `bc-message` with its `subscriptionId`. `receiveMessage`
returns a subscription with `unsubscribe()`. That sends `page.unsubscribe` with the id, and the
host stops delivering. When the element leaves the page, the host drops its subscriptions by
itself.

The areas of a page load at the same time, so a message may come before its target has
subscribed. The host keeps such messages, in order, and delivers them once the area subscribes. It
drops them when the page is left. A `target` without a web component on this page is answered with
`NO_TARGET`.

Both sides take care that no message gets lost around `page.subscribe`. The host delivers the kept
messages in a later task, after its answer with the `subscriptionId`, for example with
`setTimeout`. The wrapper still keeps every `bc-message` that comes before it knows its
`subscriptionId`. Once the answer is there, it hands the kept messages with that id to the handler,
in order, and drops the others. `ready` of the subscription resolves at that moment.

### Notifications

`showNotification` is the request `cockpit.showNotification`. The cockpit shows a toast with the
`text` in the look of the `level`: `info`, `success`, `warning` or `error`. With `sticky: true` the
toast stays until the user clicks it away. Otherwise it goes away by itself after a few seconds.
The text comes in the user's language; the part knows it from the property `language`. That is
the default language, or the language the user chose (`Q-LANGUAGE-DEFAULT`, `Q-LANGUAGE-SWITCH`,
see [interview.md](interview.md#language)).

### Error codes

|       Code       | Who sets it |                            Meaning                            |
|------------------|-------------|---------------------------------------------------------------|
| `UNKNOWN_ACTION` | host        | this cockpit does not know the action; a module may fall back |
| `REFUSED`        | host        | the server refused it; `status` carries the HTTP status       |
| `FAILED`         | host        | the request did not get through, for example a network error  |
| `NO_TARGET`      | host        | `sendTo` named an area that has no web component on this page |
| `TIMEOUT`        | wrapper     | no answer within the timeout                                  |
| `DISCONNECTED`   | wrapper     | the element was removed from the page before the answer came  |

### Timeout

The wrapper waits 30 seconds by default, then rejects with `TIMEOUT`. An answer that comes later is
ignored. `cockpit.openDialog` has no timeout, because it waits for the user. A caller may pass its
own timeout in milliseconds, and `0` means no timeout at all.

When the element is disconnected, the wrapper rejects every open request of this element with
`DISCONNECTED`. The wrapper keeps the open requests per element for this. The function for it is
`rejectOpenRequests(element)`, and `defineBcElement` calls it in `disconnectedCallback`. So a
cockpit that removes a web component, for example the dialog after `cockpit.closeDialog`, answers
its last request before it removes the element.

### Actions

The minimum set, matching the functions in [types.md](types.md#bcusertask-and-bcworkflow):

|            Action             |                                  `args`                                   |                    `result`                    |
|-------------------------------|---------------------------------------------------------------------------|------------------------------------------------|
| `userTask.open`               | `userTaskId`                                                              | nothing                                        |
| `userTask.navigateToWorkflow` | `userTaskId`                                                              | nothing                                        |
| `userTask.claim`              | `userTaskId`                                                              | nothing                                        |
| `userTask.unclaim`            | `userTaskId`                                                              | nothing                                        |
| `userTask.assign`             | `userTaskId`, `userId`                                                    | nothing                                        |
| `userTask.unassign`           | `userTaskId`, `userId`                                                    | nothing                                        |
| `workflow.navigateToWorkflow` | `workflowId`                                                              | nothing                                        |
| `workflow.getUserTasks`       | `workflowId`, `activeOnly`, `limitListAccordingToCurrentUsersPermissions` | an array of user tasks                         |
| `page.sendTo`                 | `target`, `message`                                                       | nothing                                        |
| `page.subscribe`              | none; the area is the one of the element                                  | `subscriptionId`                               |
| `page.unsubscribe`            | `subscriptionId`                                                          | nothing                                        |
| `cockpit.showNotification`    | `level`, `text`, `sticky`                                                 | nothing                                        |
| `cockpit.openDialog`          | `element`, `entry`, `data`                                                | what the dialog closed with                    |
| `cockpit.closeDialog`         | `result`                                                                  | nothing; answered before the dialog is removed |

The four claim and assign actions resolve once the server has accepted the change. The server
answers them without the changed user task. The new state reaches the part when the host sets the
`userTask` property again, after the update stream or after the host read the task again.

An organisation's own extension points add their own actions under their own prefix. A new action
is additive. An old cockpit answers `UNKNOWN_ACTION`, and the module decides what to do.

## Cells drawn by a workflow module

This is an open question. It is not required and not generated until a solution exists. So the
interview does not ask `Q-CELL-RENDERING` yet, and a wish for it gets no `CUST-` item.

The wish: a module draws single cells of a list itself, for example a traffic light for a risk
rating, buttons that complete a user task in place, or details loaded on mouse-over. It is a good
feature.

The problem: a list has many cells. If each cell is a web component that starts the module's
framework inside its own shadow root, the framework is instantiated hundreds of times in the
browser's memory, and the list gets slow.

Directions that might work, none of them proven:

- One framework runtime per module and many mounts. The module exports `mount`, `update` and
  `unmount` for a cell as plain functions. The host calls them for each visible cell. The framework
  is loaded once, and each cell is a small root of it, not a new application.
- The window of loaded rows keeps the number of cells small anyway, because only the rows around
  the visible area exist at all, see
  [behaviour.md](behaviour.md#paging-and-the-window-of-loaded-rows).

A probe has to decide this. It would compare three ways of loading a cell across frameworks: the way
of this description, which shares nothing and loads self-contained ES modules with `import()`;
Module Federation 2.0 with shared dependencies; and native federation. It would mix the frameworks
crosswise, for example a Vue page with cells from a React and an Angular module. It would measure
the memory, how fast cells are drawn, how fast they react to the user, how many cells a list can
hold, and how long loading takes. Its result would decide whether cells may relax the rule [Nothing
is shared](#nothing-is-shared). Every other part keeps the rule.

Until then, cells are drawn by the cockpit from the column types, see
[behaviour.md](behaviour.md#the-columns-a-workflow-module-brings).
