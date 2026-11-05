# The generated project

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The skill fills two repositories, and each one needs only one build tool.

- The UI repository is one npm repository with npm workspaces. Every npm package of the cockpit is
  a workspace of it: the types library, the module kit, the module host, the theme, the user
  interface and the dev shells. It also builds the UI jar and an nginx image. It has no `pom.xml`,
  and its build needs no Java.
- The backend repository is a Maven project. When the skill creates it (`Q-BACKEND`), it has a
  parent `pom.xml` and two modules: `types`, which builds the types jar for the workflow modules,
  and `application`, the cockpit application on the `business-cockpit` library. The application
  pulls the UI jar as an ordinary dependency. The repository has no `package.json`. See
  [backend.md](backend.md).

When the organisation keeps a backend of its own and the skill creates none, the types jar comes
from a small Maven project of its own instead, the types module, see
[The types jar](#the-types-jar).

Many companies run build agents that have either npm or Maven, not both. Many also treat a build
that downloads a second tool chain later, like `frontend-maven-plugin` does, as a security risk.
Keeping npm and Maven apart avoids both problems, and a Maven release of the backend stays an
ordinary Maven release.

1. [The UI repository](#the-ui-repository)
2. [The npm workspaces](#the-npm-workspaces)
3. [The user interface bundle](#the-user-interface-bundle)
4. [The UI jar](#the-ui-jar)
5. [Uploading the UI jar into the Maven repository](#uploading-the-ui-jar-into-the-maven-repository)
6. [The nginx image](#the-nginx-image)
7. [The backend repository](#the-backend-repository)
8. [The types jar](#the-types-jar)
9. [What still needs Java](#what-still-needs-java)
10. [The types library](#the-types-library)
11. [The module kit](#the-module-kit)
12. [The module host](#the-module-host)
13. [The theme library](#the-theme-library)
14. [The dev shell libraries](#the-dev-shell-libraries)
15. [What the project keeps beside the code](#what-the-project-keeps-beside-the-code)

## The UI repository

```
package.json                  the workspace root: the list of workspaces, the scripts
                              that build everything, no dependencies of the code
package-lock.json             the one lock file of all workspaces
renovate.json                 groups the updates of a framework across the workspaces
INTERVIEW.md                  the answers of the interview
CHECKLIST.md                  the checklist, see ../SKILL.md
packages/
  bc-types/                   <scope>/bc-types: the data of the GUI API and the constants,
                              no dependencies
  bc-module-kit/              <scope>/bc-module-kit: what a workflow module installs; the
                              runtime helpers, and the types of bc-types again
  bc-module-host/             <scope>/bc-module-host: the registry, the loader and the
                              cockpit's side of the event protocol
  bc-theme/                   <scope>/bc-theme, the colours, fonts and logo of Q-THEME
  ui/                         the cockpit user interface
  bc-dev-shell-<framework>/   <scope>/bc-dev-shell-<framework>, one per framework of the modules
scripts/
  fetch-openapi.mjs           loads the OpenAPI files from the jars of the cockpit
  check-texts.mjs             fails the build when a language file misses a key
  set-version.mjs             writes one version into every workspace
jars/
  build-jars.mjs              builds the UI jar
  publish-jars.mjs            uploads it into the Maven repository
docker/
  Dockerfile                  the nginx image for a separate deployment
  nginx.conf.template
e2e/                          the end-to-end tests, one per checklist item, and the
                              measurement of the lists, see behaviour.md
```

The packages are separate npm packages, each a workspace of the same repository. A dev shell needs
its framework as a peer dependency, and the module kit must not have one: a workflow module in Vue
must not be forced to install React. One package with subpath exports, such as `<scope>/bc/kit`
and `<scope>/bc/dev-shell-react`, would also work, because tree shaking drops the parts a module
does not use. But its peer dependencies would still name every framework, so separate packages are
cleaner.

### Why the cockpit has its own module kit and host

VanillaBP publishes a module kit and a host of its own, `@vanillabp/bc-module-kit` and
`@vanillabp/bc-module-host`, from the repository
[`vanillabp/business-cockpit-module-kit`](https://github.com/vanillabp/business-cockpit-module-kit/tree/main/packages).
That repository exists so that the Business Cockpit blueprints build without a generated cockpit.
It holds only the minimum every cockpit knows.

Every generated user interface gets its own copy of both packages, `<scope>/bc-module-kit` and
`<scope>/bc-module-host`, in `packages/`. Parts of them come from the interview: the extension
points beyond the minimum, such as `Header` and `Pages` (`Q-EXTENSION-POINTS`), the areas of a page
(`Q-TASK-PAGE-AREAS`, `Q-WORKFLOW-PAGE-AREAS`), the names of the functions those entries export,
and the `ui-uri-type` values (`Q-UI-URI-TYPE`). A module of the organisation installs the
cockpit's own kit, never the VanillaBP one.

The copy keeps the whole minimum of the VanillaBP kit, with the same names and the same behaviour:
`USER_TASK_LIST`, `WORKFLOW_LIST`, `USER_TASK_FORM`, `WORKFLOW_PAGE`, `DATA_ENTRIES`,
`EXPORT_NAMES` and `AREA_ALL`, the manifest, the event protocol, `defineBcElement` and
`bc-check-entries`. It only adds. So a blueprint built against the VanillaBP kit still loads in the
generated cockpit, see [types.md](types.md#the-vanillabp-kit-is-the-minimum). The copy does not
depend on the VanillaBP packages and does not bundle them. It is written from this description,
like the rest of the user interface.

### Which package holds what

There is one place for each name, so no name has two sources:

|         Package          |                                                                                                   What it holds                                                                                                   |   Depends on    |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|
| `<scope>/bc-types`       | the data of the GUI API, generated from the OpenAPI files, the other types, and every constant: entries, export names, areas, `ui-uri-type` values, event names, action names, error codes, names of theme tokens | nothing         |
| `<scope>/bc-module-kit`  | the runtime helpers a module needs: `defineBcElement`, the module's side of the event protocol, the manifest functions, `bc-check-entries`; it exports the types and constants of `bc-types` again                | `bc-types`      |
| `<scope>/bc-module-host` | the registry of modules, the central loader, ask, load and draw, and the cockpit's side of the event protocol                                                                                                     | `bc-module-kit` |

A workflow module depends on the module kit only. It gets every type and constant from there and
never names `bc-types` itself. The types library is the base the kit is built on, and the Maven
types module reads it, see [The types jar](#the-types-jar).

A question that is still open: does the kit ship only `.d.ts` files to the workflow modules? The
default is no. The kit has runtime code that every module needs: `defineBcElement`, the event
protocol and `bc-check-entries`. Only the types reach the module as `.d.ts` files, because types do
not exist at runtime.

The GUI API client is generated at build time into `packages/ui` and into each dev shell, see
[gui-api.md](gui-api.md#generating-the-client). It is not committed. `scripts/fetch-openapi.mjs`
loads the two OpenAPI files first. It reads the jars of the cockpit from the Maven repository of
the interview (`Q-COCKPIT-VERSION`, `Q-COCKPIT-REPOSITORY`). By default that is Maven Central,
which needs no token. Only a company repository with a login needs one, from the environment
variable the interview names.

## The npm workspaces

The UI repository uses npm workspaces. npm has them since version 7, so no other tool is needed,
neither pnpm nor Lerna. The root `package.json` lists the workspaces:

```json
{
  "name": "business-cockpit-ui-root",
  "version": "1.4.0",
  "private": true,
  "workspaces": [
    "packages/bc-types",
    "packages/bc-module-kit",
    "packages/bc-module-host",
    "packages/bc-theme",
    "packages/ui",
    "packages/bc-dev-shell-*"
  ]
}
```

Every npm package of the repository is a workspace. A new npm package, for example a second theme,
becomes one more entry in this list, not a repository of its own. The Maven projects stay outside,
see [The backend repository](#the-backend-repository) and [The types jar](#the-types-jar).

One `npm ci` at the root installs every workspace. There is one `package-lock.json`, at the root,
and no workspace has a lock file of its own. Each workspace still has its own `package.json` with
its own dependencies, so a dev shell for Vue never pulls React into the user interface.

### How the workspaces depend on each other

A workspace names another workspace as an ordinary dependency, with the exact version of that
workspace. The module kit names the types library, the host names the kit, and the user interface
names the host, the kit and the theme:

```json
"dependencies": {
  "<scope>/bc-module-host": "1.4.0",
  "<scope>/bc-module-kit": "1.4.0",
  "<scope>/bc-theme": "1.4.0"
}
```

|         Workspace          |            Names these workspaces             |
|----------------------------|-----------------------------------------------|
| `bc-types`                 | none                                          |
| `bc-module-kit`            | `bc-types`                                    |
| `bc-module-host`           | `bc-module-kit`                               |
| `bc-theme`                 | none                                          |
| `ui`                       | `bc-module-host`, `bc-module-kit`, `bc-theme` |
| `bc-dev-shell-<framework>` | `bc-module-kit`, `bc-theme`                   |

npm sees that a workspace has this name and version, and links it instead of downloading it. So
every workspace builds against the others of the same commit. npm has no `workspace:` protocol like
pnpm or Yarn. The version must match exactly, or npm downloads the package from the registry. When
a workflow module installs the published kit, npm takes the published `bc-types` of the same
version along with it.

### The order of the build

`npm run build --workspaces` runs the build of each workspace in the order of the list in the root
`package.json`. npm does not sort the workspaces by their dependencies. So the list keeps this
order, and the root script `build` calls it:

1. `packages/bc-types`, because the kit is built on it.
2. `packages/bc-module-kit`, because the host, the user interface and the dev shells import it.
3. `packages/bc-module-host`, because the user interface loads the parts of the modules with it.
4. `packages/bc-theme`, because the user interface and the dev shells take its styles.
5. `packages/ui`, the single-page application.
6. `packages/bc-dev-shell-*`, one per framework.
7. After the workspaces, `npm run build:jars` builds the UI jar from `packages/ui/dist`.

### One version, each package published on its own

All workspaces have the same version, the one in the root `package.json`. `scripts/set-version.mjs`
writes the new version into every `package.json` and into the dependencies between the
workspaces, then runs `npm install` so that the lock file follows. The Git tag is made from that
version.

Each package is published on its own, `npm publish --workspace packages/bc-types` and so on, or all
at once with `npm publish --workspaces`. Publish in the order of the build: the types library, the
module kit, the module host, the theme, then the dev shells. Each one needs the packages before it
in the registry. `packages/ui` and the root have `"private": true`, so npm never publishes them.
The user interface leaves the repository as the UI jar and the nginx image, not as an npm package.

The packages go to the npm registry of the interview (`Q-NPM-SCOPE`), under the organisation's
scope. So they never clash with the VanillaBP packages on npmjs.com.

### Renovate

Renovate handles npm workspaces. It finds the `package.json` of every workspace, and it updates the
one `package-lock.json` at the root. It leaves the dependencies between the workspaces alone, as
long as `updateInternalDeps` stays `false`, its default.

Group the updates of a framework across the workspaces. Otherwise Renovate opens one pull request
for the user interface and another for the dev shell of the same framework, and for a while they
use two versions. With React, for example:

```json
{
  "packageRules": [
    {
      "matchPackageNames": ["react", "react-dom", "@types/react", "@types/react-dom"],
      "groupName": "react"
    }
  ]
}
```

Write one rule like this for every framework of `Q-FRAMEWORK` and `Q-MODULE-FRAMEWORKS`, and one for
the design library of `Q-DESIGN-LIBRARY`.

## The user interface bundle

`packages/ui` builds the single-page application into `packages/ui/dist`. It is built with the
framework and the build tool from the interview. It loads the parts of the modules with the module
host of the repository, which calls `import()` for the files their manifests name, see
[module-loading.md](module-loading.md#self-contained-es-modules).

Two rules for the output come from the backend:

- `index.html` is the shell. It must not be cached, and it is served for every unknown path.
- Every other file lies below one prefix, `/assets/` by default. Where the files have to load
  without a login (`Q-HOSTING` is `in-front`), one rule then opens all of them, see
  [gui-api.md](gui-api.md#protection-and-login). Many build tools put their files into an `assets`
  folder by default. Where yours does not, configure the output path or the base path so that it
  does.

Before the bundle is built, `npm run check:texts` runs `scripts/check-texts.mjs`. It is part of
`prebuild`, so no build skips it. It reads the file of the default language (`Q-LANGUAGE-DEFAULT`)
and every other language file. If a file misses a key of the default language, the script names the
file and the key and ends with an error, so the build fails. The fallback to the default language at
run time stays, see [behaviour.md](behaviour.md#language-and-texts).

The lists keep their rules in the code of the bundle, and a few of them are easy to get wrong:

- `Q-MAX-LIST-ENTRIES` is one named constant in a file of its own, which every list reads.
- A request for a page goes into a stream of events or a queue, never into a reactive value that
  keeps only its latest value.
- A service or store that lives longer than a list has a clean-up step that the list calls when it
  closes, and it ends its old subscriptions before it starts new ones.

[behaviour.md](behaviour.md#paging-and-the-window-of-loaded-rows) says why, and which unit tests
prove it.

## The UI jar

A jar is a zip file with a `META-INF/MANIFEST.MF`. The UI jar holds the built files at the place
where the backend framework serves static files from its class path (`Q-BACKEND-FRAMEWORK`):

| Backend framework |   Place in the jar    | Served at |
|-------------------|-----------------------|-----------|
| Spring Boot       | `static/`             | `/`       |
| Quarkus           | `META-INF/resources/` | `/`       |

For Spring Boot:

```
static/index.html
static/assets/<the bundle>
META-INF/MANIFEST.MF
```

The file `static/assets/main-1234.js` is served at the URL `/assets/main-1234.js`. Spring Boot
also serves `META-INF/resources/` by default, but the runnable `container` jar sets
`spring.web.resources.static-locations` to `classpath:/static/` only, and so does the generated
backend, see [backend.md](backend.md#the-configuration). The shell is the file that
`application.spa-default-file` names, `classpath:/static/index.html` in both. So for Spring Boot,
`static/` is the place that works for every cockpit.

There is no Quarkus variant of the cockpit backend yet. It comes later, and the generator is ready
for it: the UI jar then puts the files below `META-INF/resources/`. A Quarkus native build bakes
the static files into the native image when the image is built. So the UI jar must be a dependency
at that moment. A file added afterwards is never served.

`jars/build-jars.mjs` builds the jar with a zip library from npm, for example `yazl` or `archiver`.
No Maven and no JDK are needed for it, because the jar holds no compiled class. The version of the
jar is the version in the root `package.json`. The script also writes the pom of the jar, with the
coordinates from `Q-UI-JAR`, packaging `jar` and no dependencies.

## Uploading the UI jar into the Maven repository

`jars/publish-jars.mjs` uploads the UI jar with HTTP `PUT` into the Maven repository from
`Q-MAVEN-REPOSITORY`. Nexus, Artifactory, GitHub Packages and a plain WebDAV server all take a
`PUT` on the path Maven expects:

```
<repository>/<group as path>/<artifactId>/<version>/<artifactId>-<version>.jar
<repository>/<group as path>/<artifactId>/<version>/<artifactId>-<version>.pom
```

For each of the two files the script also uploads the checksums Maven reads, `.sha1` and `.md5`,
and `.sha256` and `.sha512` as well. The credentials come from the environment variables named in
the interview, never from a file in the repository.

So that a Maven release of the backend works:

- The script uploads release versions only, like `1.4.0`. A Maven release refuses a dependency
  with `-SNAPSHOT`. A snapshot would also need time-stamped file names and its own
  `maven-metadata.xml`, which the script would have to write.
- A release version is uploaded once. Before the upload the script asks with `HEAD` for the jar.
  If it is there, the script stops with a message. It never overwrites a release.
- A build for trying something out gets a version of its own, for example `1.4.0-dev.7`. Maven
  treats it like a release version, so it never clashes with a snapshot and is never overwritten.
- The version of the npm packages and of the UI jar is the same, the one in the root `package.json`.
  `scripts/set-version.mjs` sets it in every workspace, see
  [One version, each package published on its own](#one-version-each-package-published-on-its-own).
- Nexus and Artifactory update the `maven-metadata.xml` of the artifact themselves after a `PUT`.
  A plain WebDAV server does not; there the script also writes that file. Check this with your
  repository once, before the first release.

The npm scripts call this: `npm run build:jars` and `npm run publish:jars`. `npm publish` of the npm
workspaces runs next to it, with the same version. The types library must be published before the
types jar is built, see [The types jar](#the-types-jar).

## The nginx image

Some organisations do not put the user interface into the backend but deploy it on its own
(`Q-HOSTING` is `behind` or `in-front`, see
[SKILL.md](../SKILL.md#where-the-user-interface-runs)). For them, `docker/Dockerfile` builds an
image based on the official `nginx` image. It copies `packages/ui/dist` into
`/usr/share/nginx/html` and the configuration into `/etc/nginx/templates/`.

### Behind the backend (`behind`)

The browser talks to the backend only, and the nginx serves files. Its configuration does one
thing: every path that is no file gets `index.html` (`try_files $uri $uri/ /index.html`), and
`index.html` is sent with `Cache-Control: no-cache`. It forwards nothing.

The backend passes on to the nginx what the cockpit does not know itself. The cockpit already
brings `spring-cloud-starter-gateway-server-webmvc` for its proxy under `/wm/`. One more route
goes to the nginx, for the path `/**` and with the lowest priority of all routes. Then every path
of the cockpit (the GUI API, `/wm/`, the login) is answered by the cockpit first, and the security
of the backend covers the files of the user interface as well. The address of the nginx is a
configuration value of the backend. `application.spa-default-file` is not needed then, because the
nginx answers an unknown path with `index.html`.

So far this way has only been built with the session or the token in a cookie.

### In front of the backend (`in-front`)

The address of the backend comes from an environment variable when the container starts. The
configuration does four things:

- Every path that is no file gets `index.html`
  (`try_files $uri $uri/ /index.html`). `index.html` is sent with `Cache-Control: no-cache`.
- nginx forwards to the backend whatever the backend answers: `/gui/api/`, `/wm/`,
  `/logout` and the paths of the login from `Q-LOGIN`, for example `/login`, `/oauth2/` and
  `/login/oauth2/`. So the browser sees one origin, and the session cookie travels with every
  request without CORS.
- `Host`, `X-Forwarded-For`, `X-Forwarded-Proto` and
  `X-Forwarded-Host` are passed on, so a login redirect of the backend points back to nginx.
- `/gui/api/v1/updates` is forwarded with `proxy_buffering off`,
  `proxy_http_version 1.1`, an empty `Connection` header and a long `proxy_read_timeout`, for
  example one hour. Otherwise nginx holds back the events or closes the stream.

```nginx
location /gui/api/v1/updates {
  proxy_pass ${BACKEND_URL};
  proxy_http_version 1.1;
  proxy_set_header Connection "";
  proxy_buffering off;
  proxy_cache off;
  proxy_read_timeout 1h;
}
```

The backend then needs no UI jar. It still has to trust the forwarded headers, in Spring Boot with
`server.forward-headers-strategy=framework` or `native`.

## The backend repository

The backend repository is the cockpit application of the organisation, built with Maven. When the
user wants it (`Q-BACKEND`), the skill creates it:

```
pom.xml                       the parent: the versions, the two modules
types/                        the types jar with the ui-uri-type constants
application/                  the cockpit application, the counterpart of container
docker-compose.yaml           MongoDB, Mailpit, Keycloak for an OIDC login, Kafka with Q-KAFKA
README.md                     how to start it, and the order of the builds
```

The application has the `business-cockpit` library, the main class, the controllers of the lists,
the security configuration of the login, the user directory and its configuration files.
[backend.md](backend.md) describes all of it.

When the repository exists already, read its `pom.xml` for `Q-BACKEND-FRAMEWORK`, and tell the user
which dependency to add. In both cases the application takes the UI jar as an ordinary dependency,
when `Q-HOSTING` is `embedded`:

```xml
<dependency>
  <groupId>com.example.cockpit</groupId>
  <artifactId>business-cockpit-ui</artifactId>
  <version>${ui.version}</version>
  <scope>runtime</scope>
</dependency>
```

A new version of the user interface is a new value of `ui.version` in the parent `pom.xml`, and the
backend is released as usual.

Repacking a finished runnable jar, for example turning `container-without-ui` into `container` by
putting the files of the user interface into it, is not the way this skill takes. If somebody wants
it anyway, two rules apply, and neither is proven by a test yet:

- `Start-Class` in the manifest stays as it is. The main class has a different name in every
  project.
- The nested jars in `BOOT-INF/lib` stay stored without compression. Spring Boot reads them in
  place, and a compressed nested jar does not start.

## The types jar

The values of `ui-uri-type` are a convention between this user interface and its workflow modules.
VanillaBP knows no names for them and has no constant for them. So that a workflow module does not
have to type the string, there is a small Maven artifact with constants (`Q-TYPES-JAR`):

```java
package com.example.cockpit.ui;

/** The values of ui-uri-type this cockpit user interface understands. */
public final class UiUriTypes {

    /** The module's parts are ES modules named in bc-manifest.json, loaded with import(). */
    public static final String ES_MODULES = "ES_MODULES";

    /** The module has pages of its own; the cockpit opens them in a tab of its own. */
    public static final String EXTERNAL = "EXTERNAL";

    private UiUriTypes() {
    }

}
```

`EXTERNAL` is a name of this user interface like any other. It does not point to a constant of
VanillaBP, because there is none. The type decides how a row opens, not the form of its address,
see [module-loading.md](module-loading.md#which-data-finds-the-part).

A workflow module uses the constant in its configuration or its code. The types jar holds one class
and nothing else, so it fits every module.

### Where the types jar is built

The types jar is built with Maven, never in the UI repository. Where it is built depends on
`Q-BACKEND`:

- The skill creates the backend: the types jar is the module `types` of the backend repository,
  next to the module `application`.
- The organisation keeps a backend of its own: the types jar comes from a small Maven project of
  its own, the types module, for example `business-cockpit-ui-types`, in a repository of its own.

The reason is the order of the builds. Each step needs the one before:

1. A release of the cockpit. Its jars carry the OpenAPI files of the GUI API.
2. The user interface, built with npm. It needs the OpenAPI files from the cockpit jars, see
   [gui-api.md](gui-api.md#generating-the-client). It publishes the npm package of the types
   library and uploads the UI jar.
3. The backend repository, built with Maven. Its module `types` reads the npm package of the types
   library, and its module `application` takes the UI jar. With a backend of the organisation's
   own, the types module builds first and the backend after it.
4. The workflow modules take the types jar.

Put the types into the build of the cockpit, and that build needs the npm package of the user
interface, while the user interface needs the jars of the cockpit first. The builds go round in a
circle. Put them into the UI build, and the npm-only UI repository needs `javac`. In the backend
repository they fit, because the backend comes after the user interface anyway.

It is built like this:

1. The UI repository decides the list. The types library carries it as `ui-uri-types.json`, see
   [types.md](types.md#data-from-the-gui-api).
2. The Maven build downloads the npm package `<scope>/bc-types` from the npm registry. In the
   backend repository it takes the version in `ui.version`, the same version as the UI jar. A
   tarball is a plain file, so a plain download needs no npm, for example with
   `download-maven-plugin`.
3. A short build step writes `UiUriTypes.java` from `ui-uri-types.json`, one constant per entry,
   with the description as Javadoc. Maven compiles it as usual.
4. In the backend repository the types jar has the version of the backend. It holds the values of
   the user interface that this backend serves, so a workflow module takes the types jar of the
   cockpit it reports to. The types module of its own has the version of the types library
   instead: `1.4.0` of the jar holds the values of `1.4.0` of the user interface.

The types module of its own, when the skill creates no backend, has two files (`Q-TYPES-JAR`):

```
pom.xml                       downloads the tarball, writes and compiles UiUriTypes.java
README.md                     the order of the builds and why
```

The `README.md` of the backend repository, or of the types module, explains the order and why. It
says, in the words of the project:

- The user interface needs the OpenAPI files from the cockpit jars.
- The types jar needs the npm package of the user interface.
- If the types were part of the cockpit build, the builds would form a circle. If they were part of
  the UI build, the npm-only UI repository would need Java.
- The order of a release. With the backend of the skill: the cockpit release, then the user
  interface (npm), then the backend repository (Maven, the types jar and the application). With a
  backend of the organisation's own: the cockpit release, then the user interface, then the types
  module (Maven, reads the npm tarball), then the backend.

## What still needs Java

The UI repository needs no Java to build. Two things around it still run on Java:

- The end-to-end tests run against a running cockpit with workflow modules, and both are Java
  applications. Run them as container images where the organisation has them. Otherwise the test
  machine needs a Java runtime, but only to start them, not to build anything.
- The dev shell simulator is a runnable jar, see
  [dev-shell.md](dev-shell.md#the-dev-shell-simulator). A module developer needs a Java runtime to
  start it. The UI repository does not build it.

## The types library

`<scope>/bc-types` holds the data and the names of the contract. It has no dependencies and no
peer dependencies, and it has no runtime code beyond constants. It holds the data of the GUI API
as TypeScript interfaces, generated from the OpenAPI files, the other types of the contract, and
every constant: the entries and the names of their functions, the areas, the `ui-uri-type` values
with `ui-uri-types.json`, the names of the events and actions of the event protocol, its error
codes, and the names of the theme tokens. [types.md](types.md) lists what goes into it.

A workflow module does not install it directly. It gets everything of it through the module kit.
The Maven build of the types jar reads its tarball, see [The types jar](#the-types-jar).

It is published to the npm registry from the interview (`Q-NPM-SCOPE`). Its version follows the
version of the user interface, because both sides of a contract change together.

## The module kit

`<scope>/bc-module-kit` is what a workflow module installs to build its parts. It is the cockpit's
own copy of the VanillaBP kit `@vanillabp/bc-module-kit`, see
[Why the cockpit has its own module kit and host](#why-the-cockpit-has-its-own-module-kit-and-host).
Its one dependency is `<scope>/bc-types`, with the exact version of the workspace. It has no peer
dependencies, and it depends on no loading library.

It holds the runtime code a module needs: `defineBcElement`, the module's side of the event
protocol, the functions of the manifest, and the script `bc-check-entries` a module runs in its
build. It exports the types and constants of the types library again, so a module imports
everything from the kit. [types.md](types.md#the-module-kit) lists what goes into it.

It covers everything the VanillaBP kit exports, with the same names and the same behaviour, and adds
the extension points and areas of this cockpit. It ships no host.

## The module host

`<scope>/bc-module-host` is the cockpit's own copy of the VanillaBP host
`@vanillabp/bc-module-host`. It depends on `<scope>/bc-module-kit` and on nothing else, and it has
no framework. It holds the registry of modules, the central loader, the three steps ask, load and
draw, and the cockpit's side of the event protocol, see [module-loading.md](module-loading.md#one-registry-and-one-loader). The
entries and the areas of this cockpit are its defaults, so a caller does not have to name them.

It has two users:

- The user interface. It loads every part of a module through this package, so the cockpit has one
  loader, and the rules of loading are written once. The framework code of the user interface wraps
  it, for example in a service or a hook.
- The tests of a workflow module of the organisation. They load the module's built parts the way
  the cockpit does, without a running cockpit. The blueprints do the same with the VanillaBP host.

The dev shells do not use it. They import the components of a module directly, so the dev server
can swap them, see [dev-shell.md](dev-shell.md#the-entry-point).

## The theme library

`<scope>/bc-theme` holds the look of the cockpit from `Q-THEME`: the colours, the fonts, the logo
and the spacing, also the colours of a new, changed, removed and inactive row. It ships them as a
CSS file that sets the custom properties on the root, under the names the types library exports
(`Q-THEME-TOKENS`), and as plain values for code that needs them. It has no dependencies and no
framework.

The user interface and every dev shell depend on it. So a colour changes in one place, and the
dev shells look like the cockpit without a copy of the theme. It is published with the dev shells,
because they need it.

## The dev shell libraries

`<scope>/bc-dev-shell-<framework>` is generated once for every framework the workflow modules use
(`Q-MODULE-FRAMEWORKS`). It has the framework as a peer dependency and `<scope>/bc-module-kit` and
`<scope>/bc-theme` as dependencies. It takes `BcUserTask`, `BcWorkflow` and the other types from
the module kit, the same as the module it runs. Its entry point is the framework's natural form: a
`bootstrapDevShell` function for React, an application config plus bootstrap for Angular, a
component or plugin for Vue. [dev-shell.md](dev-shell.md) describes what it must do.

The theme of the cockpit (`Q-THEME`) comes from `<scope>/bc-theme` into the base pages of every dev
shell. A user task or a case page must look the same in the dev shell as in the cockpit, as close
to pixel-identical as possible.

## What the project keeps beside the code

- `INTERVIEW.md`, the answers with their question ids.
- `CHECKLIST.md`, the reference checklist plus the customer's items, see
  [`../SKILL.md`](../SKILL.md#phase-2-generate).
- A `README.md` that says how to build, how to publish, how to run against a cockpit, how to run
  the nginx image and how to run the end-to-end tests.
- The texts of the user interface in separate files, one per language, see
  [behaviour.md](behaviour.md#language-and-texts).

The backend repository keeps its own `README.md`, see [backend.md](backend.md#the-readme).
