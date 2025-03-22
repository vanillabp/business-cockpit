[![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](./LICENSE)

![VanillaBP](../readme/vanillabp-headline.png)

# The Microservice

This module is about the runtime of the Business Cockpit, either the backend as well as
the main frontend application.

The backend is a Spring Boot application. The given frontend is a React application,
but it can also be replaced by other frameworks easily. The bundled web-application
is hosted by the Spring Boot web-server, so no additional Node-js services is needed.

## Ways to use it

1. **As-is**:<br>One can run the VanillaBP Business Cockpit by using the container-JAR
   which includes the *React* based UI. [More...](#as-is)
2. **Custom UI**:<br>The container-JAR is used as a *Maven*-dependency
   to inherit standard backend functionality for a custom *Spring Boot*
   application. [More...](#custom-ui)
    1. The provided React-webapp can be used as a template for an individual webapp.
    2. As an alternative, also an Angular based webapp can be used. The Business Cockpit UI components
       can be used as web-components.

### As-is

1. **MongoDB**:
    1. Setup a MongoDB having a `ReplicaSet` configured. A `ReplicaSet` is needed
       to enable MongoDB's change-stream used to retrieve DB updates which are the origin of
       automatic UI updates.
    1. For local testing and development the given [docker-compose.yaml](../development/docker-compose.yaml)
       can be used:
       ```sh
       cd development
       docker-compose up -d business-cockpit-mongo-setup
       ```
       Add `127.0.0.1 business-cockpit-mongo`
       to your local hosts file to make MongoDB accessible
       (Unix: `/etc/hosts`, Window: `c:\Windows\System32\Drivers\etc\hosts`). <br> <br>

       *Hint*: If you encounter the error `/usr/bin/env: 'bash\r': No such file or directory` in Docker, it might be due to incorrect line endings in the script file. To resolve this: <br>

       Remove and then clone the repository again but with the following Git configuration:
          ```powershell
          git clone [url] --config core.autocrlf=input
          ```
       Alternatively you can try disabling automatic line ending conversion with Git by running:
          ```powershell
          git config --global core.autocrlf false
          ```
1. **Download container-JAR**:<br>...from *Maven*-Central or the latest
   [snapshot](https://github.com/vanillabp/business-cockpit/packages/1956012) and name it `bc.jar`.
1. **Provide custom Spring Boot configuration**:<br>...by creating `application.yaml`:
   ```yaml
   spring:
     data: 
       mongodb:
         # Add next line with adopted values in case of using a MongoDB not created by 'docker-compose.yaml'
         uri: mongodb://XXXXXXX:27017/XXXXXXX
   business-cockpit:
     title-short: MyBC
     title-long: My Business Cockpit
     application-version: "1.0"
   ```
1. **Run VanillaBP Business Cockpit**:<br>...using this command `java -jar bc.jar`
1. **Open Browser to test the Business Cockpit**:<br>Using the URL `http://localhost:8080/` will
   prompt you for basic authentication. Confirm using username `test` and password `test`.
   Afterwards the VanillaBP Business Cockpit is loaded showing empty lists.
1. **Provide workflow and usertask data**:<br>To show workflows and usertask one has to connect
   a workflow module. See the respective docs how this is done.

### Custom UI

1. **MongoDB**:
    1. Setup a MongoDB having a `ReplicaSet` configured. A `ReplicaSet` is needed
       to enable MongoDB's change-stream used to retrieve DB updates which are the origin of
       automatic UI updates.
    1. For local testing and development the given [docker-compose.yaml](../development/docker-compose.yaml)
       can be used:
       ```sh
       cd development
       docker-compose up -d business-cockpit-mongo-setup
       ```
       Add `127.0.0.1 business-cockpit-mongo`
       to your local hosts file to make MongoDB accessible
       (Unix: `/etc/hosts`, Window: `c:\Windows\System32\Drivers\etc\hosts`).
1. **Create a blank Spring Boot application**:<br>For example by using [Spring initializr](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.3.3&packaging=jar&jvmVersion=17&groupId=com.example&artifactId=demo&name=demo&description=Demo%20project%20for%20Spring%20Boot&packageName=com.example.demo&dependencies=).
1. **Add VanillaBP Business Cockpit functionality**:<br>
    1. Add *Maven*-dependency:
       ```xml
       <dependency>
          <groupId>io.vanillabp.businesscockpit</groupId>
          <artifactId>container</artifactId>
       </dependency>
       ```
    1. Change `DemoApplication` like this:
       ```java
       public class DemoApplication extends io.vanillabp.cockpit.BusinessCockpitApplication {
       ```
    1. Add VanillaBP Business Cockpit webapp:
       ```shell
       cd demo/src/main
       
       cp my-git-folder/business-cp
       ```

## Architecture

### Spring Boot

The Business Cockpit backend application is written in reactive, non-blocking style since some
parts of Spring Boot are solely provided in that style and not in the classic, blocking style anymore.
Independently, any business service may use classic, blocking style.

### Database

Any data reported to the Business Cockpit (workflow modules, workflows, usertasks) are stored in
a NoSQL-database. The reason for using NoSQL is that [workflow modules](../README.md#architecture-in-a-glance)
may report individual business data which has to be rendered as part of the usertask list or
the workflow list. Additionally, the lists may be filtered and sorted by this business data
using indexes on data not know a priori. This scenario is what NoSQL-database are made for.

The Business Cockpit uses the [MongoDB](https://www.mongodb.com/docs/manual/administration/install-community/) (>= 4.4.) as a datastore. Additionally, any compatible
database can be used. Currently, [Azure Cosmos DB for MongoDB](https://learn.microsoft.com/en-us/azure/cosmos-db/mongodb/introduction) works as well. Other candidates
are [Oracle Database API for MongoDB](https://docs.oracle.com/en/database/oracle/mongodb-api/) and [FerretDB](https://www.ferretdb.com/).

## Security

The Business Cockpit is a distributed application: the Business Cockpit itself and the
workflow modules providing usertask forms and status-sites of workflows. Each component needs to be
aware of the current user as well as the user's roles.

To achieve this, after successful login a JWT based authentication is used. The token is passed to
the browser as a session-cookie to ensure users are not forced to login several times on hitting link
from different origins (browser, mail, bookmark, etc.). The token consists primarily of the user-id
but no business roles to keep the token short. On validation of the token the user's business roles
will be added to the Spring security-context on-the-fly. Additionally, one may use role-mappings to
separate your software's business roles from those provided by the identity provider
(e.g. Keycloak, Active Directory).

On accessing workflow modules as part of usertask forms via the Business Cockpit as a proxy
(see [architecture](../README.md#application)) the JWT is also passed to the respective
workflow module handing over the current user's security-context. Doing so, each workflow module
may introduce individual business roles mappings.

### Using an OIDC provider for authentication

For demo purposes the VanillaBP Business Cockpit is protected using BASIC authentication.
This can be changed by providing an individual bean named `guiHttpSecurity`:

```java
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityWebFilterChain guiHttpSecurity(
            final JwtServerSecurityContextRepository securityContextRepository,
            final ServerHttpSecurity http) {

        http
                /* add here any security chain config like CORS, CRSF, etc. */
                .anonymous(ServerHttpSecurity.AnonymousSpec::disable)
                .authorizeExchange(authorizeExchangeSpec -> {
                    // allow access to "unprotected" URLs 
                    authorizeExchangeSpec
                            .matchers(io.vanillabp.cockpit.config.web.WebSecurityConfiguration.appInfoWebExchangeMatcher,
                                    io.vanillabp.cockpit.config.web.WebSecurityConfiguration.currentUserWebExchangeMatcher,
                                    io.vanillabp.cockpit.config.web.WebSecurityConfiguration.assetsWebExchangeMatcher,
                                    io.vanillabp.cockpit.config.web.WebSecurityConfiguration.staticWebExchangeMatcher,
                                    io.vanillabp.cockpit.config.web.WebSecurityConfiguration.workflowModulesProxyWebExchangeMatcher)
                            .permitAll()
                            .anyExchange()
                            .authenticated();
                })
                .requestCache(spec -> spec.requestCache(new CookieServerRequestCache()))
                .oauth2Login(oAuth2LoginSpec -> {
                    oAuth2LoginSpec
                            .securityContextRepository(securityContextRepository);
                })
                .logout(logout -> { /* add here any logout functionality specific to your environment */ })
                // add the JWT security filter for future requests
                .addFilterAfter(jwtSecurityFilter(securityContextRepository), SecurityWebFiltersOrder.REACTOR_CONTEXT);
            
        return http.build();
            
    }
```
