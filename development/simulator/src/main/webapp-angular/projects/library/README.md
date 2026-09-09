# library

The Angular library of the simulator's Angular user interface: the components the federated module
exposes, kept apart from the application shell around them the way a workflow module keeps them.

```sh
ng build library     # into dist/
ng test library      # unit tests through Karma
```

It is built by the Maven module above it rather than with the Angular CLI directly, and it is not
published anywhere: it exists to exercise [dev-shell-angular](../../../../../../dev-shell-angular).
