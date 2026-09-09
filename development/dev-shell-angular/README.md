# Dev shell for Angular

The Angular counterpart of [dev-shell-react](../dev-shell-react), published as
`@vanillabp/bc-dev-shell-angular-module`. How a workflow module uses it is in the wiki, under
[Developing UI components locally](https://github.com/vanillabp/business-cockpit/wiki/Developing-UI-components-locally#angular).

Working on the library itself is more work than on the React one, because Angular libraries do not
support `npm link`. Every change has to be published to the local NPM registry and pulled into the
application using it again, which here is the [simulator](../simulator), standing in for a workflow
module.

1. In `development/dev-shell-angular`:
   ```sh
   mvn -U -Dnpm.registry=http://localhost:4873 install -P unpublish-npm
   ```
2. In `development/simulator/src/main/webapp-angular`:
   ```sh
   cd ../../..;  mvn -U -Dnpm.registry=http://localhost:4873 install -P unpublish-npm ; cd -; rm -fR .angular/cache; npm start
   ```
