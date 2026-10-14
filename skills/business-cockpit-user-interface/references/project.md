# The generated project

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The generated project is a pure npm project. It uses npm workspaces, so one repository builds and
publishes everything that belongs to one cockpit user interface. Maven is only used at the very
end, to package and publish the jars.

1. [The layout](#the-layout)
1. [The user interface bundle](#the-user-interface-bundle)
1. [The jar the backend serves](#the-jar-the-backend-serves)
1. [The runnable jar](#the-runnable-jar)
1. [The types library](#the-types-library)
1. [The dev shell libraries](#the-dev-shell-libraries)
1. [What the project keeps beside the code](#what-the-project-keeps-beside-the-code)

## The layout

```
package.json                  the workspace root, with the scripts that build everything
INTERVIEW.md                  the answers of the interview
CHECKLIST.md                  the checklist, see ../SKILL.md
packages/
  ui/                         the cockpit user interface
  bc-types/                   <scope>/bc-types, no dependencies
  bc-dev-shell-<framework>/   <scope>/bc-dev-shell-<framework>, one per framework of the modules
jar/
  pom.xml                     packages ui/dist into the UI jar
runnable/
  pom.xml                     optional, the runnable cockpit with the user interface inside
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
- Every other file must be reachable under `/assets/` or `/static/`. Today only these two prefixes
  are open without a login, see [gui-api.md](gui-api.md#protection-and-login). Many build tools
  put their files into an `assets` folder by default. Where yours does not, configure the output
  path or the base path so that it does.

## The jar the backend serves

A Spring Boot cockpit serves static files from `classpath:/static/`. The runnable `container` jar
sets `spring.web.resources.static-locations` to exactly this place, and the shell is looked up at
`classpath:/static/index.html` unless `application.spa-default-file` says otherwise. So the UI jar
holds the built files below `static/`:

```
static/index.html
static/assets/<the bundle>
META-INF/MANIFEST.MF
```

The file `static/assets/main-1234.js` is served at the URL `/assets/main-1234.js`. Put this jar on
the class path of the cockpit application, and the cockpit serves the user interface.

`META-INF/resources/` is the place Quarkus serves from, and Spring Boot by default as well. It does
not work with the runnable `container` jar, because that jar names `classpath:/static/` as its only
location. There is no Quarkus variant of the cockpit backend today. So `static/` is the one place
that works for every cockpit.

The jar gets the Maven coordinates from the interview (`Q-UI-JAR`). A minimal `pom.xml` in `jar/`
does the packaging:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example.cockpit</groupId>
  <artifactId>business-cockpit-ui</artifactId>
  <version>${revision}</version>
  <packaging>jar</packaging>
  <properties>
    <revision>0.0.0-SNAPSHOT</revision>
  </properties>
  <build>
    <resources>
      <resource>
        <directory>../packages/ui/dist</directory>
        <targetPath>static</targetPath>
      </resource>
    </resources>
  </build>
  <distributionManagement>
    <!-- the repository from Q-MAVEN-REPOSITORY -->
  </distributionManagement>
</project>
```

The npm scripts call it: `npm run build:jar` runs `mvn -B -f jar/pom.xml package`, and
`npm run publish:jar` runs `mvn -B -f jar/pom.xml deploy`. The version in the pom follows the
version in the root `package.json`: the script passes it as `-Drevision=<version>`. A machine that builds the jar needs a JDK and Maven. If that is not wanted, a
jar is a zip file with a `META-INF/MANIFEST.MF`. An npm script can build it with a zip library and
publish it with `mvn deploy:deploy-file`, or with an HTTP `PUT` of the jar and a generated pom to the
Maven repository. The pom is the simpler way and is the default.

## The runnable jar

If the organisation runs the runnable cockpit (`Q-BACKEND` is the `container` jar), build a second
jar: the runnable cockpit with the user interface inside. A second `pom.xml` in `runnable/` has two
dependencies, `io.vanillabp.businesscockpit:container` in the cockpit version from the interview and
the UI jar from above. It repackages both with `spring-boot-maven-plugin`, goal `repackage`, main
class `io.vanillabp.cockpit.BusinessCockpitStandaloneApplication`. That is the same way the
`container` module builds its own runnable jar. The result is started with `java -jar` like the
plain runnable cockpit, and it serves the user interface.

Do not unpack and repack a runnable jar by hand. Spring Boot expects the jars in `BOOT-INF/lib`
stored without compression and listed in an index, and a hand-made change easily breaks that.

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
or a workflow page must look the same in the dev shell as in the cockpit, as close to pixel-identical
as possible.

## What the project keeps beside the code

- `INTERVIEW.md`, the answers with their question ids.
- `CHECKLIST.md`, the reference checklist plus the customer's items, see
  [`../SKILL.md`](../SKILL.md#phase-2-generate).
- A `README.md` that says how to build, how to run against a cockpit and how to run the end-to-end
  tests.
- The texts of the user interface in separate files, one per language, see
  [behaviour.md](behaviour.md#language-and-texts).
