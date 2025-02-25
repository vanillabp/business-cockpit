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

## Running the Dev-Shell Simulator

1. Download runnable JAR from Maven-Central or the latest [snapshot](https://github.com/orgs/vanillabp/packages?q=dev&tab=packages&q=dev-shell-simulator).
1. Run the JAR
   ```shell
   java -jar dev-shell-simulator-*-runnable.jar
   ```
1. Configure your application to use this configuration
   for local development:
   ```yaml
   vanillabp:
    cockpit:
        rest:
            log: true
            base-url: http://localhost:9080/bpms/api/v1
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
           target: 'http://localhost:9080',
           secure: false,
           changeOrigin: true,
         },
         '/gui/api': {
           target: 'http://0.0.0.0:9080',
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

