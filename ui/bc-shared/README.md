# bc-shared

The components and TypeScript types a workflow module's user interface and the cockpit both use,
published as `@vanillabp/bc-shared`. `UserTaskForm`, `WorkflowPage`, `UserTaskListCell` and the
column types a federated module is written against come from here.

How a workflow module uses them is in the wiki, under
[User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites),
including the Webpack aliases a consuming build needs. This file is about the package.

```sh
npm run build      # once
npm start          # the same, in watch mode
npm run storybook  # the components on their own
```

A reactor build publishes this package to the NPM registry given by `-Dnpm.registry`, which
[development/README.md](../../development/README.md#local-npm-registry) sets up for local work.
Because a first build links the packages of this repository, a change here is picked up by the
cockpit's user interface and by the simulator without publishing anything again.

Anything a workflow module needs belongs here. Anything only a cockpit user interface needs belongs
in [bc-ui](../bc-ui), because a workflow module should not have to install a cockpit to render a
form.
