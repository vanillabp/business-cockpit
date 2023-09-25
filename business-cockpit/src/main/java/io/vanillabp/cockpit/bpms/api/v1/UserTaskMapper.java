package io.vanillabp.cockpit.bpms.api.v1;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public abstract class UserTaskMapper {

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    public abstract UserTask toNewTask(UserTaskCreatedOrUpdatedEvent event);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    public abstract UserTask toUpdatedTask(UserTaskCreatedOrUpdatedEvent event, @MappingTarget UserTask result);
    
}
