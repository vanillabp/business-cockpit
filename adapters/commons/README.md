![](../../readme/vanillabp-headline.png)

# Common adapter functionality

**Contents:**
1. [Configuration of event propagation](#configuration-of-event-propagation)
1. [Using templates for event details](#using-templates-for-event-details) 
1. [Spring Security in workflow modules](#spring-security-in-workflow-modules) 

## Configuration of event propagation

Based on [BPMS events](../spi-for-java#using-an-adapter) or
[application events](../spi-for-java#retrieve-user-task-details-programmatically) the *VanillaBP Business Cockpit*
needs to receive updates of the business data shown to the user.

For reporting of those update events two ways are supported out-of-the-box:
1. **REST:** The [business-cockpit REST API](../apis/bpms-api) is used for event transmission.
2. **Kafka:** Events are encoded using Protobuf and then sent via a Kafka broker.

*Hint:* Besides user task events and workflow events also each workflow module itself is reported
to the *VanillaBP Business Cockpit*. This is to update common parts of the user task application
which may offer functionality provided by the workflow module besides user tasks and workflow status sites
(e.g. a start-button to create new workflows).

### REST

To enable REST based event propagation the *VanillaBP Business Cockpit* has to
[enable the REST-API](../container#rest-api). The runtime container of the workflow module
has to reflect the same property values in the `application.yaml`:

```yaml
vanillabp:
  cockpit:
    rest:  
      base-url: <business cockpit url>  
      authentication:  
        basic: true  
        username: <username>
        password: <password>
```

### Kafka

If you want to propagate events via a Kafka broker, your workflow module's runtime container
must add the spring-kafka dependency and additionally have topic names for both user-task and
workflow events set. These must be equal to the Kafka topic names in your business-cockpit configuration.

```yaml
vanillabp:  
  cockpit:  
    kafka:  
      user-task-topic: <user task events>  
      workflow-topic: <workflow events>
      workflow-module-topic: <workflow module events>
```

All other connection properties for Kafka can be set according to the
[Kafka Spring Boot autoconfiguration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#appendix.application-properties.integration).

Additionally, these Maven dependencies have to be added:

```xml
<dependency>
    <groupId>io.vanillabp.businesscockpit</groupId>
    <artifactId>bpms-protobuf-api</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### Disabling event propagation

If you customized user task application does not provide a list of workflows to the user then you
can disable event propagation:

```yaml
vanillabp:  
  cockpit:
    user-tasks-enabled: true
    workflow-list-enabled: false
```

As a default all events are propagated.

## Using templates for event details

As mentioned in the SPI documentation for [user task details](../spi-for-java#using-templates-for-user-task-details)
and [workflow details](../spi-for-java#using-templates-for-workflow-details) templates may be used
instead of string values. A template-loader path has to be set. This is the directory where Freemarker
templates are loaded from:

```yaml
vanillabp:  
  cockpit:
    template-loader-path: classpath*:cockpit-templates/
```

If you have separate Maven modules for your workflow modules then it is mandatory to use the asterisk
character to tell Spring Boot to also scan depended JARs (the workflow modules) for files.

In each workflow module one has to provide Freemarker templates in a sub-directory to avoid name clashes.
Additionally, another sub-directory for each workflow part of the workflow module has to be defined.
As a default the workflow module ID as well as the BPMN process ID is assumed as directory names.
Also for user tasks a sub-directory is assumed and defaulted to the user tasks's definition. If
custom names are used one may override the names for a workflow module and/or a workflow like this:

```yaml
vanillabp:  
  workflow-modules:
    cockpit:
      template-path: workflow-module-a
    workflows:
      workflow-a1:
        cockpit:
          template-path: workflow-a1
        user-tasks:
          retrievePayment:
            cockpit:
              template-path: retrieve-payment
```

This results for workflow details of `workflow-a1` in a template-location
`classpath*:cockpit-templates/workflow-module-a/workflow-a1` and for user task details
of `retrievePayment` in `classpath*:cockpit-templates/workflow-module-a/workflow-a1/retrieve-payment`.

*Hint:* In case workflow modules are in separate Maven modules keep in mind that the
`template-loader-path` is a global configuration to be placed in the `application.yaml` of the
Spring Boot runtime container but the `template-path` property belongs to a specific
workflow module/workflow and therefore has to be placed in the
[respective module's properties](https://github.com/vanillabp/spring-boot-support?tab=readme-ov-file#configuration).

# Spring Security in workflow modules

As explained in detail in the security chapter of the [container module](../../container)
a JWT based authentication is used by the *VanillaBP Business Cockpit* web-application. Additionally,
for each workflow-module the *VanillaBP Business Cockpit* container acts as a proxy to
span an unifying umbrella of security across the business cockpit and the workflow module microservices.

So, any request originated by the business cockpit web-application hitting the workflow module
is secured by a JWT authentication. The JWT created by the *VanillaBP Business Cockpit*
during user login and is also passed as a cookie to workflow module microservice. By processing
the JWT cookie the information about the user currently using the *VanillaBP Business Cockpit*
is also available in the workflow module application.

To achieve this the JWT has to be validated based on a shared secret. This secret is created as
part of bootstrapping the *VanillaBP Business Cockpit* and needs to be passed to the workflow
module's configuration:

```yaml
vanillabp:
  cockpit:
    jwt:
      hmacSHA256-base64: oNr5wqOE2LBh5uKKVfTwubPfyF237OrffQj5bcMKtpw=  # sample secret
```

*Hint:* This is a global configuration and has to be placed in the `application.yaml` of the
Spring Boot runtime container of the workflow module.

If there is a special cookie configuration set in the *VanillaBP Business Cockpit* then
it has to be copied as well:

```yaml
vanillabp:
  cockpit:
    jwt:
      hmacSHA256-base64: oNr5wqOE2LBh5uKKVfTwubPfyF237OrffQj5bcMKtpw=  # sample secret
      cookie:
        name: tasklist
        domain: tasklist.my-business.com
        path: /custom-application-prefix
        secure: true
        expires-duration: PT2H
        same-site: STRICT
```

Beside the properties also the Spring Security needs to be configured. This example refers to
*Azure Active Directory* as the IDM given system:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private VanillaBpCockpitProperties cockpitProperties;
    @Autowired
    private AzureAdUsersService usersService;

    @Bean
    public JwtMapper<? extends JwtAuthenticationToken> jwtMapper() {
        return new JwtMapperImpl(cockpitProperties.getCockpit().getJwt());
    }

    @Bean
    public PassiveJwtSecurityFilter passiveJwtSecurityFilter(
            final JwtMapper<? extends JwtAuthenticationToken> jwtMapper) {
        return new PassiveJwtSecurityFilter(cockpitProperties.getCockpit().getJwt(), jwtMapper);
    }

    @Bean
    public JwtUserDetailsProvider jwtUserDetailsProvider() {
        return new AzureAdUserDetailsProvider(usersService);
    }

    @Bean
    public UserContext userContext(final JwtUserDetailsProvider userDetailsProvider) {
        return new UserContext(userDetailsProvider);
    }

    @Bean
    @Order(999) // allow other securities from workflow-modules to run first
    public SecurityFilterChain filterChain(
            final HttpSecurity http, final PassiveJwtSecurityFilter jwtSecurityFilter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .anonymous(AbstractHttpConfigurer::disable)
                .addFilterAfter(jwtSecurityFilter, BasicAuthenticationFilter.class)
                .build();
    }

}
```

The `JwtMapper`, the `JwtUserDetailsProvider` are explained in the [container documentation](../../container#security). 
