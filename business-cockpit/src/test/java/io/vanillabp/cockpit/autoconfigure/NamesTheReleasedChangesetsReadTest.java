package io.vanillabp.cockpit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import io.vanillabp.cockpit.config.MongoDbConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.util.SortIndexes;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowEndReason;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A released changeset never changes, but its code reads names which live outside of it: the names
 * of the collections, the prefix of the sort indexes, the names of the end reasons, and how the
 * classes {@link UserTask} and {@link Workflow} map a property to a field. Renaming one of them
 * changes what the changeset does on a new database, and not a line of the changeset changes.
 *
 * <p>So the values the changesets were released with are written down here as text. Whoever
 * changes one of the names gets a red test and reads why. The way out is to keep the old value,
 * not to change this test.
 */
@ExtendWith(SuppressOutputExtension.class)
class NamesTheReleasedChangesetsReadTest {

  private final MongoMappingContext mapping = mappingOfTheCockpit();

  /** The mapping with the conversions of the cockpit, which store an offset date as a value. */
  private static MongoMappingContext mappingOfTheCockpit() {

    final var mapping = new MongoMappingContext();
    mapping.setSimpleTypeHolder(new MongoDbConfiguration()
        .customConversions()
        .getSimpleTypeHolder());
    return mapping;

  }

  @Test
  void theCollectionsKeepTheirNames() {

    assertThat(UserTask.COLLECTION_NAME).isEqualTo("usertask");
    assertThat(Workflow.COLLECTION_NAME).isEqualTo("workflow");
    assertThat(WorkflowModule.COLLECTION_NAME).isEqualTo("modules");
    assertThat(NotificationOutboxEntry.COLLECTION_NAME).isEqualTo("notification_outbox");
    assertThat(User.COLLECTION_NAME).isEqualTo("users");

  }

  /**
   * The changesets of both lists drop every index whose name starts with this prefix, and the
   * workflows create one index with it.
   */
  @Test
  void theSortIndexesKeepTheirPrefix() {

    assertThat(SortIndexes.INDEX_PREFIX).isEqualTo("_sort_");
    assertThat(UserTaskService.INDEX_CUSTOM_SORT_PREFIX).isEqualTo("_sort_");
    assertThat(WorkflowlistService.INDEX_CUSTOM_SORT_PREFIX).isEqualTo("_sort_");

  }

  /** The changesets write the name of an end reason into tasks and cases which ended without one. */
  @Test
  void theEndReasonsKeepTheirNames() {

    assertThat(UserTaskEndReason.valueOf("CANCELLED")).isNotNull();
    assertThat(UserTaskEndReason.valueOf("COMPLETED")).isNotNull();
    assertThat(WorkflowEndReason.valueOf("CANCELLED")).isNotNull();
    assertThat(WorkflowEndReason.valueOf("COMPLETED")).isNotNull();

  }

  /**
   * Some changesets update by class and not by the name of the collection. Spring Data then takes
   * the collection from the class and turns each property into the field the class maps it to.
   */
  @Test
  void theClassesTheChangesetsUpdateByKeepTheirCollectionAndFields() {

    assertThat(collectionOf(UserTask.class)).isEqualTo("usertask");
    assertThat(fieldStoredFor(UserTask.class, "readBy")).isEqualTo("readBy");
    assertThat(fieldStoredFor(UserTask.class, "dangling")).isEqualTo("dangling");
    assertThat(fieldStoredFor(UserTask.class, "assignee")).isEqualTo("assignee");

    assertThat(collectionOf(Workflow.class)).isEqualTo("workflow");
    assertThat(fieldStoredFor(Workflow.class, "workflowId")).isEqualTo("workflowId");

  }

  private String collectionOf(
      final Class<?> type) {

    return mapping
        .getRequiredPersistentEntity(type)
        .getCollection();

  }

  /**
   * The field an update by class writes for a property. A name which is no property of the class
   * is written as it is.
   */
  private String fieldStoredFor(
      final Class<?> type,
      final String property) {

    final var persistentProperty = mapping
        .getRequiredPersistentEntity(type)
        .getPersistentProperty(property);
    return persistentProperty == null
        ? property
        : persistentProperty.getFieldName();

  }

}
