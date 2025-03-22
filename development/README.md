![](../readme/vanillabp-headline.png)

# Development

There are three stages of development, all explained by the contents of this module:
1. Building business applications using *VanillaBP Business Cockpit* as a frontend for user tasks.
1. Building a custom user task application based on *VanillaBP Business Cockpit*.
1. Contributing to *VanillaBP Business Cockpit* to improve it by fixes or new features.

Pick your topics according to the desired stage. If one is interested in stage 2 or 3 then it is
necessary to also read the preceding stage(s) before, since it is important to know about details
only explained in the respective section.

*Hint:* The *VanillaBP Business Cockpit* UI components are written in React. So, most of the
documentation shows examples using React. Additionally, there are chapters explaining how to
use Angular instead. However, please read the entire documentation since concepts are
explained only once.

**Contents:**

1. [Developing business applications](#developing-business-applications)
    1. [Building a business microservice](#building-a-business-microservice)
        1. [A business application monolith](#a-business-application-monolith)
        1. [Building a runtime container for multiple workflows](#building-a-runtime-container-for-multiple-workflows)
            1. [Using a mono-repo](#using-a-mono-repo)
            1. [Using multiple repositories](#using-multiple-repositories)
    1. [List user tasks and workflows in the business cockpit](#list-user-tasks-and-workflows-in-the-business-cockpit)
        1. [Report user tasks to the business cockpit](#report-user-tasks-to-the-business-cockpit)
        1. [Report workflows to the business cockpit](#report-workflows-to-the-business-cockpit)
    1. [Show user tasks forms and workflow status sites in the business cockpit](#show-user-tasks-forms-and-workflow-status-sites-in-the-business-cockpit)
        1. [Define user task forms](#define-user-task-forms)
        1. [Define workflow status-sites](#define-workflow-status-sites)
        1. [Developing user tasks forms in a local environment](#developing-user-tasks-forms-in-a-local-environment)
        1. Using Angular
    1. Customize lists of user tasks and workflows
        1. Define columns of the user task list
        1. Build custom rendered columns
        1. Create predefined lists
    1. Add custom functionality to the *VanillaBP Business Cockpit*<br>(e.g. a workflow start form)
1. Building a customized business cockpit
    1. Customizing the theme
    1. Integrating business cockpit
    1. Using Angular
1. Contributing to the *VanillaBP Business Cockpit*

## Developing business applications

In this context a business application is assumed as a service which is about
business workflows. Typically, those applications do not include the UI and
functionality of the user task application, but [they feed it](../adapters).

For tiny applications meant to be used in a non-enterprise context it might make sense to merge
the user task application and the business application but this is currently not supported by
the *VanillaBP Business Cockpit* (feel free to
[fill an issue](https://github.com/vanillabp/business-cockpit/issues) if you interested in this option).

### Building a business microservice

The *VanillaBP Business Cockpit* is a microservice. That means it is only about providing cockpit functionality.
The workflows have to be executed by other (micro-)services called business applications or business services.
Each business application bundles BPMN, business code and UI components of a certain use case implemented.
In this chapter one will find how to build up such a business application microservice.

Defining the size of a microservice (how micro is a microservice?) is a long discussed topic.
Unfortunately, there is not only "the way" to do. Actually it depends on considerations regarding operating those
microservices, building independent chunks of code and constellations of teams building the microservices.
However, VanillaBP supports all ways of sizing microservices by introducing
[workflow modules](https://github.com/vanillabp/spring-boot-support?tab=readme-ov-file#workflow-modules).

A workflow module is an independent bundle of components needed to run a certain workflow:
The BPMNs, the business code and the UI components. Some workflows covered by a workflow module
are big and cannot be splitted into parts. Typically, those workflow modules will also be deployed
as a single microservice.
Other workflow modules are tiny and one may consider grouping them to minimize resource consumption
and operational workload. In other situations it's not clear and one might want to group
workflow modules first and maybe split out individual workflow modules later, if necessary. Or
build a microservice for a single workflow which is prepared to also host upcoming workflows.

The next chapters explain how to achieve all of these variants.

#### A business application monolith

A business application monolith is a microservice which is about exactly one workflow module.
Building microservices for multiple workflow modules requires a certain project structure which
can be skipped in case of a monolith. However, keep in mind that merging monolithic applications
later will cause a refactoring for building those structures.

The project-structure of a monolithic business application looks like this:

```
src
  main
    java         -> the business code AND the runtime container code
    resources
      processes  -> the BPMN resources
    webapp       -> the user tasks to be shown by the user task application
pom.xml          -> also including the adapter dependency specific to the BPMS used
```

Checkout the
[respective Blueprint](https://github.com/vanillabp/blueprint-workflowmodule-springboot-standalone-businesscockpit)
implementing this structure, as basis for own applications.

#### Building a runtime container for multiple workflows

The project structures explained in this chapter apply to these scenarios:

1. Running multiple workflow modules in a single runtime container (to minimize resource consumption and
   operational workload).
1. Building the first workflow module of several desired to be hosted in a single runtime container.
1. Be prepared for splitting out a single workflow module (it may be necessary in the future due to
   operational aspects).

In these situations the workflow module is not a standalone application anymore. It is just a
library fetched as a dependency by a runtime container forming the actual application.

##### *Using multiple repositories*

Typically, using multiple repositories makes sense if the workflow modules are built
by several teams, so each team does not touch the code built by others. A similar situation is
when workflow modules built by one team over the time are released independently.
The main advantage of using multiple repositories is to be independent from
other workflow modules development cycle.

This is the structure of the workflow module's Maven project:

```
src
  main
    java         -> the workflow's business code ONLY
    resources
      processes  -> the BPMN resources
    webapp       -> the user tasks to be shown by the user task application
pom.xml          -> NOT including the adapter dependencies, only SPI
```

The runtime container is a separate Maven project having this structure:

```
src
  main
    java         -> the runtime container code ONLY
pom.xml          -> including dependencies for the SPI-adapter AND the worfklow module
```

Since the runtime container is not part of the workflow module's Maven project one needs a
separate runtime container for local development. This can be achieved by adding a
light-weight runtime container using Maven sub-modules.

```
pom.xml          -> parent POM
workflow         -> the module holding the workflow's code as shown above
runtime          -> light-weight runtime conatiner for local development
```

Sometimes, also other parts are needed which can be easily integrated using sub-modules:

```
pom.xml          -> parent POM
workflow         -> the module holding the workflow's code as shown above
development      -> everything needed for local development
  pom.xml        -> parent POM of development module
  runtime        -> light-weight runtime conatiner for local development
  simulator      -> a module which mimics foreign system for local development
```

*Hint:* In this example a simulator is used as an additional module. A simulator is a standalone
Spring Boot application which mimics foreign systems the workflow module depends on to be
used for local development. If the workflow module consumes a REST service the simulator
is meant to provide this REST service. A simulator may also include some simple behaviour to
support local development. Additionally, a simulator can be used for Spring Boot tests
in which no beans are mocked but the foreign systems are mocked instead.

Checkout the
respective Blueprints for the
[workflow module](https://github.com/vanillabp/blueprint-workflowmodule-springboot-businesscockpit)
and the
[runtime container](https://github.com/vanillabp/blueprint-workflowmodule-springboot-host)
implementing this structure, as basis for own applications.

##### *Using a mono-repo*

```
pom.xml          -> parent POM
workflow1        -> the module holding the workflow's code as shown above
workflow2        -> the module holding the workflow's code as shown above
runtime          -> the runtime conatiner
```

Using a mono-repo sometimes makes things easier: There is no need for a light-weight runtime container
since the original runtime container can be used for local development (e.g. using a special Spring
boot profile). On the other hand this means to always run all workflows at local development
even if your are working only on one workflow module.

A mono-repo typically makes sense if the workflow modules included form a logical group of a higher
level use-case in which one might want to also share business code. An example are workflows regarding
telecom services: Activating and deactivating a SIM card are independent workflows of a higher level
use-case.

### List user tasks and workflows in the business cockpit

To show user tasks and workflows of [VanillaBP](https://github.com/vanillabp/spi-for-java) based
[workflow modules](https://github.com/vanillabp/spring-boot-support#workflow-modules)
in the *VanillaBP Business Cockpit* one has to use the [business cockpit SPI](../spi-for-java)
in the respective workflow module.

#### Report user tasks to the business cockpit

For user tasks which should show up in the user task application the respective
[VanillaBP workflow service](https://github.com/vanillabp/spi-for-java#wire-up-a-process)
must have a corresponding method:

```java
    @UserTaskDetailsProvider
    public UserTaskDetails retrievePayment(
            final PrefilledUserTaskDetails details,
            final PaymentAggregate aggregate) {
        details.setDueDate(aggregate.getWishDate());
        return details;
    }
```

Details about providing user task details can be found in
[the SPI documentation](../spi-for-java#wire-up-a-user-task).

Typically, all user tasks of a BPMN process have to show up in the user task application to
be completed by agents. However, there are exceptions: Sometimes there are user tasks
fulfilled by people having no access to the user task application (external employee). Nonetheless,
the BPMN model should show a user task for this activity to form a semantically correct model of
the real world. Those user tasks are not meant to show up in the user task application.
This is achieved by simply skipping the `@UserTaskDetailsProvider` annotated method.

#### Report workflows to the business cockpit

The *VanillaBP Business Cockpit* based user task application may also list workflows to provide
runtime and historical information to business people. For workflows which should show up in the user task application the respective
[VanillaBP workflow service](https://github.com/vanillabp/spi-for-java#wire-up-a-process)
must have a corresponding method:

```java
    @WorkflowDetailsProvider
    public WorkflowDetails workflowDetails(
            final PrefilledWorkflowDetails details,
            final PaymentAggregate aggregate) {
        details.setFulltextSearch(aggregate.getCustomerName());
        return details;
    }
```

Details about providing workflow details can be found in
[the SPI documentation](../spi-for-java#wire-up-a-workflow).

However, workflows may also be used to
simplify software by using a BPMN engine instead of introducing the complexity of a
self implemented state engine. Those workflows may not show up in the business cockpit.
This is achieved by simply skipping the `@WorkflowDetailsProvider` annotated method.

### Show user tasks forms and workflow status sites in the business cockpit

User tasks have to be presented by the user task application based on the list of user tasks
accessible to the current user. However, the backend (e.g. REST-endpoints) to complete the form
or to load details shown in the user task form are provided by the business (micro-)service
hosting the respective workflow module. To avoid cross-site-scripting, the *VanillaBP Business Cockpit*
backend  acts as a proxy to business services. This spans one consistent umbrella of security across the
entire application. Every workflow module has to register to the business cockpit on startup to
initialize the respective proxy. This is done automatically by the
[business cockpit SPI adapter](../adapters).

Since the user task application is a separate microservice next to the business (micro-)services,
there must be a mechanism to provide the UI components for rendering the user task forms of a workflow module
as part of the user task application. In a classic framework based web application (React, Angular, etc.)
all components potentially shown must be known at the time of bundling the web application (e.g. using Webpack).
This is not given for the user task application and the workflow modules!

To overcome this *Module Federation* is used. A federated module is a bunch of Javascript code
implementing a custom contract. In case of the user task application there is one federated module
for each workflow module containing all user task UI components and workflow status-site UI components.
The "contract" are named properties of the module expected by the user task application containing
UI components. Each federated module is bundled as part of building the workflow module and
loaded by the user task application on demand from the business (micro-)service through the proxy
mentioned above.

The *VanillaBP Business Cockpit* components for listing user tasks, listing workflows, showing
user task forms and showing workflow status-sites expect four parts `UserTaskList`, `UserTaskForm`,
`WorkflowPage` and `WorkflowList` to be exposed by using Webpack's `ModuleFederationPlugin`:

```javascript
  new ModuleFederationPlugin({
      name: "MyWorkflowModule",
      filename: 'remoteEntry.js',
      exposes: {
          UserTaskList: './src/UserTaskList',
          UserTaskForm: './src/UserTaskForm',
          WorkflowPage: './src/WorkflowPage',
          WorkflowList: './src/WorkflowList',
          Header: './src/Header'
      },
      ...
  }
```

Each part is explained in to next chapters. Any additional parts are custom components
which may be used by a customized user task application. In this example the name of the
additional part `Header` suggests that it will be used to add workflow module based
functionality to the user task applications header region. In this way the custom user task
application may offer space in which workflow modules add functionality not specific to
a certain workflow or a certain user task.

*Note on Module Federation:* On building a classic framework web application a Javascript bundle
is built combining all the UI components of the entire application. This should reduce startup time
of the web application when loaded in the browser (one big file instead of many small files;
code is minified). For bigger applications those bundles become huge
(100k to several megabytes) but not all of those components are shown at the start page of the
web application. Most of them belong to pages presented during using the application. Some
of them are never shown due to limited permissions of the current user. To limit the amount of
Javascript loaded by the browser "lazy-loading" was introduced: In the web application UI components
not needed at the very beginning of the user session are marked to be loaded "lazy" on demand.
This splits the main bundle into smaller bundles loaded automatically once the web application
needs to render a UI component packed into one of those splitted bundles. The main bundle still
contains the root of the web application as well as UI components used by more than one bundle
splitted out. Module federation uses the same mechanism to load bundles but those bundles are
not known to the main web application a priori. The main web application may load bundles
programmatically on demand expecting to contain UI components at well-known properties of
the module which form a contract the module has to fulfill.

#### Define user task forms

The workflow module's federated module's exposed part `UserTaskForm` is used by the *VanillaBP
Business Cockpit* to render user tasks.

A user task form UI component is a React component of type [UserTaskForm](ui/bc-shared/src/types/UserTaskForm.ts)
receiving one single parameter `userTask` which is of type [BcUserTask](../ui/bc-shared/src/types/BcUserTask.ts):

```typescript jsx
import { UserTaskForm } from '@vanillabp/bc-shared';

const MyUserTaskForm: UserTaskForm = ({ userTask }) => {
  return (<div>My Form</div>);
}

export {
   buildVersion,
   buildTimestamp,
   UserTaskFormComponent as UserTaskForm
}
```

The `userTask` parameter consists of properties describing the user task next to functions used to
manage the user task (e.g. assign, unassign, etc.). Checkout the
[definition](../ui/bc-shared/src/types/BcUserTask.ts) for details. Next to the `UserTaskForm` also the
properties `buildVersion` and `buildTimestamp` are exported which are used by the user task application
to trigger reloading a previously loaded module if the user task's version information does not match
the exposed properties.

As mentioned previously, only one federated module is loaded for each workflow module. Each individual
user task  form UI component contained in the federated module is not explicitly exposed. Instead, only one
user task form is exposed and rendered by the user task application. It's the responsibility of
this single exposed user task form UI component to check which is the desired user task form based
on data handed over and render it. Additionally, it is recommended to
use lazy loading to only load UI components at runtime required by the desired user task:

```typescript jsx
import { lazy } from 'react';
import { UserTaskForm } from '@vanillabp/bc-shared';

const RideWorkflowRetrievePayment = lazy(() => import('./ride-workflow/retrieve-payment'));

const TaxiRideUserTasksForm: UserTaskForm = ({ userTask }) => {
  if ((userTask.bpmnProcessId === 'Ride') && (userTask.taskDefinition === 'RetrievePayment')) {
    return <RideWorkflowRetrievePayment userTask={ userTask }/>
  }
  return <div>{ `unknown user task '${userTask.taskDefinition}' of BPMN process ID '${userTask.bpmnProcessId}'!` }</div>;
}

export { TaxiRideUserTasksForm as UserTaskForm }
```

For details checkout the [simulator's webapp](simulator/src/main/webapp-react) which is used to mimic
a workflow module and may be used as a template for own workflow module webapps.

#### Define workflow status-sites

The workflow module's federated module's exposed part `WorkflowPage` is used by the *VanillaBP
Business Cockpit* to render workflow status-sites. The mechanism is exactly the same as for user tasks
described in the [former chapter](#define-user-task-forms).

```typescript jsx
import { lazy } from 'react';
import { WorkflowPage } from '@vanillabp/bc-shared';

const RideWorkflowStatusSite = lazy(() => import('./ride-workflow/status-site'));

const TaxiRideWorkflowPage: WorkflowPage = ({ workflow }) => {
  if (workflow.bpmnProcessId === 'Ride') {
    return <RideWorkflowStatusSite workflow={ workflow }/>
  }
  return <div>{ `unknown workflow having BPMN process ID '${workflow.bpmnProcessId}'!` }</div>;
}

export { TaxiRideWorkflowPage as WorkflowPage }
```

The `workflow` parameter consists of properties describing the user task next to functions used to
load additional information regarding the workflow (e.g. getUserTasks, etc.). Checkout the
[definition](../ui/bc-shared/src/types/BcWorkflow.ts) for details.

#### Developing user tasks forms in a local environment

UI developers prefer to use [hot module replacement](https://webpack.js.org/concepts/hot-module-replacement/)
as an efficient way of development. Unfortunately, Module Federation breaks it which forces the
developer to run the business cockpit next to the workflow module, bundle the federated module after
each change and refresh the browser to reload the new bundle. This is not a good developer experience.

To overcome this a tiny webapp-wrapper is provided called **Dev Shell**. It is available as a
React application as well as an Angular application and imports the workflow modules components
in a regular way. In this way hot module replacement is available and user task forms or
workflow status-sites can be developed without running the business cockpit.

Enabling the Dev Shell is done by adapting your Webpack configuration depending on the build's target
environment:

```javascript
process.env.NODE_ENV !== 'production'
  ? { entry: './test/index.tsx' }                  // run Dev Shell
  : { output: { publicPath: '/wm/TestModule/' } }  // build federated module
```

For details checkout the [simulator's webapp](simulator/src/main/webapp-react) which is used to mimic
a workflow module and may be used as a template for own workflow module webapps.

Bootstrapping the Dev Shell is done by passing the parts typically exposed in the federated module
to a ready-to-use function:

```typescript
import { bootstrapDevShell } from '@vanillabp/bc-dev-shell-react';
import { UserTaskForm } from '../src/UserTaskForm';
import { UserTaskListCell, userTaskListColumns } from '../src/UserTaskList';
import { WorkflowListCell, workflowListColumns } from '../src/WorkflowList';
import { WorkflowPage } from '../src/WorkflowPage';
import { Header } from '../src/Header';

bootstrapDevShell(
    'root',                // name of the HTML tag to render into
    '/official-api/v1',    // where to load data from to fill the parameters of UI components
    UserTaskForm,
    userTaskListColumns,
    UserTaskListCell,
    workflowListColumns,
    WorkflowListCell,
    WorkflowPage,
    {                      // optional: custom components exposed by the federated module
       'Header': Header
    });
```

After running the dev server a tiny web application is started listing the workflow module's UI
components for interactive development using hot module replacement. Also additional components
(e.g. `Header`) are listed. In this way one can test all UI components without running the business
cockpit itself.

*Hint:* The DevShell is only about UI development. The VanillaBP Business Cockpit
is still needed as a backend. For simplifying local development the
[DevShell Simulator](./dev-shell-simulator) can be used instead
running the original Business Cockpit application.

## Contributing to the *VanillaBP Business Cockpit*

The VanillaBP business cockpit is a Java Spring Boot application using React as a web framework.

For local development there are two preconditions:

1. [A MongoDB cluster](#mongodb)
1. [A local NPM registry](#local-npm-registry)

The MongoDB and the NPM registry can be started by a prepared configuration using docker-compose:

```sh
cd development
docker-compose up -d
```

Afterwards one can [build and run the business cockpit](#build-and-run-the-business-cockpit).

*Hint:* Building the business cockpit will also establish npm-links between the packages provided by this repository. This helps to ensure using the right version during the build and also supports local development.

### MongoDB

For production MongoDB one has to use replica-sets because the VanillaBP business cockpit
uses the MongoDB `changestream` feature, which not available otherwise
(see https://www.mongodb.com/docs/manual/changeStreams/).

The local MongoDB is accessible at mongodb://127.0.0.1:27017 with your favourite tool,
without user and password (which gives you an admin role).

The development application user credentials are:

* *username:* business-cockpit
* *password:* business-cockpit

and the replica set is named `rs-business-cockpit`.

### Local NPM registry

As part of the build NPM packages are published which has to be used by BPMS software which wants to integrate to the VanillaBP business cockpit. Additionally, the business cockpit itself uses those packages as dependencies. To make this work for local development as well as for publishing builds one has to use a local NPM registry. For this the tool [Verdaccio](https://www.verdaccio.org/) is used which is also part of the provided `docker-compose.yaml`.

To use this registry one has to create a file `.npmrc` in your home folder:

```
@vanillabp:registry=http://localhost:4873
//localhost:4873/:_authToken="fake"
```

*Hint:* Verdaccio is preconfigured to accept unauthorized attempts of publishing NPM packages. Therefore, as you can see, the content of the authentication token `fake` is not taken into account - it just has to be filled.

To connect to the registry UI use these parameters:

* *URL:* [http://localhost:4873/](http://localhost:4873/)
* *username:* admin
* *password:* admin

*Hint:* If you do repeating builds for testing then you have to use the Maven profile `unpublish-npm` which removes previously published packages from the local registry.

## Build and Run the Business Cockpit

The business cockpit is developed by using Java 17 and Spring Boot 3 (reactive). To build the business cockpit Maven is used:

```sh
cd vanillabp-business-cockpit
mvn -Dnpm.registry=http://localhost:4873 package -P unpublish-npm
```

(`-P unpublish-npm` is a Maven profile which forces to unpublish NPM components previously published to Verdaccio. You might need to skip this profile for the very first build since there was no packages published before.)

After the build succeeded the service can be started:

```sh
cd business-cockpit
java -Dspring.profiles.active=local -jar target/business-cockpit-*-runnable.jar
```

To connect to the business cockpit UI use these parameters:

* *URL:* [http://localhost:8080/](http://localhost:8080/)
* *username:* test
* *password:* test

This should show up an empty business cockpit since no user tasks or workflows were reported yet. To test this before connecting your business service one can use the [simulation service](#simulation-service).

### Repeating builds

During developing the Business Cockpit itself one might change only backend code and whishes to test that changes without doing a full build.
This can be easily achieved by this Maven command:

```sh
mvn install -Plocal-install
```

If you do some UI development one can run

```sh
npm start
```

in every UI source directory to get a continuous build.

Since the first build links all npm packages, changes in one packages are active immediately using this technique.
There is one exception: The `webapp-angular`-UI in `simulator` uses the package `dev-shell-angular` and Angular does not support NPM linking. So extending `dev-shell-angular` implies the need to publish the package to the local registry after every change and update the `webapp-angular` afterward to use that change:

```sh
cd development/dev-shell-angular
mvn -Dnpm.registry=http://localhost:4873 package -P unpublish-npm
cd -
cd development/simulator/src/main/webapp
npm update --scope '@vanillabp/*'
```

## Simulation Service

This is a service which can be used for local development and testing. I mimics a business service which reports user tasks and workflows to be shown in the business cockpit UI.

To start it use these commands:

```sh
cd development/simulator
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar target/simulator-*-runnable.jar
```

Now you can open the [test data generator form](http://localhost:8079/testdata/usertask/form) in your browser. It gives you verify parameters to generate user tasks. For now simply press the `Generate` button. 10 new user tasks should appear in the VanillaBP business cockpit immediately.

## Integrate your Business Services

The [simulation service](#simulation-service) also acts as template for your services. In `development/simulator/src/main/webapp` you will find samples of user task forms.

to be completed...

## Mongo

Use

```sh
docker-compose up -d
```

to start Mongo database. It is required to use MongoDB as a ReplicaSet (for change-streams), so one has to add this line to you `/etc/hosts` file:

```sh
127.0.0.1       business-cockpit-mongo
```

*Hint:* For Windows the file is `C:\Windows\System32\drivers\etc\hosts`.

Use these parameters to connect to Mongo database using a GUI database tool:

- *Hostname:* business-cockpit-mongo
- *Port:* 27017
- *Replica set:* rs-business-cockpit
- *Username:* business-cockpit
- *Password:* business-cockpit
- *Authentication database:* business-cockpit
- *Database:* business-cockpit

## Simulator

The *simulator* is a standalone Spring Boot application which mimiks the behavior of bounded systems. It can be used to develop features of tasklist without the need of having external services available.

### Generate testdata

To develop tasklist features dummy tasks can be generated. At [this URL](http://localhost:8079/testdata/usertask/form) you will find a form used to trigger new tasks. The tasks generated are independent from any process instance data.
