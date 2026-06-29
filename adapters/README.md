![](../readme/vanillabp-headline.png)

# Adapters

Business Cockpit adapters implement the [SPI for Java](../spi-for-java) which is used by business applications
to provide business data about user tasks and workflows. They are responsible for processing
events of the underlying Business Processing Management System (BPMS) to keep the data shown in the
*VanillaBP Business Cockpit* (list of user tasks, list of workflows) up to date.

**Contents:**
1. [General](#general)
1. [Common adapter functionality]()
1. [BPMS-specific Adapters](#bpms-specific-adapters)
1. [List user tasks and workflows in the business cockpit](#list-user-tasks-and-workflows-in-the-business-cockpit)
   1. [Report user tasks to the business cockpit](#report-user-tasks-to-the-business-cockpit)
   1. [Using templates for user task details](#using-templates-for-user-task-details)
   1. [Report workflows to the business cockpit](#report-workflows-to-the-business-cockpit)
   1. [Using templates for workflow details](#using-templates-for-workflow-details)
1. [Show user tasks forms and workflow status sites in the business cockpit](#show-user-tasks-forms-and-workflow-status-sites-in-the-business-cockpit)
   1. [Define user task forms](#define-user-task-forms)
   1. [Define workflow status-sites](#define-workflow-status-sites)
   1. [Developing user tasks forms in a local environment](#developing-user-tasks-forms-in-a-local-environment)
   1. Using Angular

## General

The business cockpit adapters are the ready-to-use components for *Spring Boot* and [VanillaBP based](https://github.com/vanillabp/spi-for-java)
business applications. The business workflow module should only use the SPI as a dependency but the
runtime container must provide the dependency for adapter specific to the BPMS used
(see ["Using an adapter"](../spi-for-java#using-an-adapter)).

*Hint:* For simple use cases the Maven module
for the business workflow module and the Maven module for the runtime container is the same
(see ["Workflow modules"](https://github.com/vanillabp/spring-boot-support?tab=readme-ov-file#workflow-modules)).

## Common adapter functionality

There are feature which are common to all adapters provided. These features are implemented in a
separate Maven module used as a dependency by adapters specific to a BPMS.

However, those features a described in [separate documentation](./commons):

1. [Configuration of event propagation](./commons#configuration-of-event-propagation)
1. [Using templates for event details](./commons#using-templates-for-event-details)

## BPMS-specific Adapters

There are configuration properties specific to the underlying BPMS. Checkout each adapter documentation
for details:

1. [Camunda 7](./camunda7)
1. [Camunda 8](./camunda8)

## List user tasks and workflows in the business cockpit

To show user tasks and workflows of [VanillaBP](https://github.com/vanillabp/spi-for-java) based
[workflow modules](https://github.com/vanillabp/spring-boot-support#workflow-modules)
in the *VanillaBP Business Cockpit* one has to use the [business cockpit SPI](../spi-for-java)
in the respective workflow module.

### Report user tasks to the business cockpit

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

This method is called if a user task changes caused by any action of the process engine,
since the Business Cockpit needs to be updated in order to show current data in the list
of user tasks (e.g. due date, assignee, etc.). Typical events are:
- user task created
- user task assigned
- user task updated
- user task completed
- user task canceled (e.g. due to an interrupting boundary event)

Details about providing user task details can be found in
[the SPI documentation](../spi-for-java#wire-up-a-user-task).

Typically, all user tasks of a BPMN process have to show up in the user task application to
be completed by agents. However, there are exceptions: Sometimes there are user tasks
fulfilled by people having no access to the user task application (external employee). Nonetheless,
the BPMN model should show a user task for this activity to form a semantically correct model of
the real world. Those user tasks are not meant to show up in the user task application.
This is achieved by simply skipping the `@UserTaskDetailsProvider` annotated method.

### Using templates for user task details

As mentioned in [the SPI documentation](../spi-for-java#using-templates-for-user-task-details)
templates can be used for some user task details instead of just passing strings. Those
templates will be rendered using [Freemarker](https://freemarker.apache.org/).

For rendering the `detailsFulltextSearch` additional values are provided to the template
next to the template context provided by the application:
- `taskDefinitionTitle`: The given or rendered title of the task definition
- `taskTitle`: The given or rendered title of the user task
- `workflowTitle`: The given or rendered title of the workflow
- `.lang`: The current language (e.g. `en`, `de`, etc.)

If the value of a detail supported to be rendered by template is not set
(neither to a text nor to a template name), then a default template is used if found.
The template names are:
- `taskDefinitionTitle`: `task-definition-title.ftl`
- `title`: `task-title.ftl`
- `workflowTitle`: `workflow-title.ftl`
- `detailsFulltextSearch`: `task-fulltext-search.ftl`

The directories for looking up the templates are [given by properties](../commons/README.md#using-templates-for-event-details).

Additionally, a template for each locale defined in the map-keys of those rendered details
can be used. E.g.:

- `workflow-title_en.ftl`
- `workflow-title_de.ftl`

**Hint:** `task-fulltext-search.ftl` is only rendered once with the first locale but all
configured languages are passed as `taskLanguages`. 

### Report workflows to the business cockpit

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

This method is called if a workflow changes caused by any action of the process engine,
since the Business Cockpit needs to be updated in order to show current data in the list
of workflows (e.g. due date, assignee, etc.). Typical events are:
- workflow created
- workflow updated
- workflow ended
- workflow canceled (e.g. due to administrative actions if supported by the BPMS)

Details about providing workflow details can be found in
[the SPI documentation](../spi-for-java#wire-up-a-workflow).

However, workflows may also be used to
simplify software by using a BPMN engine instead of introducing the complexity of a
self implemented state engine. Those workflows may not show up in the business cockpit.
This is achieved by simply skipping the `@WorkflowDetailsProvider` annotated method.

### Using templates for workflow details

As mentioned in [the SPI documentation](../spi-for-java#using-templates-for-workflow-details)
templates can be used for some workflow details instead of just passing strings. Those
templates will be rendered using [Freemarker](https://freemarker.apache.org/).

For rendering the `detailsFulltextSearch` additional values are provided to the template
next to the template context provided by the application:
- `workflowTitle`: The given or rendered title of the workflow
- `.lang`: The current language (e.g. `en`, `de`, etc.)

If the value of a detail supported to be rendered by template is not set
(neither to a text nor to a template name), then a default template is used if found.
The template names are:
- `workflowTitle`: `workflow-title.ftl`
- `detailsFulltextSearch`: `workflow-fulltext-search.ftl`

The directories for looking up the templates are [given by properties](../commons/README.md#using-templates-for-event-details).

Additionally, a template for each locale defined in the map-keys of those rendered details
can be used. E.g.:

- `workflow-title_en.ftl`
- `workflow-title_de.ftl`

**Hint:** `workflow-fulltext-search.ftl` is only rendered once with the first locale but all
configured languages are passed as `workflowLanguages`.

## Show user tasks forms and workflow status sites in the business cockpit

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

### Define user task forms

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

### Define workflow status-sites

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
