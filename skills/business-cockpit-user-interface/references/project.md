# The generated project

*Part of the description in [`../SKILL.md`](../SKILL.md).*

By default the skill fills two repositories, and each one needs only one build tool.

- The UI repository is a pure npm project. It builds and publishes the user interface, the types
  library, the dev shells, the UI jar and an nginx image. It has no `pom.xml`, and its build needs
  no Java.
- The backend repository is a pure Maven project. It is the cockpit application, and it pulls
  the UI jar as an ordinary dependency. By default it also builds the types jar. It has no
  `package.json`.

Many companies run build agents that have either npm or Maven, not both. Many also treat a build
that downloads a second tool chain later, like `frontend-maven-plugin` does, as a security risk.
Two repositories avoid both problems, and a Maven release of the backend stays an ordinary Maven
release.

1. [The UI repository](#the-ui-repository)
1. [The user interface bundle](#the-user-interface-bundle)
1. [The UI jar](#the-ui-jar)
1. [Uploading the UI jar into the Maven repository](#uploading-the-ui-jar-into-the-maven-repository)
1. [The nginx image](#the-nginx-image)
1. [The backend repository](#the-backend-repository)
1. [The types jar](#the-types-jar)
1. [What still needs Java](#what-still-needs-java)
1. [The types library](#the-types-library)
1. [The dev shell libraries](#the-dev-shell-libraries)
1. [What the project keeps beside the code](#what-the-project-keeps-beside-the-code)

## The UI repository

```
package.json                  the workspace root, with the scripts that build everything
INTERVIEW.md                  the answers of the interview
CHECKLIST.md                  the checklist, see ../SKILL.md
packages/
  ui/                         the cockpit user interface
  bc-types/                   <scope>/bc-types, no dependencies
  bc-dev-shell-<framework>/   <scope>/bc-dev-shell-<framework>, one per framework of the modules
jars/
  build-jars.mjs              builds the UI jar
  publish-jars.mjs            uploads it into the Maven repository
docker/
  Dockerfile                  the nginx image for a separate deployment
  nginx.conf.template
e2e/                          the end-to-end tests, one per checklist item
```

The types library and the dev shells are separate npm packages from the same workspace. That is the
recommended way. A dev shell needs its framework as a peer dependency, and the types library must
not have one: a workflow module in Vue must not be forced to install React. One package with subpath
exports, such as `<scope>/bc/types` and `<scope>/bc/dev-shell-react`, would also work, because tree
shaking drops the parts a module does not use. But its peer dependencies would still name every
framework, so separate packages are cleaner.

The GUI API client is generated at build time into `packages/ui` and into each dev shell, see
[gui-api.md](gui-api.md#generating-the-client). It is not committed.

## The user interface bundle

`packages/ui` builds the single-page application into `packages/ui/dist`. It is built with the
framework and the build tool from the interview. It does not need a federation build of its own:
the cockpit is the host, and the host only needs the runtime, see
[module-federation.md](module-federation.md#native-federation).

Two rules for the output come from the backend:

- `index.html` is the shell. It must not be cached, and it is served for every unknown path.
- Every other file lies below one prefix, `/assets/` by default. Then the backend application
  opens all of them without a login with one rule, see
  [gui-api.md](gui-api.md#protection-and-login). Many build tools put their files into an `assets`
  folder by default. Where yours does not, configure the output path or the base path so that it
  does.

## The UI jar

A jar is a zip file with a `META-INF/MANIFEST.MF`. The UI jar holds the built files at the place
where the backend framework serves static files from its class path (`Q-BACKEND-FRAMEWORK`):

| Backend framework | Place in the jar      | Served at |
|-------------------|-----------------------|-----------|
| Spring Boot       | `static/`             | `/` |
| Quarkus           | `META-INF/resources/` | `/` |

For Spring Boot:

```
static/index.html
static/assets/<the bundle>
META-INF/MANIFEST.MF
```

The file `static/assets/main-1234.js` is served at the URL `/assets/main-1234.js`. Spring Boot
also serves `META-INF/resources/` by default, but the runnable `container` jar sets
`spring.web.resources.static-locations` to `classpath:/static/` only, and the shell is looked up at
`classpath:/static/index.html` unless `application.spa-default-file` says otherwise. So for Spring
Boot, `static/` is the place that works for every cockpit.

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
  `npm version` sets it, and the Git tag is made from it.
- Nexus and Artifactory update the `maven-metadata.xml` of the artifact themselves after a `PUT`.
  A plain WebDAV server does not; there the script also writes that file. Check this with your
  repository once, before the first release.

The npm scripts call this: `npm run build:jars` and `npm run publish:jars`. `npm publish` of the
npm packages runs next to it, with the same version. The types library must be published before the
backend builds the types jar, see [The types jar](#the-types-jar).

## The nginx image

Some organisations do not put the user interface into the backend but deploy it on its own. For
them, `docker/Dockerfile` builds an image based on the official `nginx` image. It copies
`packages/ui/dist` into `/usr/share/nginx/html` and the configuration into
`/etc/nginx/templates/`, so the address of the backend comes from an environment variable when the
container starts.

The configuration does four things:

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

The backend repository is the cockpit application of the organisation, built with Maven. How such
an application is built is in the wiki under
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).
It takes the UI jar as an ordinary dependency:

```xml
<dependency>
  <groupId>com.example.cockpit</groupId>
  <artifactId>business-cockpit-ui</artifactId>
  <version>1.4.0</version>
  <scope>runtime</scope>
</dependency>
```

The skill does not write the application. If the repository exists, read its `pom.xml` for
`Q-BACKEND-FRAMEWORK`, and tell the user which dependency to add. The one thing the skill may add
to it is the module of the types jar, see [The types jar](#the-types-jar). A new version of the user
interface is a new version number in this `pom.xml`, and the backend is released as usual.

Repacking a finished runnable jar, for example turning `container-without-ui` into `container` by
putting the files of the user interface into it, is not the way this skill takes. If somebody wants
it anyway, two rules apply, and neither is proven by a test yet:

- `Start-Class` in the manifest stays as it is. The main class has a different name in every
  project.
- The nested jars in `BOOT-INF/lib` stay stored without compression. Spring Boot reads them in
  place, and a compressed nested jar does not start.

## The types jar

**Proposal, to be confirmed by Stephan.**

The values of `ui-uri-type` are a convention between this user interface and its workflow modules.
VanillaBP knows no names for them and has no constant for them. So that a workflow module does not
have to type the string, there is a small Maven artifact with constants (`Q-TYPES-JAR`):

```java
package com.example.cockpit.ui;

/** The values of ui-uri-type this cockpit user interface understands. */
public final class UiUriTypes {

    /** The module's parts are loaded into the cockpit through native federation. */
    public static final String NATIVE_MF = "NATIVE_MF";

    /** The module has pages of its own; the cockpit opens them in a tab of its own. */
    public static final String EXTERNAL = "EXTERNAL";

    private UiUriTypes() {
    }

}
```

`EXTERNAL` is a name of this user interface like any other. It does not point to a constant of
VanillaBP, because there is none. The type decides how a row opens, not the form of its address,
see [module-federation.md](module-federation.md#which-data-finds-the-part).

A workflow module uses the constant in its configuration or its code. The types jar holds one class
and nothing else, so it fits every module.

A jar with a compiled class needs `javac`, and the UI repository stays npm-only. So the types jar is
built where Maven already runs. The default is a small Maven module in the backend repository, for
example `business-cockpit-ui-types`. If the backend repository should not carry it, it is a tiny
Maven project of its own. Either way it is built like this:

1. The UI repository decides the list. The types library carries it as `ui-uri-types.json`, see
   [types.md](types.md#data-from-the-gui-api).
2. The Maven module downloads the npm package `<scope>/bc-types` of the same version from the npm
   registry. A tarball is a plain file, so a plain download needs no npm, for example with
   `download-maven-plugin`.
3. A short build step writes `UiUriTypes.java` from `ui-uri-types.json`, one constant per entry,
   with the description as Javadoc. Maven compiles it as usual.
4. The version of the types jar is the version of the types library. So `1.4.0` of the jar always
   holds the values of `1.4.0` of the user interface.

The generator writes this module into the backend repository only when the user agrees, and tells
the user which coordinates it has (`Q-TYPES-JAR`).

## What still needs Java

The UI repository needs no Java to build. Two things around it still run on Java:

- The end-to-end tests run against a running cockpit with workflow modules, and both are Java
  applications. Run them as container images where the organisation has them. Otherwise the test
  machine needs a Java runtime, but only to start them, not to build anything.
- The dev shell simulator is a runnable jar, see
  [dev-shell.md](dev-shell.md#the-dev-shell-simulator). A module developer needs a Java runtime to
  start it. The UI repository does not build it.

## The types library

`<scope>/bc-types` is what a workflow module installs to build its parts. It has no dependencies
and no peer dependencies, and it does not depend on module federation. It holds the types and the
small helper that runs the event protocol. [types.md](types.md) lists what goes into it.

It is published to the npm registry from the interview (`Q-NPM-SCOPE`). Its version follows the
version of the user interface, because both sides of a contract change together.

## The dev shell libraries

`<scope>/bc-dev-shell-<framework>` is generated once for every framework the workflow modules use
(`Q-MODULE-FRAMEWORKS`). It has the framework as a peer dependency and `<scope>/bc-types` as a
dependency. Its entry point is the framework's natural form: a `bootstrapDevShell` function for
React, an application config plus bootstrap for Angular, a component or plugin for Vue.
[dev-shell.md](dev-shell.md) describes what it must do.

The theme of the cockpit (`Q-THEME`) is built into the base pages of every dev shell. A user task
or a case page must look the same in the dev shell as in the cockpit, as close to pixel-identical
as possible.

## What the project keeps beside the code

- `INTERVIEW.md`, the answers with their question ids.
- `CHECKLIST.md`, the reference checklist plus the customer's items, see
  [`../SKILL.md`](../SKILL.md#phase-2-generate).
- A `README.md` that says how to build, how to publish, how to run against a cockpit, how to run
  the nginx image and how to run the end-to-end tests.
- The texts of the user interface in separate files, one per language, see
  [behaviour.md](behaviour.md#language-and-texts).
