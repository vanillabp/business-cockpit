![](../readme/vanillabp-headline.png)

# SPI for Java

*VanillaBP* is an aspect orientated [service provider interface (SPI) for workflow systems](https://github.com/vanillabp/spi-for-java)
as a Java developer would expect it to be. **This is an extension of the SPI for user task applications.**

This SPI is used by business developers to provide business data associated with user tasks created
as part of their business processes as well as to business data about the workflows that own those user tasks.
The provided data is meant to be presented to the business users off user task applications (like the
*VanillaBP Business Cockpit* itself) to support them managing their workload.

It is about these types of data:

1. User tasks lists:
   1. Provide custom columns values for filtering and sorting by business data.
   2. Provide information about task responsibility (candidate users and groups for completing the task).
   3. Provide meta-data about the task (title, due date, etc.).
4. Workflow lists:
    1. Provide custom columns values for filtering and sorting by business data.
    2. Provide information about workflow accessibility (users and groups allowed to get information regarding a workflow).
    3. Provide meta-data about the workflow (title, etc.).

By using this SPI the business code is fully decoupled from the user task application. Vendors of user task applications
(like the *VanillaBP Business Cockpit* itself) provide adapters collecting the data provided using this SPI.
  
## Content

1. [How it looks like](#how-it-looks-like)
1. [Usage](#usage)
   1. [Using an adapter](#using-an-adapter)
   1. [Wire up a user task](#wire-up-a-user-task)
      1. [Reporting business data for a user task](#reporting-business-data-for-a-user-task)
      1. [Using templates for user task details](#using-templates-for-user-task-details)
   1. [Wire up a workflow](#wire-up-a-workflow)
      1. [Reporting business data for a workflow](#reporting-business-data-for-a-workflow)
      1. [Using templates for workflow details](#using-templates-for-workflow-details)
   1. [Trigger updates programmatically](#trigger-updates-programmatically)
   1. [Retrieve user task details programmatically](#retrieve-user-task-details-programmatically)

## How it looks like

This is a section of a taxi ride workflow and should give you an idea of how the Vanilla BP SPI for user task
applications is used in your business code:

```java
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@WorkflowService(workflowAggregateClass = Ride.class)
public class TaxiRide {
     // collect data for a specific user task
     @UserTaskDetailsProvider
     public UserTaskDetails retrievePayment(
             final Ride ride,
             final PrefilledUserTaskDetails userTaskDetails) {
         // set task's due-data depending on the ride's end time
         userTaskDetails.setDueDate(ride.getEndedAt().plus(ChronoUnit.HOURS, 2));
         return userTaskDetails;
     }
     // collect data for a specific workflow
     @WorkflowDetailsProvider
     public WorkflowDetails rideDetails(
             final Ride ride,
             final PrefilledWorkflowDetails workflowDetails) {
         // provide data for custom column "from"
         workflowDetails.setDetails(
                Map.of("from", ride.getFrom()));
        return workflowDetails;
    }
}
```

## Usage

### Using an adapter

Vendors of user task applications can provide adapters which uses this SPI to retrieve details about
the user tasks and workflows to be shown in the user task application. Those adapters will react to user task
and workflow life-cycle events of the underlying Business Process Management System (BPMS), like `created`,
`changed`, `completed` or `cancelled`.

An adapter will scan your business code for methods annotated by
`io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider` or
`io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider` and will call the respective method on processing
the lifecycle events mentioned. The business code should only use this SPI as a dependency (Maven or Gradle)
and the runtime container hosting the business code (e.g. Spring Boot container) should use the
specific adapter as a dependency (e.g. [Camunda 7](../adapters/camunda7/README.md) or
[Camunda 8](../adapters/camunda8/README.md)). **Please consult the adapter's documentation for features and
configuration specific to the targeting user task application and the underlying BPMS!** 

For the business workflow module:
```xml
<dependency>
   <groupId>io.vanillabp.businesscockpit</groupId>
   <artifactId>spi-for-java</artifactId>
</dependency>
```

For the runtime container (in simple use cases this is the same Maven project as the business workflow module):
```xml
<dependency>
   <groupId>io.vanillabp.businesscockpit</groupId>
   <artifactId>camunda8-spring-boot-adapter</artifactId>
</dependency>
```

*Hint:* An adapter will provide defaults for some of those details but they may be modified or enriched by the business
software. The user task's title is a representative example for this: The title is defaulted to the text
read from underlying BPMN model but typically, texts in BPMN models are shortened, not multi-language and lack of
context information (since they are originated in the context of the BPMN process but this context is missing on
showing the task in the user task application).

### Wire up a user task

Wiring up a user task means to provide details about a specific user task to be shown in the list of user tasks
in a user task application (like the *VanillaBP Business Cockpit*).

To modify or enrich user task details the
[workflow's service class](https://github.com/vanillabp/spi-for-java?#wire-up-a-process)
has to provide a custom method for each individual
user task. The return-value's class has to implement the interface `io.vanillabp.spi.cockpit.usertask.UserTaskDetails`:

```java
@WorkflowService(...)
public class TaxiRide {
    ...
    @UserTaskDetailsProvider
    public UserTaskDetails retrievePayment() {
        var details = new MyUserTaskDetails();
        ...
        return details;
    }
    ...
}
```

All non-null attributes provided by `io.vanillabp.spi.cockpit.usertask.UserTaskDetails` returned by the method will
override default values provided by the adapter implementation.

Since the SPI works according the principles of *convention of configuration* the user task is identified by the method's name.
As a fallback the name of the task can be provided by the annotation's attribute `taskDefinition`:

```java
    @UserTaskDetailsProvider(taskDefinition = "RETRIEVE_PAYMENT")
    public UserTaskDetails retrievePayment() {
        var details = new MyUserTaskDetails();
        ...
        return details;
    }
```

Sometimes for multiple user task the same details should be provided. This can be achieved by adding the annotation
multiple times:

```java
    @UserTaskDetailsProvider(taskDefinition = "RISK_ASSESSMENT")
    @UserTaskDetailsProvider(taskDefinition = "OUT_OF_STOCK_MANAGEMENT")
    public UserTaskDetails userTaskOrderDetails() {
        var details = new MyUserTaskDetails();
        ...
        return details;
    }
```

To retrieve context information parameters can be added which will be filled by the 
adapter used (which is calling this method). These parameters (originated in [VanillaBP](https://www.vanillabp.io)) are supported:

1. Prefilled details: The adapter provides a prefilled details object which may be simply adopted and returned
   (see JavaDoc of `io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails`).
1. [Aggregate](https://github.com/vanillabp/spi-for-java#workflow-aggregate-argument): Typically, user task details are
based on attributes of the [workflow-aggregate](https://github.com/vanillabp/spi-for-java/blob/main/README.md#process-specific-workflow-aggregate).
1. [Task id](https://github.com/vanillabp/spi-for-java#task-id): The current user task's identifier (also passed as part of the prefilled details parameter).
1. [Task event](https://github.com/vanillabp/spi-for-java#task-event): The event which caused calling this method.
1. [Multi-instance](https://github.com/vanillabp/spi-for-java#tasks): The current multi-instance context if the
user task is part of a multi-instance execution.

Example:

```java
    @UserTaskDetailsProvider
    public UserTaskDetails retrievePayment(
            final PrefilledUserTaskDetails details,
            final PaymentAggregate aggregate) {
        details.setDueDate(aggregate.getWishDate());
        return details;
    }
```

#### Reporting business data for a user task

There are a lot of details which can be provided as part of the method's result implementing
`io.vanillabp.spi.cockpit.usertask.UserTaskDetails` or by simple modifying the provided
`io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails`. Checkout the respective JavaDoc.

For passing business data to the user task application (e.g. to fill custom columns in the user task list),
the details property is used. It is a map which values may be simple values or complex objects of the
business application. However, please keep in mind that complex objects will be serialized into JSON internally
by the adapter. As a
best practice one should not pass original business objects but rather define classes specialized for 
this use case. These classes should only hold properties of types supported by JSON:

```java
    @UserTaskDetailsProvider
    public UserTaskDetails retrievePayment(
            final PrefilledUserTaskDetails details,
            final PaymentAggregate aggregate) {
        final var customerDetails = new UserTaskCustomerDetails();
        customerDetails.setName(aggregate.getCustomerName());
        details.setDetails(Map.of("customer", customerDetails));
        return details;
    }
```

#### Using templates for user task details

Some detail values may be defined based on templates instead of passing a ready-to-use strings
(e.g. title of the user task). To hide the templating from the business code the SPI adapters do
this job.

Using templates is an alternative to provide context information next to custom user task list columns.
Supported details:

1. `title`: The user task's title.
2. `workflowTitle`: The user task's workflow's title.
3. `taskDefinitionTitle`: The title of the task-definition defined for the user task.
4. `getDetailsFulltextSearch`: A string used for fulltext search in the list of user tasks.

If the string given for one of the supported properties points to a template then the templated is rendered
and the result of rendering is passed to the user task application. For any details about templates
(template library used, folder to provide templates, etc.) checkout the specific
[SPI adapter documentation](../adapters/README.md).

For processing the template additional data may be passed to be used in the template. This is done by
specifying a template context map:

```java
    // assuming a Freemarker template "title-template.ftl" with content:
    // Retrieve payment for ${customer}
    @UserTaskDetailsProvider
    public UserTaskDetails retrievePayment(
            final PrefilledUserTaskDetails details,
            final PaymentAggregate aggregate) {
        details.setTitle(Map.of("en", "title-template.ftl"));
        details.setTemplateContext(Map.of("customer", aggregate.getCustomerName()));
        return details;
    }
```

### Wire up a workflow

Wiring up a workflow means to provide details about a workflow to be shown in the list of workflows
in a user task application (like the *VanillaBP Business Cockpit*).

To modify or enrich workflow details the
[workflow's service class](https://github.com/vanillabp/spi-for-java?#wire-up-a-process)
has to provide a custom method. The return-value's class has to implement the interface
`io.vanillabp.spi.cockpit.workflow.WorkflowDetails`:

```java
@WorkflowService(...)
public class TaxiRide {
    ...
    @WorkflowDetailsProvider
    public WorkflowDetails workflowDetails() {
        var details = new MyWorkflowDetails();
        ...
        return details;
    }
    ...
}
```

All non-null attributes provided by `io.vanillabp.spi.cockpit.workflow.WorkflowDetails` returned by the method will
override default values provided by the adapter implementation.

To retrieve context information parameters can be added which will be filled by the
adapter used (which is calling this method). These parameters (originated in [VanillaBP](https://www.vanillabp.io)) are supported:

1. Prefilled details: The adapter provides a prefilled details object which may be simply adopted and returned
   (see JavaDoc of `io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails`).
1. [Aggregate](https://github.com/vanillabp/spi-for-java#workflow-aggregate-argument): Typically, user task details are
   based on attributes of the [workflow-aggregate](https://github.com/vanillabp/spi-for-java/blob/main/README.md#process-specific-workflow-aggregate).

Example:

```java
    @WorkflowDetailsProvider
    public WorkflowDetails workflowDetails(
            final PrefilledWorkflowDetails details,
            final PaymentAggregate aggregate) {
        details.setFulltextSearch(aggregate.getCustomerName());
        return details;
    }
```

#### Reporting business data for a workflow

There are a lot of details which can be provided as part of the method's result implementing
`io.vanillabp.spi.cockpit.workflow.WorkflowDetails` or by simple modifying the provided
`io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails`. Checkout the respective JavaDoc.

For passing business data to the user task application (e.g. to fill custom columns in the workflow list),
the details property is used. It is a map which values may be simple values or complex objects of the
business application. However, please keep in mind that complex objects will be serialized into JSON internally
by the adapter. As a
best practice one should not pass original business objects but rather define classes specialized for
this use case. These classes should only hold properties of types supported by JSON:

```java
    @WorkflowDetailsProvider
    public WorkflowDetails workflowDetails(
            final PrefilledWorkflowDetails details,
            final PaymentAggregate aggregate) {
        final var customerDetails = new WorkflowCustomerDetails();
        customerDetails.setName(aggregate.getCustomerName());
        details.setDetails(Map.of("customer", customerDetails));
        return details;
    }
```

#### Using templates for workflow details

Some detail values may be defined based on templates instead of passing a ready-to-use strings
(e.g. title of the workflow). To hide the templating from the business code the SPI adapters do
this job.

Using templates is an alternative to provide context information next to custom user task list columns.
Supported details:

1. `title`: The workflow's title.
4. `getDetailsFulltextSearch`: A string used for fulltext search in the list of workflows.

If the string given for one of the supported properties points to a template then the templated is rendered
and the result of rendering is passed to the user task application. For any details about templates
(template library used, folder to provide templates, etc.) checkout the specific
[SPI adapter documentation](../adapters/README.md).

For processing the template additional data may be passed to be used in the template. This is done by
specifying a template context map:

```java
    // assuming a Freemarker template "title-template.ftl" with content:
    // Retrieve payment for ${customer}
    @WorkflowDetailsProvider
    public WorkflowDetails workflowDetails(
            final PrefilledWorkflowDetails details,
            final PaymentAggregate aggregate) {
        details.setTitle(Map.of("en", "title-template.ftl"));
        return details;
    }
```

### Trigger updates programmatically

User task details and workflow details are updated based on event of the BPMS used
(see [Using an adapter](#using-an-adapter)). But there are also external events which may cause changes
of business data shown in the user task application (e.g. custom columns in the list of user tasks).
To trigger an update programmatically one can inject a service `io.vanillabp.spi.cockpit.BusinessCockpitService`
which provides methods for this use-case:

```java
@WorkflowService(...)
public class TaxiRide {
    @Autowired
    private BusinessCockpitService<Ride> businessCockpitService;
    ...
    // change business values in the context of a user task form
    public void changePaymentDueToCashGiven(String rideId, String userTaskId, float outstandingPayment) {
       final var ride = rideRepository.findById(rideId);
       ride.setOutstandingPayment(outstandingPayment);
       // trigger update of 
       businessCockpitService.aggregateChanged(ride, userTaskId);
    }
    // change business values NOT in the context of a user task form
    public void changePickupTime(String rideId, OffsetDateTime newPickupTime) {
        final var ride = rideRepository.findById(rideId);
        ride.setPickupTime(newPickupTime);
        businessCockpitService.aggregateChanged(ride);
    }
    ...
}
```

### Retrieve user task details programmatically

In some situations one needs to retrieve the same details for a certain user task as they were sent
to the user task application. An example is to sent email-notifications in which the text should be exactly
the same as shown in the user task application. Since some for some details default values and for
other details templates are used, those values are not available to the business application.

However, the service `io.vanillabp.spi.cockpit.BusinessCockpitService` provides a method providing all
data filled by the SPI adapter:

```java
@WorkflowService(...)
public class TaxiRide {
    @Autowired
    private BusinessCockpitService<Ride> businessCockpitService;
    ...
    @WorkflowTask
    public void sendEmailNotification(Ride ride) {
       final var ride = rideRepository.findById(rideId);
       final var userTaskId = ride.getPaymentUserTaskId();
       final var userTask = businessCockpitService.getUserTask(ride, userTaskId);
       sendOverdueNotification(ride.getEmailAddress(), userTaskId, userTask.getTitle(), userTask.getDueDate());
    }
    ...
}
```
