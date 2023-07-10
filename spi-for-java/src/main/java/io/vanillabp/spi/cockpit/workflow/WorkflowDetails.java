package io.vanillabp.spi.cockpit.workflow;

import java.util.List;
import java.util.Map;

import io.vanillabp.spi.cockpit.usertask.DetailCharacteristics;

public interface WorkflowDetails {

    String getInitiator();

    String getComment();

    Map<String, String> getWorkflowTitle();

    Map<String, String> getTitle();

    Map<String, Object> getDetails();

    Map<String, ? extends DetailCharacteristics> getDetailsCharacteristics();

    String getDetailsFulltextSearch();

    List<String> getI18nLanguages();
}
