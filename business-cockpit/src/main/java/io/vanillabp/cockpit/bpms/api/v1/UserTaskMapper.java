package io.vanillabp.cockpit.bpms.api.v1;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import io.vanillabp.cockpit.tasklist.model.UserTask;

@Mapper
public interface UserTaskMapper {

    @Mapping(target = "version", ignore = true)
    UserTask toModel(UserTaskCreatedEvent event);
    
}
