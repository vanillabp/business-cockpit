package io.vanillabp.cockpit.adapter.common.usertask.rest;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUiUriType;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UiUriType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserTaskRestMapperImpl implements UserTaskRestMapper {

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.UserTaskActivatedEvent map(UserTaskActivatedEvent userTaskActivatedEvent) {
        if ( userTaskActivatedEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskActivatedEvent();

        result.setId( userTaskActivatedEvent.getEventId() );
        result.setUserTaskId( userTaskActivatedEvent.getUserTaskId() );
        result.setInitiator( userTaskActivatedEvent.getInitiator() );
        result.setTimestamp( userTaskActivatedEvent.getTimestamp() );
        result.setSource( userTaskActivatedEvent.getSource() );
        result.setComment( userTaskActivatedEvent.getComment() );

        return result;

    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent map(UserTaskCancelledEvent userTaskCancelledEvent) {
        if ( userTaskCancelledEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent();

        result.setId( userTaskCancelledEvent.getEventId() );
        result.setUserTaskId( userTaskCancelledEvent.getUserTaskId() );
        result.setInitiator( userTaskCancelledEvent.getInitiator() );
        result.setTimestamp( userTaskCancelledEvent.getTimestamp() );
        result.setSource( userTaskCancelledEvent.getSource() );
        result.setWorkflowModuleId( userTaskCancelledEvent.getWorkflowModuleId() );
        result.setComment( userTaskCancelledEvent.getComment() );
        result.setBpmnProcessId( userTaskCancelledEvent.getBpmnProcessId() );
        result.setBpmnProcessVersion( userTaskCancelledEvent.getBpmnProcessVersion() );
        Map<String, String> map = userTaskCancelledEvent.getWorkflowTitle();
        if ( map != null ) {
            result.setWorkflowTitle( new LinkedHashMap<String, String>( map ) );
        }
        result.setWorkflowId( userTaskCancelledEvent.getWorkflowId() );
        result.setSubWorkflowId( userTaskCancelledEvent.getSubWorkflowId() );
        result.setBusinessId( userTaskCancelledEvent.getBusinessId() );
        Map<String, String> map1 = userTaskCancelledEvent.getTitle();
        if ( map1 != null ) {
            result.setTitle( new LinkedHashMap<String, String>( map1 ) );
        }
        result.setBpmnTaskId( userTaskCancelledEvent.getBpmnTaskId() );
        result.setTaskDefinition( userTaskCancelledEvent.getTaskDefinition() );
        Map<String, String> map2 = userTaskCancelledEvent.getTaskDefinitionTitle();
        if ( map2 != null ) {
            result.setTaskDefinitionTitle( new LinkedHashMap<String, String>( map2 ) );
        }
        result.setUiUriPath( userTaskCancelledEvent.getUiUriPath() );
        result.setUiUriType( userTaskUiUriTypeToUiUriType( userTaskCancelledEvent.getUiUriType() ) );
        result.setAssignee( userTaskCancelledEvent.getAssignee() );
        List<String> list = userTaskCancelledEvent.getCandidateUsers();
        if ( list != null ) {
            result.setCandidateUsers( new ArrayList<String>( list ) );
        }
        List<String> list1 = userTaskCancelledEvent.getCandidateGroups();
        if ( list1 != null ) {
            result.setCandidateGroups( new ArrayList<String>( list1 ) );
        }
        List<String> list2 = userTaskCancelledEvent.getExcludedCandidateUsers();
        if ( list2 != null ) {
            result.excludedCandidateUsers( new ArrayList<String>( list2 ) );
        }
        result.setDueDate( userTaskCancelledEvent.getDueDate() );
        result.setFollowUpDate( userTaskCancelledEvent.getFollowUpDate() );
        Map<String, Object> map3 = userTaskCancelledEvent.getDetails();
        if ( map3 != null ) {
            result.setDetails( new LinkedHashMap<String, Object>( map3 ) );
        }
        result.setDetailsFulltextSearch( userTaskCancelledEvent.getDetailsFulltextSearch() );

        result.setUpdated( true );


        return result;

    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent map(UserTaskCompletedEvent userTaskCompletedEvent) {
        if ( userTaskCompletedEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent();

        result.setId( userTaskCompletedEvent.getEventId() );
        result.setUserTaskId( userTaskCompletedEvent.getUserTaskId() );
        result.setInitiator( userTaskCompletedEvent.getInitiator() );
        result.setTimestamp( userTaskCompletedEvent.getTimestamp() );
        result.setSource( userTaskCompletedEvent.getSource() );
        result.setWorkflowModuleId( userTaskCompletedEvent.getWorkflowModuleId() );
        result.setComment( userTaskCompletedEvent.getComment() );
        result.setBpmnProcessId( userTaskCompletedEvent.getBpmnProcessId() );
        result.setBpmnProcessVersion( userTaskCompletedEvent.getBpmnProcessVersion() );
        Map<String, String> map = userTaskCompletedEvent.getWorkflowTitle();
        if ( map != null ) {
            result.setWorkflowTitle( new LinkedHashMap<String, String>( map ) );
        }
        result.setWorkflowId( userTaskCompletedEvent.getWorkflowId() );
        result.setSubWorkflowId( userTaskCompletedEvent.getSubWorkflowId() );
        result.setBusinessId( userTaskCompletedEvent.getBusinessId() );
        Map<String, String> map1 = userTaskCompletedEvent.getTitle();
        if ( map1 != null ) {
            result.setTitle( new LinkedHashMap<String, String>( map1 ) );
        }
        result.setBpmnTaskId( userTaskCompletedEvent.getBpmnTaskId() );
        result.setTaskDefinition( userTaskCompletedEvent.getTaskDefinition() );
        Map<String, String> map2 = userTaskCompletedEvent.getTaskDefinitionTitle();
        if ( map2 != null ) {
            result.setTaskDefinitionTitle( new LinkedHashMap<String, String>( map2 ) );
        }
        result.setUiUriPath( userTaskCompletedEvent.getUiUriPath() );
        result.setUiUriType( userTaskUiUriTypeToUiUriType( userTaskCompletedEvent.getUiUriType() ) );
        result.setAssignee( userTaskCompletedEvent.getAssignee() );
        List<String> list = userTaskCompletedEvent.getCandidateUsers();
        if ( list != null ) {
            result.setCandidateUsers( new ArrayList<String>( list ) );
        }
        List<String> list1 = userTaskCompletedEvent.getCandidateGroups();
        if ( list1 != null ) {
            result.setCandidateGroups( new ArrayList<String>( list1 ) );
        }
        List<String> list2 = userTaskCompletedEvent.getExcludedCandidateUsers();
        if ( list2 != null ) {
            result.excludedCandidateUsers( new ArrayList<String>( list2 ) );
        }
        result.setDueDate( userTaskCompletedEvent.getDueDate() );
        result.setFollowUpDate( userTaskCompletedEvent.getFollowUpDate() );
        Map<String, Object> map3 = userTaskCompletedEvent.getDetails();
        if ( map3 != null ) {
            result.setDetails( new LinkedHashMap<String, Object>( map3 ) );
        }
        result.setDetailsFulltextSearch( userTaskCompletedEvent.getDetailsFulltextSearch() );

        result.setUpdated( true );

        return result;

    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.UserTaskSuspendedEvent map(UserTaskSuspendedEvent userTaskSuspendedEvent) {
        if ( userTaskSuspendedEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskSuspendedEvent();

        result.setId( userTaskSuspendedEvent.getEventId() );
        result.setUserTaskId( userTaskSuspendedEvent.getUserTaskId() );
        result.setInitiator( userTaskSuspendedEvent.getInitiator() );
        result.setTimestamp( userTaskSuspendedEvent.getTimestamp() );
        result.setSource( userTaskSuspendedEvent.getSource() );
        result.setComment( userTaskSuspendedEvent.getComment() );

        return result;

    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent map(UserTaskCreatedEvent userTaskEvent) {
        if ( userTaskEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent();

        result.setId( userTaskEvent.getEventId() );
        result.setUserTaskId( userTaskEvent.getUserTaskId() );
        result.setInitiator( userTaskEvent.getInitiator() );
        result.setTimestamp( userTaskEvent.getTimestamp() );
        result.setSource( userTaskEvent.getSource() );
        result.setWorkflowModuleId( userTaskEvent.getWorkflowModuleId() );
        result.setComment( userTaskEvent.getComment() );
        result.setBpmnProcessId( userTaskEvent.getBpmnProcessId() );
        result.setBpmnProcessVersion( userTaskEvent.getBpmnProcessVersion() );
        Map<String, String> map = userTaskEvent.getWorkflowTitle();
        if ( map != null ) {
            result.setWorkflowTitle( new LinkedHashMap<String, String>( map ) );
        }
        result.setWorkflowId( userTaskEvent.getWorkflowId() );
        result.setSubWorkflowId( userTaskEvent.getSubWorkflowId() );
        result.setBusinessId( userTaskEvent.getBusinessId() );
        Map<String, String> map1 = userTaskEvent.getTitle();
        if ( map1 != null ) {
            result.setTitle( new LinkedHashMap<String, String>( map1 ) );
        }
        result.setBpmnTaskId( userTaskEvent.getBpmnTaskId() );
        result.setTaskDefinition( userTaskEvent.getTaskDefinition() );
        Map<String, String> map2 = userTaskEvent.getTaskDefinitionTitle();
        if ( map2 != null ) {
            result.setTaskDefinitionTitle( new LinkedHashMap<String, String>( map2 ) );
        }
        result.setUiUriPath( userTaskEvent.getUiUriPath() );
        result.setUiUriType( userTaskUiUriTypeToUiUriType( userTaskEvent.getUiUriType() ) );
        result.setAssignee( userTaskEvent.getAssignee() );
        List<String> list = userTaskEvent.getCandidateUsers();
        if ( list != null ) {
            result.setCandidateUsers( new ArrayList<String>( list ) );
        }
        List<String> list1 = userTaskEvent.getCandidateGroups();
        if ( list1 != null ) {
            result.setCandidateGroups( new ArrayList<String>( list1 ) );
        }
        List<String> list2 = userTaskEvent.getExcludedCandidateUsers();
        if ( list2 != null ) {
            result.excludedCandidateUsers( new ArrayList<String>( list2 ) );
        }
        result.setDueDate( userTaskEvent.getDueDate() );
        result.setFollowUpDate( userTaskEvent.getFollowUpDate() );
        Map<String, Object> map3 = userTaskEvent.getDetails();
        if ( map3 != null ) {
            result.setDetails( new LinkedHashMap<String, Object>( map3 ) );
        }
        result.setDetailsFulltextSearch( userTaskEvent.getDetailsFulltextSearch() );

        result.setUpdated( false );

        return result;

    }

    @Override
    public io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent map(UserTaskUpdatedEvent userTaskCreatedEvent) {
        if ( userTaskCreatedEvent == null ) {
            return null;
        }

        final var result = new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent();

        result.setId( userTaskCreatedEvent.getEventId() );
        result.setUserTaskId( userTaskCreatedEvent.getUserTaskId() );
        result.setInitiator( userTaskCreatedEvent.getInitiator() );
        result.setTimestamp( userTaskCreatedEvent.getTimestamp() );
        result.setSource( userTaskCreatedEvent.getSource() );
        result.setWorkflowModuleId( userTaskCreatedEvent.getWorkflowModuleId() );
        result.setComment( userTaskCreatedEvent.getComment() );
        result.setBpmnProcessId( userTaskCreatedEvent.getBpmnProcessId() );
        result.setBpmnProcessVersion( userTaskCreatedEvent.getBpmnProcessVersion() );
        Map<String, String> map = userTaskCreatedEvent.getWorkflowTitle();
        if ( map != null ) {
            result.setWorkflowTitle( new LinkedHashMap<String, String>( map ) );
        }
        result.setWorkflowId( userTaskCreatedEvent.getWorkflowId() );
        result.setSubWorkflowId( userTaskCreatedEvent.getSubWorkflowId() );
        result.setBusinessId( userTaskCreatedEvent.getBusinessId() );
        Map<String, String> map1 = userTaskCreatedEvent.getTitle();
        if ( map1 != null ) {
            result.setTitle( new LinkedHashMap<String, String>( map1 ) );
        }
        result.setBpmnTaskId( userTaskCreatedEvent.getBpmnTaskId() );
        result.setTaskDefinition( userTaskCreatedEvent.getTaskDefinition() );
        Map<String, String> map2 = userTaskCreatedEvent.getTaskDefinitionTitle();
        if ( map2 != null ) {
            result.setTaskDefinitionTitle( new LinkedHashMap<String, String>( map2 ) );
        }
        result.setUiUriPath( userTaskCreatedEvent.getUiUriPath() );
        result.setUiUriType( userTaskUiUriTypeToUiUriType( userTaskCreatedEvent.getUiUriType() ) );
        result.setAssignee( userTaskCreatedEvent.getAssignee() );
        List<String> list = userTaskCreatedEvent.getCandidateUsers();
        if ( list != null ) {
            result.setCandidateUsers( new ArrayList<String>( list ) );
        }
        List<String> list1 = userTaskCreatedEvent.getCandidateGroups();
        if ( list1 != null ) {
            result.setCandidateGroups( new ArrayList<String>( list1 ) );
        }
        List<String> list2 = userTaskCreatedEvent.getExcludedCandidateUsers();
        if ( list2 != null ) {
            result.excludedCandidateUsers( new ArrayList<String>( list2 ) );
        }
        result.setDueDate( userTaskCreatedEvent.getDueDate() );
        result.setFollowUpDate( userTaskCreatedEvent.getFollowUpDate() );
        Map<String, Object> map3 = userTaskCreatedEvent.getDetails();
        if ( map3 != null ) {
            result.setDetails( new LinkedHashMap<String, Object>( map3 ) );
        }
        result.setDetailsFulltextSearch( userTaskCreatedEvent.getDetailsFulltextSearch() );

        result.setUpdated( true );

        return result;

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
