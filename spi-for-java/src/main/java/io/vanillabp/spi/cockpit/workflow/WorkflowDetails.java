package io.vanillabp.spi.cockpit.workflow;

import java.util.List;
import java.util.Map;

/**
 * What a workflow module reports to the Business Cockpit about one workflow, which is one run of
 * a BPMN process for one workflow aggregate. The workflow list of the cockpit shows it.
 *
 * <p>A method annotated with {@link WorkflowDetailsProvider} returns it. Most such methods fill in
 * the {@link PrefilledWorkflowDetails} they were given and return that very object. A method may
 * also return an object of its own. The cockpit then takes only the values a provider may set,
 * and it keeps what the BPMS reported, like the id.
 *
 * @see PrefilledWorkflowDetails
 */
public interface WorkflowDetails {

    /**
     * The BPMS' own id of the workflow: the process-instance id in Camunda 7, the process-instance
     * key in Camunda 8.
     *
     * @return The id of the workflow
     */
    String getId();

    /**
     * Who caused this report. The value is mandatory: once a details provider has run, every
     * details object carries one, and the two allowed answers are a user id and the constant
     * {@link io.vanillabp.spi.cockpit.Initiator#SYSTEM} for an action no user caused.
     *
     * <p>Where the property <code>vanillabp.cockpit.initiator-source</code> says
     * <code>by-application</code> and nothing is set, the report fails and the message says
     * which of the two answers to give. Where it says <code>system</code>, the constant is
     * filled in.
     *
     * @return The user who caused this report
     */
    String getInitiator();

    /**
     * A short text the cockpit stores with the workflow. Lists of workflows can be sorted and
     * filtered by it.
     *
     * @return The comment, or <code>null</code>
     */
    String getComment();

    /**
     * The title of the workflow, in each language of {@link #getI18nLanguages()}.
     *
     * <p>The key of the map is a language, like <code>en</code>, and the value is the text in that
     * language. A value set by a details provider is first taken as the name of a template. Where a
     * template of that name exists, the cockpit renders it. Otherwise the value is shown as it is.
     * Where nothing is set, the cockpit renders its own template for this title, if the application
     * has templates, or it shows the name of the BPMN process.
     *
     * @return The titles by language
     */
    Map<String, String> getTitle();

    /**
     * The business data of the workflow. The workflow module decides what goes in. The cockpit
     * stores it with the workflow, and columns, sorting and filters of the workflow list can read its
     * keys, like <code>details.customer.name</code>.
     *
     * @return The business data, or <code>null</code>
     */
    Map<String, Object> getDetails();

    /**
     * The text the full-text search of the workflow list looks in. The workflow module puts it
     * together itself. Data which is not in this text cannot be found by that search. Where it is
     * left <code>null</code> and the application has templates, the cockpit renders it from a
     * template.
     *
     * @return The text to search in, or <code>null</code>
     */
    String getDetailsFulltextSearch();

    /**
     * The languages the title is rendered in, like <code>en</code> and <code>de</code>. Where this
     * is empty, the cockpit uses the languages configured for the workflow module.
     *
     * @return The languages
     */
    List<String> getI18nLanguages();

    /**
     * The context the templates for title and details fulltext search are rendered with.
     * 
     * @return The template context
     */
    Object getTemplateContext();
    
    /**
     * Where the cockpit finds the status page of this workflow.
     *
     * @return What the cockpit opens for this workflow. It is the path the workflow module
     *         configured unless a details provider set one of its own
     */
    String getUiUriPath();

    /**
     * The users who may open this workflow in the cockpit. Where neither this list nor
     * {@link #getAccessibleToGroups()} names anybody, everybody who may use the cockpit may.
     *
     * @return The user ids admitted to this workflow
     */
    List<String> getAccessibleToUsers();

    /**
     * The groups whose members may open this workflow in the cockpit.
     *
     * @return The group ids admitted to this workflow
     * @see #getAccessibleToUsers()
     */
    List<String> getAccessibleToGroups();

}
