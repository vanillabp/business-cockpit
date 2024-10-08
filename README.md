[![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](./LICENSE)

![VanillaBP](./readme/vanillabp-headline.png)

# VanillaBP Business Cockpit

The *VanillaBP Business Cockpit* is an application for business people to work with business processes. It is about:

1. *Workflows* (= Processes currently executed):
     1. Search for workflows by business data.
     1. View active and retired workflows.
     1. Show business aspects and provide actions.
1. *User tasks* (= items of work of a workflow to be done by humans):
     1. Search for user tasks by business data.
     1. List active and retired user tasks.
     1. Fulfill user tasks.

Like [VanillaBP](https://www.vanillabp.io) itself was designed to be [BPMS](https://en.wikipedia.org/wiki/Business_process_management#Definitions)-agnostic, also the *VanillaBP Business Cockpit* is able
to act as a UI for any [BPMS](https://en.wikipedia.org/wiki/Business_process_management#Definitions).
As a consequence workflows and user tasks from different systems will be available in one unifying user interface.

**Contents:**

1. [Application](#application)
   1. [Concepts in a glance](#architecture-in-a-glance)
   2. [Customized business cockpit applications](#customized-business-cockpit-applications)
   3. [Functionality provided by the business cockpit](#functionality-provided-by-the-business-cockpit)
   4. [Technologies](#technologies)
3. [Documentation](#documentation)
   1. [By use-case](#by-use-case)
   2. [By module](#by-module)
3. [Noteworthy & Contributors](#noteworthy--contributors)
4. [License](#license)

## Application

### Concepts in a glance

In companies that use business process management, agents need a single place to organize their tasks originated in
different workflows or even different workflow systems. Otherwise, employees would have to regularly check each individual application for new 
tasks or use email notifications to alert them about new tasks, which then flood the inbox. To avoid this, the 
*VanillaBP Business Cockpit* acts as *the* unifying frontend application for all workflows.

The *VanillaBP Business Cockpit* runtime is a microservice. That means it is only about providing cockpit functionality.
The workflows have to be executed by other (micro-)services (business services). Each workflow specific content
is hosted by those business services running the workflow. So, everything needed to fulfill a workflow is bundled in
a so-called "Workflow Module" within the business service. Each workflow module has its own version lifecycle
(workflow as BPMN, code to run the BPMN, UI for user-tasks, backend code to show/complete user-tasks).

However, the business cockpit UI will provide all parts (UI provided by the business cockpit, UI provided by the
workflow modules like user task forms or status-sites of workflows)
as one seamless web-application to the user. This is achieved by using
"Module Federation", a mechanism to load parts of the UI (user task forms, etc.) dynamically (not packed into the main
web-application at compile-time).

To avoid cross-site-scripting, the business cockpit backend acts as a proxy to business services. This spans
one consistent umbrella of security across the entire application. Every "Workflow Module" has to register to the
business cockpit on startup to initialize the respective proxy. 

To provide lists of user tasks and workflows any business
workflow lifecycle event or user task lifecycle event (created, completed, cancelled) has to be reported by the
"Workflow module" to the business cockpit. Reporting can be done via REST or asynchronously via Kafka and is
already implemented by ready-to-use adapters (Maven modules) for [VanillaBP](https://www.vanillabp.io) based workflows.

The *VanillaBP Business Cockpit* approach described above also implies that not every business workflow service
needs its own standalone web-application.
Individual web-applications are only necessary if the workflow application has excessive UI functionality
beyond user tasks or status-sites of workflows. Additionally, the business cockpit is a perfect place to add
cross-cutting functionality (e.g. registration of absences or vacation replacements) as well as start-forms
for business processes having no own web-application.

### Customized business cockpit applications

The *VanillaBP Business Cockpit* can be executed out-of-the-box showing a vanilla flavored design.
Typically, one wants to use own icons, colors and fonts according to the cooperate identity.
To achieve this one can build an individual Spring Boot application using the *VanillaBP Business Cockpit*
container module as a Maven-dependency. Doing so ensures the backend functionality is inherited.

For the custom UI the original frontend (web-application) can be copied as a template or replaced by your
own implementation. The *VanillaBP Business Cockpit* UI is implemented by using React. Several NPM libraries are provided to implement
a custom business cockpit UI next to user task forms provided by business services. However, one may also use other frameworks (Vue, Angular).
The [UI components provided by the *VanillaBP Business Cockpit*](#functionality-provided-by-the-business-cockpit) can be integrated as web-components.

### Functionality provided by the business cockpit

Each UI-feature provided is based on UI components next to their backend-counterparts: 

* **List(s) of user tasks**:<br>There is a default-list showing all user tasks assigned to the user logged in, assigned 
to the user's groups, claimed by the user or any dangling user task (no information about assignment given). Based 
on that, one can easily provide prefiltered lists according to any business requirement. The individual content 
to list for each user task (e.g. columns showing business data) is reported by the business service spawning the user task.
Additionally, the rendering of columns can be customized (e.g. render icons instead of values, etc.).
* **A mechanism to show user task forms**:<br>Once the user wants to work on or complete a user task
(by selecting an item in the 
list of user tasks), the form's UI is loaded from the business service through the registered proxy by using *Module Federation*.
Saving or completing the form is not part of the business cockpit but implemented by the business 
service (providing its own API). If the form can be completed the respective business service will report 
that change of status to the business cockpit which in turn results in an update of the list of user tasks.
* **List(s) of workflows**:<br>There is a default-list showing all workflows accessible to the current user, accessible
to the user's groups or any dangling workflow (no information about access). Analogous to lists of user tasks, one 
can easily provide prefiltered lists according to any business requirement. The individual content to list for 
each workflow (e.g. columns showing business data) is reported by the business service running the workflow.
Additionally, the rendering of columns can be customized (e.g. render icons instead of values, etc.).
* **A mechanism to show status-sites of workflows**:<br>Once the user wants to review the status of a workflow
(select an item in the list of workflows), the workflow's status-site UI is loaded from the business service
through the registered proxy by using *Module Federation*. Any data shown or action provided (e.g. cancel the
workflow) is not part of the business cockpit but implemented by the business service (providing its own API).
If such an action affects the status of the workflow (e.g. cancel the workflow) the respective business service
will report that change to the business cockpit which in turn results in an update of the list of workflows.
* **A mechanism to show individual workflow UI-components**:<br>The given mechanism to show user task forms or
status-sites of workflows can also be used to add individual UI-components of the business service
to the business cockpit UI. 
You may define regions in your UI (header, menu, etc.) in which those individual components are shown. This is a
possibility to provide additional sites like workflow start forms or sites providing functionality common to all
running workflows (e.g. suspend/active all workflows).

Additionally, there are features targeting the entire application:

* **Security**:<br>It is based on `Spring Security` and therefore includes
  a lot of identity providers (Active Directory, Keycloak, etc.). The business cockpit adds
  an unifying umbrella of security across all components involved to show users tasks and workflows.
  The current user logged into the business cockpit is also passed to workflow services through the
  proxy so the UI of user tasks and workflow status-sites can be adapted according to the user's roles.
* **Usertask and workflow lifecycle events**:<br>Those events are reported by individual workflow
  modules, as workflows or user tasks are created, updated, completed or cancelled. Reporting can
  be done via REST or asynchronously via Kafka which is implemented by ready-to-use adapters
  (Maven modules) for [VanillaBP](https://www.vanillabp.io) based workflows. The events reported are processed by the
  business cockpit to update
  the underlying database as well as notifying UI components for life-updates resulting
  in a state-of-the-art UX.

### Technologies

To learn about technologies used by the *VanillaBP Business Cockpit* checkout the
[respective section](./container#technologies) of the *container module*
(see [Documentation by module](#by-module)).

## Documentation

### By use-case

* Running the *VanillaBP Business Cockpit*: [container/README.md](./container/README.md)
* Building a customized business cockpit: [container/README.md](./container/README.md)
* Use out-of-the-box adapters for filling the business cockpit with data of your business processes:
[spi-for-java/README.md](./spi-for-java) and [adapters/README.md](./adapters/README.md)
* Build user task forms** to be shown in the business cockpit: []
* Build workflow status sites** to be shown in the business cockpit: []
* Integrate business service not using out-of-the-box adapters: [development/simulator/README.md](./development/simulator/README.md)
* Contributing to the *VanillaBP Business Cockpit*: [development/README.md](./development/README.md)

### By module

in logical order:

1. **[spi-for-java](./spi-for-java)**:<br>The *service provider interface* for workflow modules to be used in
   [VanillaBP workflow services](https://github.com/vanillabp/spi-for-java#wire-up-a-process)
   for customizing data reported as part of workflow or user task lifecycle events (e.g. adding business data).
1. **[adapters](./adapters)**:<br>Business processing system (BPS) adapters in the meaning of hexagonal architecture implementing *spi-for-java*.
   1. **[commons](./adapters/commons)**:<br>Functionality common to more than one specific BPS adapter.
   1. **[camunda7](./adapters/camunda7)**:<br>Adapter to be used in workflow modules leveraging the BPS [Camunda 7](http://www.camunda.org).
   1. **[camunda8](./adapters/camunda8)**:<br>Adapter to be used in workflow modules leveraging the BPS [Camunda 8](http://www.camunda.io).
1. **container**:<br>The business cockpit microservice. Use this as a Maven-dependency for a custom business cockpit.
   The included React application can be used out-of-the-box or as a template.
1. **ui**:<br>NPM libraries for custom UIs.
    1. **bc-shared**:<br>Components and Typescript types used by the business cockpit UI and also by workflow module
       UI components (user task form, workflow status-site).
    1. **bc-ui**:<br>Components and Typescript types for building individual business cockpit UIs.
1. **common**:<br>Spring Boot based functionality used by `container` but may also be used by
   workflow modules or other individual services.
1. **apis**:<br>Ready-to-use clients and servers for various APIs in the context of the *VanillaBP Business Cockpit*.
   1. **bpms-api**:<br>API for reporting workflow and user task lifecycle events via REST or Kafka.
   1. **official-gui-api**:<br>REST-API expected by business cockpit UI components to retrieve data from.
   1. **workflow-provider-api**:<br>REST-APIs workflow modules may implement to customize business cockpit behavior.
1. **development**:<br>Functionality to support developing workflow modules or the *VanillaBP Business Cockpit* itself.
   1. **dev-shell-react**:<br>A NPM library for easy developing user task forms and workflow status-sites using *React*.
   1. **dev-shell-angular**:<br>A NPM library for easy developing user task forms and workflow status-sites using *Angular*.
   1. **simulator**:<br>A standalone microservice used for business cockpit development, which mimics a workflow module.
   1. **docker-compose.yaml**:<br>Preconfigured docker containers for business cockpit development.

## Noteworthy & Contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back to the community as it has benefited the community in the past.

![Phactum](./readme/phactum.png)

## License

Copyright 2024 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0

## Changelog

### 0.0.4-SNAPSHOT

* Introduce person & group
    * user task.assignee, user task.candidateUsers, workflow.initiator and workflow.accessibleToUsers type change from string to Person
    * user task.canidateGroups, workflow.accessibleToGroups type change to Group
    * current-user type change to a combination of Person and Groups
    * A PersonAndGroupMapper is introduced to provide a mapping between user provided by APIs and the values stored in the database
    * A PersonAndGroupApiMapper is introduced to provide a mapping between data stored in the database and the details shown in the UI
* Introduce user task-list and workflow-list column's type
    * `i18n`: An sub-field for each supported locale (language) is expected
    * `person`: A person-compatible object has to be provided
    * `date`: A Date object which will be rendered as a date
    * `date-time`: A Date object which will be rendered as a timestamp
* Define explicit colors in `ui/bc-shared/src/theme/index.ts`
* React-Dev-Shell: Changed parameters of DevShell component
* fetch-API: change from dispatch- to toast-function
* Add custom federated components to DevShell
* Sort-indexes will be dropped and recreated on demand due to naming