package io.vanillabp.cockpit.bpms.api.v1;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import io.vanillabp.cockpit.tasklist.model.UserTask;

@Mapper
public interface UserTaskMapper {

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    UserTask toModel(UserTaskCreatedOrUpdatedEvent event);

}
