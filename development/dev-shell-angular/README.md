# DevShell

The DevShell library for Angular is not that easy to develop because `npm link` is not supported for Angular libraries.

Therefor one has to deploy every change to the local NPM registry (Verdaccio) und update the node-module in the workflow module it is used.
In case of developing VanillaBP this means to update the `simulator` which mimics a workflow module.

1. In `development/dev-shell-angular`:
   ```sh
   mvn -U -Dnpm.registry=http://localhost:4873 install -P unpublish-npm
   ```
2. In `development/simulator/src/main/webapp-angular`:
   ```sh
   cd ../../..;  mvn -U -Dnpm.registry=http://localhost:4873 install -P unpublish-npm ; cd -; rm -fR .angular/cache; npm start
   ```
