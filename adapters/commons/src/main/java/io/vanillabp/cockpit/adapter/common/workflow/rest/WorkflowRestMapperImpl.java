package io.vanillabp.cockpit.adapter.common.workflow.rest;

import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUiUriType;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UiUriType;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkflowRestMapperImpl implements WorkflowRestMapper {

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent map(WorkflowCancelledEvent workflowCancelledEvent) {
        if ( workflowCancelledEvent == null ) {
            return null;
        }

        io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent workflowCancelledEvent1 = new io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent();

        workflowCancelledEvent1.setId( workflowCancelledEvent.getEventId() );
        workflowCancelledEvent1.setWorkflowId( workflowCancelledEvent.getWorkflowId() );
        workflowCancelledEvent1.setInitiator( workflowCancelledEvent.getInitiator() );
        workflowCancelledEvent1.setTimestamp( workflowCancelledEvent.getTimestamp() );
        workflowCancelledEvent1.setSource( workflowCancelledEvent.getSource() );
        workflowCancelledEvent1.setComment( workflowCancelledEvent.getComment() );
        workflowCancelledEvent1.setBpmnProcessId( workflowCancelledEvent.getBpmnProcessId() );
        workflowCancelledEvent1.setBpmnProcessVersion( workflowCancelledEvent.getBpmnProcessVersion() );

        return workflowCancelledEvent1;
    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent map(WorkflowCompletedEvent workflowCompletedEvent) {
        if ( workflowCompletedEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent();

        result.setId( workflowCompletedEvent.getEventId() );
        result.setWorkflowId( workflowCompletedEvent.getWorkflowId() );
        result.setBusinessId( workflowCompletedEvent.getBusinessId() );
        result.setInitiator( workflowCompletedEvent.getInitiator() );
        result.setTimestamp( workflowCompletedEvent.getTimestamp() );
        result.setSource( workflowCompletedEvent.getSource() );
        result.setWorkflowModuleId( workflowCompletedEvent.getWorkflowModuleId() );
        Map<String, String> map = workflowCompletedEvent.getTitle();
        if ( map != null ) {
            result.setTitle( new LinkedHashMap<String, String>( map ) );
        }
        result.setComment( workflowCompletedEvent.getComment() );
        result.setBpmnProcessId( workflowCompletedEvent.getBpmnProcessId() );
        result.setBpmnProcessVersion( workflowCompletedEvent.getBpmnProcessVersion() );
        result.setUiUriPath( workflowCompletedEvent.getUiUriPath() );
        result.setUiUriType( workflowUiUriTypeToUiUriType( workflowCompletedEvent.getUiUriType() ) );
        Map<String, Object> map1 = workflowCompletedEvent.getDetails();
        if ( map1 != null ) {
            result.setDetails( new LinkedHashMap<String, Object>( map1 ) );
        }
        result.setDetailsFulltextSearch( workflowCompletedEvent.getDetailsFulltextSearch() );
        result.setAccessibleToUsers( workflowCompletedEvent.getAccessibleToUsers() );
        result.setAccessibleToGroups( workflowCompletedEvent.getAccessibleToGroups() );

        result.setUpdated( true );

        return result;

    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent map(WorkflowCreatedEvent workflowCreatedEvent) {
        if ( workflowCreatedEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent();

        result.setId( workflowCreatedEvent.getEventId() );
        result.setWorkflowId( workflowCreatedEvent.getWorkflowId() );
        result.setBusinessId( workflowCreatedEvent.getBusinessId() );
        result.setInitiator( workflowCreatedEvent.getInitiator() );
        result.setTimestamp( workflowCreatedEvent.getTimestamp() );
        result.setSource( workflowCreatedEvent.getSource() );
        result.setWorkflowModuleId( workflowCreatedEvent.getWorkflowModuleId() );
        Map<String, String> map = workflowCreatedEvent.getTitle();
        if ( map != null ) {
            result.setTitle( new LinkedHashMap<String, String>( map ) );
        }
        result.setComment( workflowCreatedEvent.getComment() );
        result.setBpmnProcessId( workflowCreatedEvent.getBpmnProcessId() );
        result.setBpmnProcessVersion( workflowCreatedEvent.getBpmnProcessVersion() );
        result.setUiUriPath( workflowCreatedEvent.getUiUriPath() );
        result.setUiUriType( workflowUiUriTypeToUiUriType( workflowCreatedEvent.getUiUriType() ) );
        Map<String, Object> map1 = workflowCreatedEvent.getDetails();
        if ( map1 != null ) {
            result.setDetails( new LinkedHashMap<String, Object>( map1 ) );
        }
        result.setDetailsFulltextSearch( workflowCreatedEvent.getDetailsFulltextSearch() );
        result.setAccessibleToUsers( workflowCreatedEvent.getAccessibleToUsers() );
        result.setAccessibleToGroups( workflowCreatedEvent.getAccessibleToGroups() );

        result.setUpdated( false );

        return result;

    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent map(WorkflowUpdatedEvent workflowUpdatedEvent) {
        if ( workflowUpdatedEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent();

        result.setId( workflowUpdatedEvent.getEventId() );
        result.setWorkflowId( workflowUpdatedEvent.getWorkflowId() );
        result.setBusinessId( workflowUpdatedEvent.getBusinessId() );
        result.setInitiator( workflowUpdatedEvent.getInitiator() );
        result.setTimestamp( workflowUpdatedEvent.getTimestamp() );
        result.setSource( workflowUpdatedEvent.getSource() );
        result.setWorkflowModuleId( workflowUpdatedEvent.getWorkflowModuleId() );
        Map<String, String> map = workflowUpdatedEvent.getTitle();
        if ( map != null ) {
            result.setTitle( new LinkedHashMap<String, String>( map ) );
        }
        result.setComment( workflowUpdatedEvent.getComment() );
        result.setBpmnProcessId( workflowUpdatedEvent.getBpmnProcessId() );
        result.setBpmnProcessVersion( workflowUpdatedEvent.getBpmnProcessVersion() );
        result.setUiUriPath( workflowUpdatedEvent.getUiUriPath() );
        result.setUiUriType( workflowUiUriTypeToUiUriType( workflowUpdatedEvent.getUiUriType() ) );
        Map<String, Object> map1 = workflowUpdatedEvent.getDetails();
        if ( map1 != null ) {
            result.setDetails( new LinkedHashMap<String, Object>( map1 ) );
        }
        result.setDetailsFulltextSearch( workflowUpdatedEvent.getDetailsFulltextSearch() );
        result.setAccessibleToUsers( workflowUpdatedEvent.getAccessibleToUsers() );
        result.setAccessibleToGroups( workflowUpdatedEvent.getAccessibleToGroups() );

        result.setUpdated( true );

        return result;

    }

    protected UiUriType workflowUiUriTypeToUiUriType(WorkflowUiUriType workflowUiUriType) {
        if ( workflowUiUriType == null ) {
            return null;
        }

        UiUriType uiUriType;

        switch ( workflowUiUriType ) {
            case EXTERNAL: uiUriType = UiUriType.EXTERNAL;
            break;
            case WEBPACK_MF_REACT: uiUriType = UiUriType.WEBPACK_MF_REACT;
            break;
            default: throw new IllegalArgumentException( "Unexpected enum constant: " + workflowUiUriType );
        }

        return uiUriType;
    }
}
