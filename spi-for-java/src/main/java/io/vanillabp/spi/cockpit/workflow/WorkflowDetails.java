package io.vanillabp.spi.cockpit.workflow;

import java.util.List;
import java.util.Map;

import io.vanillabp.spi.cockpit.usertask.DetailCharacteristics;

public interface WorkflowDetails {

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

    String getComment();

    Map<String, String> getTitle();

    Map<String, Object> getDetails();

    Map<String, ? extends DetailCharacteristics> getDetailsCharacteristics();

    String getDetailsFulltextSearch();

    List<String> getI18nLanguages();

    /**
     * The context the templates for title and details fulltext search are rendered with.
     * 
     * @return The template context
     */
    Object getTemplateContext();
    
    /**
     * @return What the cockpit opens for this workflow. It is the path the workflow module
     *         configured unless a details provider set one of its own
     */
    String getUiUriPath();

}
