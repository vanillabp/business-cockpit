package io.vanillabp.cockpit.tasklist.api.v1;

import java.time.OffsetDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.KwicResult;
import io.vanillabp.cockpit.gui.api.v1.SearchQuery;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.tasklist.UserTaskService.RetrieveItemsMode;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;

/**
 * Turns the user tasks the cockpit stores into what its GUI API answers, and the values of a GUI
 * API request into what the service reads. MapStruct writes the implementation,
 * {@code TasklistGuiApiMapperImpl}. The application's {@link PersonAndGroupApiMapper} turns
 * persons and groups.
 */
@Mapper(implementationName = "TasklistGuiApiMapperImpl")
public abstract class GuiApiMapper {

  private static final String PERSON_MAPPING = "personMapping";
  private static final String GROUP_MAPPING = "groupMapping";

  @Autowired
  private PersonAndGroupApiMapper personAndGroupMapper;

  /**
   * Used by the implementation MapStruct writes.
   */
  public GuiApiMapper() {

  }

  /**
   * Turns a stored person into a person of the GUI API.
   *
   * @param user The stored person
   * @return The person of the GUI API
   */
  @Named(PERSON_MAPPING)
  public io.vanillabp.cockpit.gui.api.v1.Person toPerson(
      final Person user) {
    return personAndGroupMapper.personToApiPerson(user);
  }

  /**
   * Turns a stored group into a group of the GUI API.
   *
   * @param group The stored group
   * @return The group of the GUI API
   */
  @Named(GROUP_MAPPING)
  public io.vanillabp.cockpit.gui.api.v1.Group toGroup(
      final Group group) {
    return personAndGroupMapper.groupToApiGroup(group);
  }

  /**
   * What the cockpit knows about one user task, for its own user interface.
   * <p>
   * <code>uiUri</code> carries the path the workflow module reported, unchanged. Turning a path
   * into an address is a convention between the module and the user interface which loads it, so
   * the cockpit does not read <code>uiUriType</code> and builds nothing from it.
   *
   * @param userTask The task as it is stored
   * @param userId Who is asking, which decides whether the task counts as read
   * @return The task as the GUI API answers it
   */
  @Mapping(target = "uiUri", source = "userTask.uiUriPath")
  @Mapping(target = "dueDate", expression = "java(mapDateTimeMaxToNull(userTask))")
  @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(userTask))")
  @Mapping(target = "read", expression = "java(userTask.getReadAt(userId))")
  @Mapping(target = "assignee", source = "userTask.assignee", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "candidateUsers", source = "userTask.candidateUsers", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "candidateGroups", source = "userTask.candidateGroups", qualifiedByName = GROUP_MAPPING)
  public abstract UserTask toApi(
      io.vanillabp.cockpit.tasklist.model.UserTask userTask,
      String userId);

  /**
   * What the cockpit knows about several user tasks, for its own user interface.
   *
   * @param userTasks The tasks as they are stored, or {@code null}
   * @param userId Who is asking, which decides whether a task counts as read
   * @return The tasks as the GUI API answers them, or {@code null}
   */
  public List<UserTask> toApi(
      List<io.vanillabp.cockpit.tasklist.model.UserTask> userTasks,
      String userId) {
    if (userTasks == null) {
      return null;
    }
    return userTasks
        .stream()
        .map(userTask -> toApi(userTask, userId))
        .toList();
  }

  /**
   * One page of the user task list, as the GUI API answers it.
   *
   * @param data The page of stored tasks
   * @param timestamp The time of the server when the page was read
   * @param userId Who is asking, which decides whether a task counts as read
   * @return The page as the GUI API answers it
   */
  @Mapping(target = "page.number", source = "data.number")
  @Mapping(target = "page.size", source = "data.size")
  @Mapping(target = "page.totalPages", source = "data.totalPages")
  @Mapping(target = "page.totalElements", source = "data.totalElements")
  @Mapping(target = "userTasks", expression = "java(toApi(data.getContent(), userId))")
  @Mapping(target = "serverTimestamp", source = "timestamp")
  public abstract UserTasks toApi(
      Page<io.vanillabp.cockpit.tasklist.model.UserTask> data,
      OffsetDateTime timestamp,
      String userId);

  /**
   * Turns a filter of a GUI API request into the filter the service reads.
   *
   * @param data The filter of the request
   * @return The filter for the service
   */
  public abstract io.vanillabp.cockpit.util.SearchQuery toModel(
      SearchQuery data);

  /**
   * Turns the filters of a GUI API request into the filters the service reads.
   *
   * @param data The filters of the request
   * @return The filters for the service
   */
  public abstract List<io.vanillabp.cockpit.util.SearchQuery> toModel(
      List<SearchQuery> data);

  /**
   * Turns which tasks a GUI API request asks for into the mode the service reads.
   *
   * @param mode The mode of the request
   * @return The mode for the service
   */
  @ValueMapping(target = "All", source = "ALL")
  @ValueMapping(target = "OpenTasks", source = "OPENTASKS")
  @ValueMapping(target = "OpenTasksWithoutFollowUp", source = "OPENTASKSWITHOUTFOLLOWUP")
  @ValueMapping(target = "OpenTasksWithFollowUp", source = "OPENTASKSWITHFOLLOWUP")
  @ValueMapping(target = "OpenTaskOnlyFollowUp", source = "OPENTASKONLYFOLLOWUP")
  @ValueMapping(target = "ClosedTasksOnly", source = "CLOSEDTASKSONLY")
  public abstract RetrieveItemsMode toModel(
      UserTaskRetrieveMode mode);

  /**
   * The address of the workflow module of the user task, as the cockpit's proxy serves it.
   *
   * @param userTask The user task as it is stored
   * @return The path below which the cockpit serves the workflow module, or {@code null} if the
   *         user task names no workflow module
   */
  @NoMappingMethod
  protected String proxiedWorkflowModuleUri(
      final io.vanillabp.cockpit.tasklist.model.UserTask userTask) {

    if (userTask.getWorkflowModuleId() == null) {
      return null;
    }

    return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX + userTask.getWorkflowModuleId();

  }

  /**
   * The due date of the task for the GUI API. A task without a due date is stored with
   * {@link OffsetDateTime#MAX}, and the GUI API answers {@code null} for it.
   *
   * @param userTask The task as it is stored
   * @return The due date, or {@code null} if the task has none
   */
  @NoMappingMethod
  protected OffsetDateTime mapDateTimeMaxToNull(
      final io.vanillabp.cockpit.tasklist.model.UserTask userTask) {
    if ((userTask.getDueDate() != null) && userTask.getDueDate().equals(OffsetDateTime.MAX)) {
      return null;
    }

    return userTask.getDueDate();
  }

  /**
   * Turns a suggestion for a search field into what the GUI API answers.
   *
   * @param result The word found and how often it was found
   * @return The suggestion of the GUI API
   */
  public abstract KwicResult toApi(
      io.vanillabp.cockpit.util.kwic.KwicResult result);

}
