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

Maven does not build or publish this package any more, see decision 62 in the repository's
[DECISIONS.md](../../DECISIONS.md). To try a change in another package, publish it by hand to the
local NPM registry which [development/README.md](../../development/README.md#local-npm-registry) sets up:
`npm run publish:snapshot -- --@vanillabp:registry=http://localhost:4873`.

Anything a workflow module needs belongs here. Anything only a cockpit user interface needs belongs
in [bc-ui](../bc-ui), because a workflow module should not have to install a cockpit to render a
form.
