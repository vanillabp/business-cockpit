package io.vanillabp.cockpit.workflowlist.api.v1;

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
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.WorkflowRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;

/**
 * Turns the workflows the cockpit stores into what its GUI API answers, and the values of a GUI API
 * request into what the service reads. MapStruct writes the implementation,
 * {@code WorkflowListGuiApiMapperImpl}. The application's {@link PersonAndGroupApiMapper} turns
 * persons and groups.
 */
@Mapper(implementationName = "WorkflowListGuiApiMapperImpl")
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
   * Turns which workflows a GUI API request asks for into the mode the service reads.
   *
   * @param mode The mode of the request
   * @return The mode for the service
   */
  @ValueMapping(target = "All", source = "ALL")
  @ValueMapping(target = "Active", source = "ACTIVE")
  @ValueMapping(target = "Inactive", source = "INACTIVE")
  public abstract WorkflowlistService.RetrieveItemsMode toModel(
      WorkflowRetrieveMode mode);

  /**
   * What the cockpit knows about one workflow, for its own user interface.
   * <p>
   * <code>uiUri</code> carries the path the workflow module reported, unchanged. Turning a path
   * into an address is a convention between the module and the user interface which loads it, so
   * the cockpit does not read <code>uiUriType</code> and builds nothing from it.
   *
   * @param workflow The workflow as it is stored
   * @return The workflow as the GUI API answers it
   */
  @Mapping(target = "uiUri", source = "uiUriPath")
  @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(workflow))")
  @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
  public abstract Workflow toApi(
      io.vanillabp.cockpit.workflowlist.model.Workflow workflow);

  /**
   * What the cockpit knows about several workflows, for its own user interface.
   *
   * @param data The workflows as they are stored
   * @return The workflows as the GUI API answers them
   */
  public abstract List<Workflow> toApi(
      List<io.vanillabp.cockpit.workflowlist.model.Workflow> data);

  /**
   * One page of the workflow list, as the GUI API answers it.
   *
   * @param data The page of stored workflows
   * @param timestamp The time of the server when the page was read
   * @param requestId The id the client gave its request, sent back so it can match the answer
   * @return The page as the GUI API answers it
   */
  @Mapping(target = "page.number", source = "data.number")
  @Mapping(target = "page.size", source = "data.size")
  @Mapping(target = "page.totalPages", source = "data.totalPages")
  @Mapping(target = "page.totalElements", source = "data.totalElements")
  @Mapping(target = "workflows", expression = "java(toApi(data.getContent()))")
  @Mapping(target = "serverTimestamp", source = "timestamp")
  @Mapping(target = "requestId", source = "requestId")
  public abstract Workflows toApi(
      Page<io.vanillabp.cockpit.workflowlist.model.Workflow> data,
      OffsetDateTime timestamp,
      String requestId);

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
   * The address of the workflow module of the workflow, as the cockpit's proxy serves it.
   *
   * @param workflow The workflow as it is stored
   * @return The path below which the cockpit serves the workflow module, or {@code null} if the
   *         workflow names no workflow module
   */
  @NoMappingMethod
  protected String proxiedWorkflowModuleUri(
      final io.vanillabp.cockpit.workflowlist.model.Workflow workflow) {

    if (workflow.getWorkflowModuleId() == null) {
      return null;
    }

    return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX + workflow.getWorkflowModuleId();

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
