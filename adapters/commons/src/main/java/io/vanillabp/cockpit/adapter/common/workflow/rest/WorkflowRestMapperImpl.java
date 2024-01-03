package io.vanillabp.cockpit.adapter.common.workflow.rest;

import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUiUriType;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorkflowRestMapperImpl implements WorkflowRestMapper {

    @Override
    public io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent map(WorkflowCancelledEvent workflowCancelledEvent) {
        if ( workflowCancelledEvent == null ) {
            return null;
        }

        io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent workflowCancelledEvent1 = new io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent();

        workflowCancelledEvent1.setId( workflowCancelledEvent.getId() );
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
    public WorkflowCompletedEvent map(io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent workflowCompletedEvent) {
        if ( workflowCompletedEvent == null ) {
            return null;
        }

        WorkflowCompletedEvent workflowCompletedEvent1 = new WorkflowCompletedEvent();

        workflowCompletedEvent1.setId( workflowCompletedEvent.getId() );
        workflowCompletedEvent1.setWorkflowId( workflowCompletedEvent.getWorkflowId() );
        workflowCompletedEvent1.setInitiator( workflowCompletedEvent.getInitiator() );
        workflowCompletedEvent1.setTimestamp( workflowCompletedEvent.getTimestamp() );
        workflowCompletedEvent1.setSource( workflowCompletedEvent.getSource() );
        workflowCompletedEvent1.setComment( workflowCompletedEvent.getComment() );
        workflowCompletedEvent1.setBpmnProcessId( workflowCompletedEvent.getBpmnProcessId() );
        workflowCompletedEvent1.setBpmnProcessVersion( workflowCompletedEvent.getBpmnProcessVersion() );

        return workflowCompletedEvent1;
    }

    @Override
    public WorkflowCreatedOrUpdatedEvent map(WorkflowCreatedEvent workflowCreatedEvent) {
        if ( workflowCreatedEvent == null ) {
            return null;
        }

        WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent = new WorkflowCreatedOrUpdatedEvent();

        workflowCreatedOrUpdatedEvent.setId( workflowCreatedEvent.getId() );
        workflowCreatedOrUpdatedEvent.setWorkflowId( workflowCreatedEvent.getWorkflowId() );
        workflowCreatedOrUpdatedEvent.setBusinessId( workflowCreatedEvent.getBusinessId() );
        workflowCreatedOrUpdatedEvent.setInitiator( workflowCreatedEvent.getInitiator() );
        workflowCreatedOrUpdatedEvent.setTimestamp( workflowCreatedEvent.getTimestamp() );
        workflowCreatedOrUpdatedEvent.setSource( workflowCreatedEvent.getSource() );
        workflowCreatedOrUpdatedEvent.setWorkflowModule( workflowCreatedEvent.getWorkflowModule() );
        Map<String, String> map = workflowCreatedEvent.getTitle();
        if ( map != null ) {
            workflowCreatedOrUpdatedEvent.setTitle( new LinkedHashMap<String, String>( map ) );
        }
        workflowCreatedOrUpdatedEvent.setComment( workflowCreatedEvent.getComment() );
        workflowCreatedOrUpdatedEvent.setBpmnProcessId( workflowCreatedEvent.getBpmnProcessId() );
        workflowCreatedOrUpdatedEvent.setBpmnProcessVersion( workflowCreatedEvent.getBpmnProcessVersion() );
        workflowCreatedOrUpdatedEvent.setWorkflowModuleUri( workflowCreatedEvent.getWorkflowModuleUri() );
        workflowCreatedOrUpdatedEvent.setUiUriPath( workflowCreatedEvent.getUiUriPath() );
        workflowCreatedOrUpdatedEvent.setUiUriType( workflowUiUriTypeToUiUriType( workflowCreatedEvent.getUiUriType() ) );
        workflowCreatedOrUpdatedEvent.setWorkflowProviderApiUriPath( workflowCreatedEvent.getWorkflowProviderApiUriPath() );
        Map<String, Object> map1 = workflowCreatedEvent.getDetails();
        if ( map1 != null ) {
            workflowCreatedOrUpdatedEvent.setDetails( new LinkedHashMap<String, Object>( map1 ) );
        }
        workflowCreatedOrUpdatedEvent.setDetailsFulltextSearch( workflowCreatedEvent.getDetailsFulltextSearch() );

        workflowCreatedOrUpdatedEvent.setUpdated( false );

        return workflowCreatedOrUpdatedEvent;
    }

    @Override
    public WorkflowCreatedOrUpdatedEvent map(WorkflowUpdatedEvent workflowUpdatedEvent) {
        if ( workflowUpdatedEvent == null ) {
            return null;
        }

        WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent = new WorkflowCreatedOrUpdatedEvent();

        workflowCreatedOrUpdatedEvent.setId( workflowUpdatedEvent.getId() );
        workflowCreatedOrUpdatedEvent.setWorkflowId( workflowUpdatedEvent.getWorkflowId() );
        workflowCreatedOrUpdatedEvent.setBusinessId( workflowUpdatedEvent.getBusinessId() );
        workflowCreatedOrUpdatedEvent.setInitiator( workflowUpdatedEvent.getInitiator() );
        workflowCreatedOrUpdatedEvent.setTimestamp( workflowUpdatedEvent.getTimestamp() );
        workflowCreatedOrUpdatedEvent.setSource( workflowUpdatedEvent.getSource() );
        workflowCreatedOrUpdatedEvent.setWorkflowModule( workflowUpdatedEvent.getWorkflowModule() );
        Map<String, String> map = workflowUpdatedEvent.getTitle();
        if ( map != null ) {
            workflowCreatedOrUpdatedEvent.setTitle( new LinkedHashMap<String, String>( map ) );
        }
        workflowCreatedOrUpdatedEvent.setComment( workflowUpdatedEvent.getComment() );
        workflowCreatedOrUpdatedEvent.setBpmnProcessId( workflowUpdatedEvent.getBpmnProcessId() );
        workflowCreatedOrUpdatedEvent.setBpmnProcessVersion( workflowUpdatedEvent.getBpmnProcessVersion() );
        workflowCreatedOrUpdatedEvent.setWorkflowModuleUri( workflowUpdatedEvent.getWorkflowModuleUri() );
        workflowCreatedOrUpdatedEvent.setUiUriPath( workflowUpdatedEvent.getUiUriPath() );
        workflowCreatedOrUpdatedEvent.setUiUriType( workflowUiUriTypeToUiUriType( workflowUpdatedEvent.getUiUriType() ) );
        workflowCreatedOrUpdatedEvent.setWorkflowProviderApiUriPath( workflowUpdatedEvent.getWorkflowProviderApiUriPath() );
        Map<String, Object> map1 = workflowUpdatedEvent.getDetails();
        if ( map1 != null ) {
            workflowCreatedOrUpdatedEvent.setDetails( new LinkedHashMap<String, Object>( map1 ) );
        }
        workflowCreatedOrUpdatedEvent.setDetailsFulltextSearch( workflowUpdatedEvent.getDetailsFulltextSearch() );

        workflowCreatedOrUpdatedEvent.setUpdated( true );

        return workflowCreatedOrUpdatedEvent;
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
