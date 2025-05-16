![](../../readme/vanillabp-headline.png)

# Dev-Shell Simulator

## Overview

The **dev-shell-simulator** is a tool designed
to provide data to the **VanillaBP Business Cockpit DevShell**,
a web application. The DevShell is used for
developing workflow modules UIs. It allows developers
to interact with workflows and user tasks without running
the VanillaBP Business Cockpit.

When an application starts a workflow or a
user task is spawned, it is
reported to the Business Cockpit. When a 
workflow module's UI has to render a user task
this data previously reported is loaded
from the Business Cockpit. Both features,
retrieving and providing data, which are
fulfilled by the Business Cockpit, are also
fulfilled by the DevShell Simulator at local
development.

Additionally, the *Dev-Shell Simulator* acts as a user provider for the
*Dev-Shell* UI. When designing UIs of user tasks or workflow pages
it might be important to adopt the data shown according to the permissions
of the user currently logged in. Also, APIs used by user tasks or 
workflow pages may be protected to certain user roles. When using the *Dev-Shell*
for local development, a user can be chosen by using the drop-down on the
*Dev-Shell*'s main page. For local development those users are loaded from an API
exposed by the *Dev-Shell Simulator*. Learn in the next section how to define
those users.

## Running the Dev-Shell Simulator

1. Download runnable JAR from
   [Github](https://github.com/vanillabp/business-cockpit/releases/tag/0.0.7)
   or the latest [snapshot](https://github.com/orgs/vanillabp/packages?q=dev&tab=packages&q=dev-shell-simulator).
1. Prepare a configuration file `users.yaml` defining users and their groups (authorities):
   ```yaml
   dev-shell-simulator:
     users:
         - id: john
           email: john@doe.com
           first-name: John
           last-name: Doe
           groups: ADMIN, RISK_ASSESSMENT
           attributes:
             office365-uuid: 7bda0fe7-881e-4aa3-b0ee-258cddb71711
         - id: jane
           email: jane@doe.com
           first-name: Jane
           last-name: Doe
           groups: ADMIN
           attributes:
             office365-uuid: f9107b18-8fdf-40c1-aac4-531054b5bd5c
         - id: joe
           email: joe@doe.com
           first-name: Joe
           last-name: Doe
           groups: RISK_ASSESSMENT
           attributes:
             office365-uuid: 8b778542-096b-4f07-93f4-574466d15711
      ```
1. Run the JAR
   ```shell
   java -jar dev-shell-simulator-*-runnable.jar --spring.config.additional-location=./users.yaml
   ```
1. Configure your application to use this configuration
   for local development:
   ```yaml
   vanillabp:
    cockpit:
        rest:
            log: true
            base-url: http://localhost:8079/bpms/api/v1
            authentication:
                basic: true
                username: abc
                password: 123
   ```
1. Configure your application webapp dev-server as a proxy
   for these pathes used by the DevShell:
   ```javascript
   module.exports = {
     devServer: {
       proxy: {
         '/official-api': {
           target: 'http://localhost:8079',
           secure: false,
           changeOrigin: true,
         },
         '/gui/api': {
           target: 'http://0.0.0.0:8079',
           secure: false,
           changeOrigin: true,
         },
       },
     },
   };
   ```

## Noteworthy & Contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back to the community as it
has benefited the community in the past.\
![Phactum](../../readme/phactum.png)

## License

Copyright 2025 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0

