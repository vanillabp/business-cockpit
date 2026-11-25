package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import io.vanillabp.cockpit.devshell.simulator.DevShellSimulator;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModuleRepository;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
import io.vanillabp.cockpit.gui.api.v1.EndReason;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import liquibase.Scope;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * An application which does not let Hibernate create the tables applies the changelog
 * {@value #CHANGELOG} the simulator ships. Each field of an entity needs its column there. A field
 * without one shows up only when a report arrives: the insert fails and the report gets a 500.
 *
 * <p>Each test applies the changelog to an empty database and then starts the simulator with
 * {@code ddl-auto=validate}. Hibernate refuses to start when a table or a column of an entity is
 * missing or has a type it cannot use. The test then stores a row of each entity with every field
 * set and reads it back, which the validation alone does not do.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheChangelogMatchesTheEntitiesTest {

  private static final String CHANGELOG = "db/changelog/dev-shell-simulator.yaml";

  /** The newest MySQL with long-term support, the database the missing columns were found on. */
  private static final String MYSQL_IMAGE = "mysql:8.4";

  /** Keeps the names of tables and columns as the entities write them. */
  private static final String STANDARD_NAMING = "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl";

  private static final OffsetDateTime SOME_TIME = OffsetDateTime
      .parse("2026-11-25T10:15:30Z")
      .truncatedTo(ChronoUnit.SECONDS);

  @Test
  void theChangelogMatchesTheEntitiesOnH2() throws Exception {

    final var url = "jdbc:h2:mem:the-changelog-matches-the-entities;DB_CLOSE_DELAY=-1";
    applyTheChangelog(url, "sa", "");
    try (var simulator = startTheSimulatorWhichValidates(url, "sa", "", "org.hibernate.dialect.H2Dialect")) {
      storeAndReadEveryField(simulator);
    }

  }

  @Test
  void theChangelogMatchesTheEntitiesOnMySql() throws Exception {

    assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "MySQL runs in a container, and Docker is missing");
    try (var mysql = new MySQLContainer(MYSQL_IMAGE)) {
      mysql.start();
      applyTheChangelog(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
      try (var simulator = startTheSimulatorWhichValidates(
          mysql.getJdbcUrl(),
          mysql.getUsername(),
          mysql.getPassword(),
          "org.hibernate.dialect.MySQLDialect",
          // MySQL on Linux tells table names apart by case. The changelog writes them in upper
          // case, like the entities, and Spring Boot's default naming looks for them in lower case.
          "spring.jpa.hibernate.naming.physical-strategy=%s".formatted(STANDARD_NAMING))) {
        storeAndReadEveryField(simulator);
      }
    }

  }

  private static void applyTheChangelog(
      final String url,
      final String username,
      final String password) throws Exception {

    Scope.child(Scope.Attr.resourceAccessor, new ClassLoaderResourceAccessor(), () -> new CommandScope(
        UpdateCommandStep.COMMAND_NAME)
        .addArgumentValue(DbUrlConnectionArgumentsCommandStep.URL_ARG, url)
        .addArgumentValue(DbUrlConnectionArgumentsCommandStep.USERNAME_ARG, username)
        .addArgumentValue(DbUrlConnectionArgumentsCommandStep.PASSWORD_ARG, password)
        .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, CHANGELOG)
        .execute());

  }

  private static ConfigurableApplicationContext startTheSimulatorWhichValidates(
      final String url,
      final String username,
      final String password,
      final String dialect,
      final String... moreProperties) {

    final var properties = Stream.concat(Stream.of(
        "server.port=0",
        "spring.datasource.url=%s".formatted(url),
        "spring.datasource.username=%s".formatted(username),
        "spring.datasource.password=%s".formatted(password),
        "spring.jpa.database-platform=%s".formatted(dialect),
        "spring.jpa.hibernate.ddl-auto=validate",
        // the simulator does not start without a user to log in as
        "dev-shell-simulator.users[0].id=john"), Stream.of(moreProperties));
    return new SpringApplicationBuilder(DevShellSimulator.class)
        .properties(properties.toArray(String[]::new))
        .run();

  }

  private static void storeAndReadEveryField(
      final ConfigurableApplicationContext simulator) {

    final var workflowModules = simulator.getBean(WorkflowModuleRepository.class);
    final var module = new WorkflowModule();
    module.setId("loan-approval");
    module.setVersion(1L);
    module.setUri("http://localhost:8080/loan-approval");
    module.setAccessibleToGroups(List.of("clerks"));
    module.setGroupHierarchy(Map.of("clerks", List.of("seniors")));
    workflowModules.save(module);
    assertThat(workflowModules.findById("loan-approval"))
        .get()
        .satisfies(stored -> assertThat(stored.getGroupHierarchy()).containsKey("clerks"));

    final var workflows = simulator.getBean(WorkflowRepository.class);
    final var workflow = new Workflow();
    workflow.setId("workflow-1");
    workflow.setCreatedAt(SOME_TIME);
    workflow.setUpdatedAt(SOME_TIME);
    workflow.setEndedAt(SOME_TIME);
    workflow.setSuspendedAt(SOME_TIME);
    workflow.setSuspendedBy("john");
    workflow.setEndReason(EndReason.CANCELLED);
    workflow.setNote("a note of the users");
    workflow.setWorkflowModuleId("loan-approval");
    workflow.setBusinessId("business-1");
    workflow.setTitle(Map.of("en", "Loan 1"));
    workflow.setDetails(Map.of("amount", 1000));
    workflows.save(workflow);
    assertThat(workflows.findById("workflow-1"))
        .get()
        .satisfies(stored -> {
          assertThat(stored.getSuspendedAt()).isEqualTo(SOME_TIME);
          assertThat(stored.getSuspendedBy()).isEqualTo("john");
          assertThat(stored.getEndReason()).isEqualTo(EndReason.CANCELLED);
          assertThat(stored.getNote()).isEqualTo("a note of the users");
        });

    final var userTasks = simulator.getBean(UserTaskRepository.class);
    final var userTask = new UserTask();
    userTask.setId("task-1");
    userTask.setCreatedAt(SOME_TIME);
    userTask.setEndedAt(SOME_TIME);
    userTask.setSuspendedAt(SOME_TIME);
    userTask.setSuspendedBy("john");
    userTask.setEndReason(EndReason.COMPLETED);
    userTask.setNote("a note of the users");
    userTask.setRead(SOME_TIME);
    userTask.setDueDate(SOME_TIME);
    userTask.setFollowUpDate(SOME_TIME);
    userTask.setWorkflowId("workflow-1");
    userTask.setTitle(Map.of("en", "Check the loan"));
    userTask.setExcludedCandidateUsers(List.of("jane"));
    userTask.setAdmittedUsers(List.of("joe"));
    userTask.setNotificationDelivery("IMMEDIATELY");
    userTask.setLastReport(Map.of("id", "event-1"));
    userTasks.save(userTask);
    assertThat(userTasks.findById("task-1"))
        .get()
        .satisfies(stored -> {
          assertThat(stored.getSuspendedAt()).isEqualTo(SOME_TIME);
          assertThat(stored.getRead()).isEqualTo(SOME_TIME);
          assertThat(stored.getEndReason()).isEqualTo(EndReason.COMPLETED);
          assertThat(stored.getNote()).isEqualTo("a note of the users");
          assertThat(stored.getLastReport()).containsEntry("id", "event-1");
        });

  }

}
