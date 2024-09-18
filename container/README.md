[![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](./LICENSE)

![VanillaBP](../readme/vanillabp-headline.png)

# The Microservice

The module is about the runtime of the Business Cockpit, either the backend as well as the main frontend application.

The backend is a Spring Boot application. The given frontend is a React application but it can also be replaced by other frameworks easily. The bundled web-application is hosted by the Spring Boot web-server, no additionaly Node-js services is needed.

## Ways to use it

1. *As-is:* One can run the VanillaBP
1. *Adopted:*
1. *Custom Design:*
1. *Custom UI:*

## Architecture

### Spring Boot

The Business Cockpit backend application is written in reactive, non-blocking style since some parts of Spring Boot are solely provided in that style and not in the classic, blocking style anymore. Independently, any business mircroservice may use classic, blocking style.

### Database

Any data reported to the Business Cockpit (workflow modules, usertasks, workflows) are stored in a NoSQL-database. The reason for using NoSQL is that business microservices may report individual business data which has to be rendered as part of the usertask list or the workflow list. Additionally, the lists may be filtered and sorted by this business data using indexes on data not know a priori. This is what NoSQL-database are made for.

The Business Cockpit uses the [MongoDB](https://www.mongodb.com/docs/manual/administration/install-community/) (>= 4.4.) as a datastore. Additionally, any compatible database can be used. Currently [Azure Cosmos DB for MongoDB](https://learn.microsoft.com/en-us/azure/cosmos-db/mongodb/introduction) works as well. Other candidates are [Oracle Database API for MongoDB](https://docs.oracle.com/en/database/oracle/mongodb-api/) and [FerretDB](https://www.ferretdb.com/).

## Security

The Business Cockpit is a distributed application. There is the Business Cockpit itself and the business microservices providing usertask forms and status-sites of workflows. Each component needs to be aware of the current user as well as the user's roles.

To achieve this, after successful login a JWT based authentication is used. The token is passed to the browser as a session-cookie to ensure users are not forced to login several times on hitting link from different origins (browser, mail, bookmark, etc.). The token consists primarily of the user-id but no business roles to keep the token short. On validation of the token the user's business roles will be added to the Spring security-context on-the-fly. Additionally, one may use role-mappings to separate your software's business roles from those provided by the identity provider (e.g. Keycloak, Active Directory).

On accessing business microservices as part of usertask forms via the Business Cockpit as a proxy (see [architecture](../README.md#conceptional)) the JWT is also passed to the respective business microservice providing the current user's security-context. Doing so, each business microservice may introduce individual business roles mappings. 

### Using an OIDC provider for authentication

For demo purposes the VanillaBP Business Cockpit is protected using BASIC authentication. This can be changed by providing an individual bean named `guiHttpSecurity`:

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

