package io.vanillabp.cockpit.api.v1;

import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedEvent;
import io.vanillabp.cockpit.uerstasks.model.UserTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserTaskMapper {

    @Mapping(target = "version", ignore = true)
    UserTask toModel(UserTaskCreatedEvent event);
    
}
