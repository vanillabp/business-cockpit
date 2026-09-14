package io.vanillabp.spi.cockpit.workflow;

import java.util.List;
import java.util.Map;

import io.vanillabp.spi.cockpit.usertask.DetailCharacteristics;

public interface WorkflowDetails {

    String getInitiator();

    String getComment();

    Map<String, String> getTitle();

    Map<String, Object> getDetails();

    Map<String, ? extends DetailCharacteristics> getDetailsCharacteristics();

    String getDetailsFulltextSearch();

    List<String> getI18nLanguages();

    /**
     * Used for rendering title and details-fulltext-search
     * based on templates.
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
