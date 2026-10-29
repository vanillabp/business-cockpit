package io.vanillabp.cockpit.workflowmodules.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The stored registration of a workflow module: where it lives, which groups may see it and how
 * its groups relate to each other.
 */
@Document(collection = WorkflowModule.COLLECTION_NAME)
public class WorkflowModule {

  /** The name of the MongoDB collection which holds the registrations. */
  public static final String COLLECTION_NAME = "modules";

  /** Creates an empty registration. Spring Data uses it to read a document. */
  public WorkflowModule() {
  }

  @Id
  private String id;

  @Version
  private long version;

  private String uri;

  private String taskProviderApiUriPath;

  private String workflowProviderApiUriPath;

  private List<String> accessibleToGroups;

  private List<GroupHierarchy> groupHierarchy = null;

  /**
   * A new registration which has nothing but its id.
   *
   * @param id The id of the workflow module
   * @return The new registration
   */
  public static WorkflowModule withId(
      final String id) {

    final var result = new WorkflowModule();
    result.setId(id);
    return result;

  }

  /**
   * The id of the workflow module, which is the id of the document as well.
   *
   * @return The id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id of the workflow module.
   *
   * @param id The id
   */
  public void setId(
      String id) {
    this.id = id;
  }

  /**
   * The version of the document. Spring Data counts it up on every save and refuses a save
   * based on an older version.
   *
   * @return The version
   */
  public long getVersion() {
    return version;
  }

  /**
   * Sets the version of the document.
   *
   * @param version The version
   */
  public void setVersion(
      long version) {
    this.version = version;
  }

  /**
   * Where the module answers the cockpit's calls.
   *
   * @return The URI, or {@code null} when the module registered none
   */
  public String getUri() {
    return uri;
  }

  /**
   * Sets where the module answers the cockpit's calls.
   *
   * @param uri The URI
   */
  public void setUri(
      String uri) {
    this.uri = uri;
  }

  /**
   * The path below the URI serving the user-task provider API.
   *
   * @return The path
   */
  public String getTaskProviderApiUriPath() {
    return taskProviderApiUriPath;
  }

  /**
   * Sets the path below the URI serving the user-task provider API.
   *
   * @param taskProviderApiUriPath The path
   */
  public void setTaskProviderApiUriPath(
      String taskProviderApiUriPath) {
    this.taskProviderApiUriPath = taskProviderApiUriPath;
  }

  /**
   * The path below the URI serving the workflow provider API.
   *
   * @return The path
   */
  public String getWorkflowProviderApiUriPath() {
    return workflowProviderApiUriPath;
  }

  /**
   * Sets the path below the URI serving the workflow provider API.
   *
   * @param workflowProviderApiUriPath The path
   */
  public void setWorkflowProviderApiUriPath(
      String workflowProviderApiUriPath) {
    this.workflowProviderApiUriPath = workflowProviderApiUriPath;
  }

  /**
   * The groups which may see the module. A module with no groups is open to everybody.
   *
   * @return The groups
   */
  public List<String> getAccessibleToGroups() {
    return accessibleToGroups;
  }

  /**
   * Sets the groups which may see the module.
   *
   * @param accessibleToGroups The groups, none for everybody
   */
  public void setAccessibleToGroups(
      List<String> accessibleToGroups) {
    this.accessibleToGroups = accessibleToGroups;
  }

  /**
   * Which groups a group stands for, as the module registered it.
   *
   * @return The entries of the hierarchy, or {@code null} when the module registered none
   */
  public List<GroupHierarchy> getGroupHierarchy() {
    return groupHierarchy;
  }

  /**
   * Sets which groups a group stands for.
   *
   * @param groupHierarchy The entries of the hierarchy
   */
  public void setGroupHierarchy(
      List<GroupHierarchy> groupHierarchy) {
    this.groupHierarchy = groupHierarchy;
  }

}
