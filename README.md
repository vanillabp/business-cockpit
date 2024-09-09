[![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](./LICENSE)

![VanillaBP](./readme/vanillabp-headline.png)

# Business Cockpit

The *VanillaBP Business Cockpit* is a web-application for business people to work with business processes. It is about:

1. Process instances (= workflows currently executed)
     1. Search for workflows by business data
     1. View active and retired workflows
     1. Show business aspects and provide actions
1. User tasks (= items of work of a workflow to be done by humans)
     1. Search for user tasks by business data
     1. List active and retired user tasks
     1. Fulfill user tasks

Like [VanillaBP](https://www.vanillabp.io) itself was designed to be [BPMS](https://en.wikipedia.org/wiki/Business_process_management)-agnostic, also the *VanillaBP Business Cockpit* is able
to act as a UI for any BPMS. This also makes it possible to display workflows and user tasks from different systems
in one unifying user interface.

**Contents:**

1. [Application](#application)
   1. [Architecture in a glance](#architecture-in-a-glance)
   2. [Customized Business Cockpit applications](#customized-business-cockpit-applications)
3. [Documentation](#documentation)
   1. [By use-case](#by-use-case)
   2. [By module](#by-module)

## Application

### Architecture in a glance

In companies that use business process management, agents need a single place to organize their tasks originated in
different workflows or even different workflow systems. Otherwise, employees would have to regularly check each individual application for new 
tasks or use email notifications to alert them about new tasks, which then flood the inbox. To avoid this, the 
Business Cockpit acts as the unifying frontend application for all workflows.

The Business Cockpit runtime is a microservice. That means it is only about providing cockpit functionality.
The workflows have to be executed by other (micro-)services (business services). Each workflow specific content
is hosted by those business services running the workflow. So, everything needed to fulfill a workflow is bundled in
a so-called "Workflow Module" within the business service. Each workflow module has its own version lifecycle
(workflow as BPMN, code to run the BPMN, UI for user-tasks, backend code show/complete user-tasks).

However, the Business Cockpit UI will provide all parts (UI provided by the Business Cockpit, UI provided by the
workflow modules as usertask forms or status-sites of workflows)
as one seamless web-application to the user without using cross-site-scripting. This is achieved by using
"Module Federation", a mechanism to load parts of the UI (usertask forms, etc.) dynamically (not packed into the main
web-application at compile-time).

To avoid cross-site-scripting, the Business Cockpit backend acts as a proxy to business microservices. This spawns
one consistent umbrella of security across the entire application. Any "Workflow Module" has to register to the
Business Cockpit on startup to initialize the respective proxy. Any business
workflow event or usertask lifecycle event (created, completed, cancelled) has to be reported by the "Workflow module" 
to the Business Cockpit as well. Reporting can be done via REST or asynchronously via Kafka which is implemented
by ready-to-use adapters (Maven modules).

This architecture also implies that not every business workflow microservice needs its own standalone web-application.
Individual web-applications are only necessary if the workflow application has excessive UI functionality
beyond usertasks or status-sites of workflows. Additionally, the Business Cockpit is a perfect place to add
cross-cutting functionality (e.g. registration of absences or vacation replacements) as well as start-forms
for business processes having no own web-application.

### Customized Business Cockpit applications

The Business Cockpit can be executed out-of-the-box showing a vanilla flavored design. Typically, one wants to use own
icons, colors and fonts. To achieve this one can build an individual Spring Boot application using the Business Cockpit
container module as a Maven-dependency. Doing so opens the way to build your own Business Cockpit UI based on
the provided functionality.

The VanillaBP Business Cockpit UI is implemented by using React. Several NPM libraries are provided to implement
a custom business cockpit UI next to usertask forms provided by business services. However, one may also use other frameworks (Vue, Angular).
The [UI components provided by the Business Cockpit](#functionality-provided-by-the-business-cockpit) can be integrated as web-components.

### Functionality provided by the Business Cockpit

Each UI-feature provided is based on UI components next their backend-counterparts: 

* **List(s) of usertasks**:<br>There is a default-list showing all usertasks assigned to the user logged in, assigned 
to the user's groups, claimed by the user or any dangling usertask (no information about assignment given). Based 
on that, one can easily provide prefiltered lists according to any business requirement. The individual content 
to list for each usertask (e.g. columns showing business data) is reported by the business microservice spawning the usertask.
Additionally, the rendering of columns can be customized (e.g. render icons instead of values, etc.).
* **A mechanism to show usertask forms**:<br>Once the user wants to work on or complete a usertask (by selecting an item of the 
list of usertasks), the form's UI is loaded from the business microservice through the registered proxy by using *Module Federation*.
Saving or completing the form is not part of the Business Cockpit but implemented by the business 
microservice (providing its own API). If the form can be completed the respective business microservice will report 
that change of status to the Business Cockpit which in turn results in an update of the list of usertasks.
* **List(s) of workflows**:<br>There is a default-list showing all workflows accessible to the current user, accessible
to the user's groups or any dangling workflow (no information about access). Analogous to lists of usertasks, one 
can easily provide prefiltered lists according to any business requirement. The individual content to list for 
each workflow (e.g. columns showing business data) is reported by the business microservice running the workflow.
Additionally, the rendering of columns can be customized (e.g. render icons instead of values, etc.).
* **A mechanism to show status-sites of workflows**:<br>Once the user wants to review the status of the workflow
(select an item of the list of workflows), the workflow's status-site UI is loaded from the business microservice
through the registered proxy by using *Module Federation*. Any data shown or action provided (e.g. cancel the
workflow) is not part of the Business Cockpit but implemented by the business microservice (providing its own API).
* **A mechanism to show individual workflow UI-components**:<br>The given mechanism to show usertask forms or
status-sites of workflows can also be used to add individual UI-components to the Business Cockpit UI. 
You may define regions in your UI (header, menu, etc.) in which those individual components are shown. This is a
possibility to provide additional sites like workflow start forms or sites providing functionality common to all
running workflows (e.g. suspend/active all workflows).

Additionally, there are features targeting the entire application:

* **Security**:<br>It is based on `Spring Security` and therefore includes
  a lot of identity providers (Active Directory, Keycloak, etc.). The Business Cockpit adds
  a unifying umbrella of security across all components involved to show users tasks and workflows.
  The current user logged into the Business Cockpit is also passed to workflow services
  so UX can be adapted according the user's roles.
* **Usertask and workflow lifecycle events**:<br>Those events are reported by individual workflow
  modules, as workflows or usertasks are created, updated, completed or cancelled. Reporting can
  be done via REST or asynchronously via Kafka which is implemented by ready-to-use adapters
  (Maven modules). The events reported are processed by the Business Cockpit to update
  the underlying database (MongoDB) as well as notifying UI components for a better UX.

## Documentation

### By use-case

* **Building a customized Business Cockpit:** [container/README.md](./container/README.md)
* **Integrate your business service:** [development/simulator/README.md](./development/simulator/README.md)
* **Contributing to the VanillaBP Business Cockpit:** [development/README.md](./development/README.md)

### By module

in logical order:

1. **container**:<br>The Business Cockpit microservice. Use this as a Maven-dependency for a custom Business Cockpit.
   The included React application can be used out-of-the-box or as a template.
2. **ui**:<br>NPM libraries for custom UIs.
   1. **bc-shared**:<br>Components and Typescript types used by the Business Cockpit UI and also by workflow module
      UI components (usertask form, workflow status-site).
   2. **bc-ui**:<br>Components and Typescript types for building individual Business Cockpit UIs.
3. **spi-for-java**:<br>The *service provider interface* for workflow modules to be used in
   [VanillaBP workflow services](https://github.com/vanillabp/spi-for-java#wire-up-a-process)
   for customizing data reported as part of workflow or usertask lifecycle events.
4. **adapters**:<br>Business processing system (BPS) adapters in the meaning of hexagonal architecture implementing *spi-for-java*.
   1. **commons**:<br>Functionality common to more than one specific BPS adapter.
   2. **camunda7**:<br>Adapter to be used in workflow modules based on the BPS [Camunda 7](http://www.camunda.org).
   2. **camunda8**:<br>Adapter to be used in workflow modules based on the BPS [Camunda 8](http://www.camunda.io).
5. **common**:<br>Spring Boot based functionality used by `container` but may also be used by
   workflow modules or other individual services.
6. **apis**:<br>Ready-to-use clients and servers for various APIs in the context of the VanillaBP Business Cockpit.
   1. **bpms-api**:<br>API for reporting workflow and usertask lifecycle events via REST or Kafka.
   2. **official-gui-api**:<br>REST-API expected by Business Cockpit UI components to retrieve data from.
   3. **workflow-provider-api**:<br>REST-APIs workflow modules may implement to customize Business Cockpit behavior.
4. **development**:<br>Functionality supporting developing workflow modules or the Business Cockpit itself.
   1. **dev-shell-react**:<br>A NPM library for easy developing usertask forms and workflow status-sites using *React*.
   2. **dev-shell-angular**:<br>A NPM library for easy developing usertask forms and workflow status-sites using *Angular*.
   3. **simulator**:<br>A standalone microservice used for Business Cockpit development, which mimics a workflow module.
   4. **docker-compose.yaml**:<br>Preconfigured docker containers for Business Cockpit development.

## Noteworthy & Contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back to the community as it has benefited the community in the past.

![Phactum](./readme/phactum.png)

## License

Copyright 2024 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0

## Changelog

### 0.0.4-SNAPSHOT

* Introduce person & group
    * usertask.assignee, usertask.candidateUsers, workflow.initiator and workflow.accessibleToUsers type change from string to Person
    * usertask.canidateGroups, workflow.accessibleToGroups type change to Group
    * current-user type change to a combination of Person and Groups
    * A PersonAndGroupMapper is introduced to provide a mapping between user provided by APIs and the values stored in the database
    * A PersonAndGroupApiMapper is introduced to provide a mapping between data stored in the database and the details shown in the UI
* Introduce usertask-list and workflow-list column's type
    * `i18n`: An sub-field for each supported locale (language) is expected
    * `person`: A person-compatible object has to be provided
    * `date`: A Date object which will be rendered as a date
    * `date-time`: A Date object which will be rendered as a timestamp
* Define explicit colors in `ui/bc-shared/src/theme/index.ts`
* React-Dev-Shell: Changed parameters of DevShell component
* fetch-API: change from dispatch- to toast-function
* Add custom federated components to DevShell
* Sort-indexes will be dropped and recreated on demand due to naming