package io.vanillabp.cockpit.adapter.common.usertask.rest;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUiUriType;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskActivatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskSuspendedEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserTaskRestMapperImpl implements UserTaskRestMapper {

    @Override
    public UserTaskActivatedEvent map(io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent userTaskActivatedEvent) {
        if ( userTaskActivatedEvent == null ) {
            return null;
        }

        UserTaskActivatedEvent userTaskActivatedEvent1 = new UserTaskActivatedEvent();

        userTaskActivatedEvent1.setId( userTaskActivatedEvent.getEventId() );
        userTaskActivatedEvent1.setUserTaskId( userTaskActivatedEvent.getUserTaskId() );
        userTaskActivatedEvent1.setInitiator( userTaskActivatedEvent.getInitiator() );
        userTaskActivatedEvent1.setTimestamp( userTaskActivatedEvent.getTimestamp() );
        userTaskActivatedEvent1.setSource( userTaskActivatedEvent.getSource() );
        userTaskActivatedEvent1.setComment( userTaskActivatedEvent.getComment() );

        return userTaskActivatedEvent1;
    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent map(UserTaskCancelledEvent userTaskCancelledEvent) {
        if ( userTaskCancelledEvent == null ) {
            return null;
        }

        io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent userTaskCancelledEvent1 = new io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent();

        userTaskCancelledEvent1.setId( userTaskCancelledEvent.getEventId() );
        userTaskCancelledEvent1.setUserTaskId( userTaskCancelledEvent.getUserTaskId() );
        userTaskCancelledEvent1.setInitiator( userTaskCancelledEvent.getInitiator() );
        userTaskCancelledEvent1.setTimestamp( userTaskCancelledEvent.getTimestamp() );
        userTaskCancelledEvent1.setSource( userTaskCancelledEvent.getSource() );
        userTaskCancelledEvent1.setComment( userTaskCancelledEvent.getComment() );

        return userTaskCancelledEvent1;
    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent map(UserTaskCompletedEvent userTaskCompletedEvent) {
        if ( userTaskCompletedEvent == null ) {
            return null;
        }

        io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent userTaskCompletedEvent1 = new io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent();

        userTaskCompletedEvent1.setId( userTaskCompletedEvent.getEventId() );
        userTaskCompletedEvent1.setUserTaskId( userTaskCompletedEvent.getUserTaskId() );
        userTaskCompletedEvent1.setInitiator( userTaskCompletedEvent.getInitiator() );
        userTaskCompletedEvent1.setTimestamp( userTaskCompletedEvent.getTimestamp() );
        userTaskCompletedEvent1.setSource( userTaskCompletedEvent.getSource() );
        userTaskCompletedEvent1.setComment( userTaskCompletedEvent.getComment() );

        return userTaskCompletedEvent1;
    }

    @Override
    public UserTaskSuspendedEvent map(io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent userTaskSuspendedEvent) {
        if ( userTaskSuspendedEvent == null ) {
            return null;
        }

        UserTaskSuspendedEvent userTaskSuspendedEvent1 = new UserTaskSuspendedEvent();

        userTaskSuspendedEvent1.setId( userTaskSuspendedEvent.getEventId() );
        userTaskSuspendedEvent1.setUserTaskId( userTaskSuspendedEvent.getUserTaskId() );
        userTaskSuspendedEvent1.setInitiator( userTaskSuspendedEvent.getInitiator() );
        userTaskSuspendedEvent1.setTimestamp( userTaskSuspendedEvent.getTimestamp() );
        userTaskSuspendedEvent1.setSource( userTaskSuspendedEvent.getSource() );
        userTaskSuspendedEvent1.setComment( userTaskSuspendedEvent.getComment() );

        return userTaskSuspendedEvent1;
    }

    @Override
    public UserTaskCreatedOrUpdatedEvent map(UserTaskCreatedEvent userTaskCreatedEvent) {
        if ( userTaskCreatedEvent == null ) {
            return null;
        }

        UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdatedEvent = new UserTaskCreatedOrUpdatedEvent();

        userTaskCreatedOrUpdatedEvent.setId( userTaskCreatedEvent.getId() );
        userTaskCreatedOrUpdatedEvent.setUserTaskId( userTaskCreatedEvent.getUserTaskId() );
        userTaskCreatedOrUpdatedEvent.setInitiator( userTaskCreatedEvent.getInitiator() );
        userTaskCreatedOrUpdatedEvent.setTimestamp( userTaskCreatedEvent.getTimestamp() );
        userTaskCreatedOrUpdatedEvent.setSource( userTaskCreatedEvent.getSource() );
        userTaskCreatedOrUpdatedEvent.setWorkflowModuleId( userTaskCreatedEvent.getWorkflowModuleId() );
        userTaskCreatedOrUpdatedEvent.setComment( userTaskCreatedEvent.getComment() );
        userTaskCreatedOrUpdatedEvent.setBpmnProcessId( userTaskCreatedEvent.getBpmnProcessId() );
        userTaskCreatedOrUpdatedEvent.setBpmnProcessVersion( userTaskCreatedEvent.getBpmnProcessVersion() );
        Map<String, String> map = userTaskCreatedEvent.getWorkflowTitle();
        if ( map != null ) {
            userTaskCreatedOrUpdatedEvent.setWorkflowTitle( new LinkedHashMap<String, String>( map ) );
        }
        userTaskCreatedOrUpdatedEvent.setWorkflowId( userTaskCreatedEvent.getWorkflowId() );
        userTaskCreatedOrUpdatedEvent.setSubWorkflowId( userTaskCreatedEvent.getSubWorkflowId() );
        userTaskCreatedOrUpdatedEvent.setBusinessId( userTaskCreatedEvent.getBusinessId() );
        Map<String, String> map1 = userTaskCreatedEvent.getTitle();
        if ( map1 != null ) {
            userTaskCreatedOrUpdatedEvent.setTitle( new LinkedHashMap<String, String>( map1 ) );
        }
        userTaskCreatedOrUpdatedEvent.setBpmnTaskId( userTaskCreatedEvent.getBpmnTaskId() );
        userTaskCreatedOrUpdatedEvent.setTaskDefinition( userTaskCreatedEvent.getTaskDefinition() );
        Map<String, String> map2 = userTaskCreatedEvent.getTaskDefinitionTitle();
        if ( map2 != null ) {
            userTaskCreatedOrUpdatedEvent.setTaskDefinitionTitle( new LinkedHashMap<String, String>( map2 ) );
        }
        userTaskCreatedOrUpdatedEvent.setWorkflowModuleUri( userTaskCreatedEvent.getWorkflowModuleUri() );
        userTaskCreatedOrUpdatedEvent.setTaskProviderApiUriPath( userTaskCreatedEvent.getTaskProviderApiUriPath() );
        userTaskCreatedOrUpdatedEvent.setUiUriPath( userTaskCreatedEvent.getUiUriPath() );
        userTaskCreatedOrUpdatedEvent.setUiUriType( userTaskUiUriTypeToUiUriType( userTaskCreatedEvent.getUiUriType() ) );
        userTaskCreatedOrUpdatedEvent.setAssignee( userTaskCreatedEvent.getAssignee() );
        List<String> list = userTaskCreatedEvent.getCandidateUsers();
        if ( list != null ) {
            userTaskCreatedOrUpdatedEvent.setCandidateUsers( new ArrayList<String>( list ) );
        }
        List<String> list1 = userTaskCreatedEvent.getCandidateGroups();
        if ( list1 != null ) {
            userTaskCreatedOrUpdatedEvent.setCandidateGroups( new ArrayList<String>( list1 ) );
        }
        userTaskCreatedOrUpdatedEvent.setDueDate( userTaskCreatedEvent.getDueDate() );
        userTaskCreatedOrUpdatedEvent.setFollowUpDate( userTaskCreatedEvent.getFollowUpDate() );
        Map<String, Object> map3 = userTaskCreatedEvent.getDetails();
        if ( map3 != null ) {
            userTaskCreatedOrUpdatedEvent.setDetails( new LinkedHashMap<String, Object>( map3 ) );
        }
        userTaskCreatedOrUpdatedEvent.setDetailsFulltextSearch( userTaskCreatedEvent.getDetailsFulltextSearch() );

        userTaskCreatedOrUpdatedEvent.setUpdated( false );

        return userTaskCreatedOrUpdatedEvent;
    }

    @Override
    public UserTaskCreatedOrUpdatedEvent map(UserTaskUpdatedEvent userTaskCreatedEvent) {
        if ( userTaskCreatedEvent == null ) {
            return null;
        }

        UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdatedEvent = new UserTaskCreatedOrUpdatedEvent();

        userTaskCreatedOrUpdatedEvent.setId( userTaskCreatedEvent.getId() );
        userTaskCreatedOrUpdatedEvent.setUserTaskId( userTaskCreatedEvent.getUserTaskId() );
        userTaskCreatedOrUpdatedEvent.setInitiator( userTaskCreatedEvent.getInitiator() );
        userTaskCreatedOrUpdatedEvent.setTimestamp( userTaskCreatedEvent.getTimestamp() );
        userTaskCreatedOrUpdatedEvent.setSource( userTaskCreatedEvent.getSource() );
        userTaskCreatedOrUpdatedEvent.setWorkflowModuleId( userTaskCreatedEvent.getWorkflowModuleId() );
        userTaskCreatedOrUpdatedEvent.setComment( userTaskCreatedEvent.getComment() );
        userTaskCreatedOrUpdatedEvent.setBpmnProcessId( userTaskCreatedEvent.getBpmnProcessId() );
        userTaskCreatedOrUpdatedEvent.setBpmnProcessVersion( userTaskCreatedEvent.getBpmnProcessVersion() );
        Map<String, String> map = userTaskCreatedEvent.getWorkflowTitle();
        if ( map != null ) {
            userTaskCreatedOrUpdatedEvent.setWorkflowTitle( new LinkedHashMap<String, String>( map ) );
        }
        userTaskCreatedOrUpdatedEvent.setWorkflowId( userTaskCreatedEvent.getWorkflowId() );
        userTaskCreatedOrUpdatedEvent.setSubWorkflowId( userTaskCreatedEvent.getSubWorkflowId() );
        userTaskCreatedOrUpdatedEvent.setBusinessId( userTaskCreatedEvent.getBusinessId() );
        Map<String, String> map1 = userTaskCreatedEvent.getTitle();
        if ( map1 != null ) {
            userTaskCreatedOrUpdatedEvent.setTitle( new LinkedHashMap<String, String>( map1 ) );
        }
        userTaskCreatedOrUpdatedEvent.setBpmnTaskId( userTaskCreatedEvent.getBpmnTaskId() );
        userTaskCreatedOrUpdatedEvent.setTaskDefinition( userTaskCreatedEvent.getTaskDefinition() );
        Map<String, String> map2 = userTaskCreatedEvent.getTaskDefinitionTitle();
        if ( map2 != null ) {
            userTaskCreatedOrUpdatedEvent.setTaskDefinitionTitle( new LinkedHashMap<String, String>( map2 ) );
        }
        userTaskCreatedOrUpdatedEvent.setWorkflowModuleUri( userTaskCreatedEvent.getWorkflowModuleUri() );
        userTaskCreatedOrUpdatedEvent.setTaskProviderApiUriPath( userTaskCreatedEvent.getTaskProviderApiUriPath() );
        userTaskCreatedOrUpdatedEvent.setUiUriPath( userTaskCreatedEvent.getUiUriPath() );
        userTaskCreatedOrUpdatedEvent.setUiUriType( userTaskUiUriTypeToUiUriType( userTaskCreatedEvent.getUiUriType() ) );
        userTaskCreatedOrUpdatedEvent.setAssignee( userTaskCreatedEvent.getAssignee() );
        List<String> list = userTaskCreatedEvent.getCandidateUsers();
        if ( list != null ) {
            userTaskCreatedOrUpdatedEvent.setCandidateUsers( new ArrayList<String>( list ) );
        }
        List<String> list1 = userTaskCreatedEvent.getCandidateGroups();
        if ( list1 != null ) {
            userTaskCreatedOrUpdatedEvent.setCandidateGroups( new ArrayList<String>( list1 ) );
        }
        userTaskCreatedOrUpdatedEvent.setDueDate( userTaskCreatedEvent.getDueDate() );
        userTaskCreatedOrUpdatedEvent.setFollowUpDate( userTaskCreatedEvent.getFollowUpDate() );
        Map<String, Object> map3 = userTaskCreatedEvent.getDetails();
        if ( map3 != null ) {
            userTaskCreatedOrUpdatedEvent.setDetails( new LinkedHashMap<String, Object>( map3 ) );
        }
        userTaskCreatedOrUpdatedEvent.setDetailsFulltextSearch( userTaskCreatedEvent.getDetailsFulltextSearch() );

        userTaskCreatedOrUpdatedEvent.setUpdated( true );

        return userTaskCreatedOrUpdatedEvent;
    }

    protected UiUriType userTaskUiUriTypeToUiUriType(UserTaskUiUriType userTaskUiUriType) {
        if ( userTaskUiUriType == null ) {
            return null;
        }

        UiUriType uiUriType;

        switch ( userTaskUiUriType ) {
            case EXTERNAL: uiUriType = UiUriType.EXTERNAL;
            break;
            case WEBPACK_MF_REACT: uiUriType = UiUriType.WEBPACK_MF_REACT;
            break;
            default: throw new IllegalArgumentException( "Unexpected enum constant: " + userTaskUiUriType );
        }

        return uiUriType;
    }
}
