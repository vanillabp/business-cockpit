package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = MappingConstants.ComponentModel.DEFAULT)
public abstract class ProtobufUserTaskMapper {

    private static final String DETAILS_MAPPING = "detailsMapping";

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetRoles", ignore = true)
    @Mapping(target = "candidateUsers", source = "candidateUsersList")
    @Mapping(target = "candidateGroups", source = "candidateGroupsList")
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toNewTask(UserTaskCreatedOrUpdatedEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetRoles", ignore = true)
    @Mapping(target = "candidateUsers", source = "candidateUsersList")
    @Mapping(target = "candidateGroups", source = "candidateGroupsList")
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toUpdatedTask(UserTaskCreatedOrUpdatedEvent event, @MappingTarget UserTask result);

    public OffsetDateTime map(Timestamp value) {
        return ProtobufHelper.map(value);
    }

    public List<String> map(ProtocolStringList stringList){
        return stringList.stream().toList();
    }

    @Named(DETAILS_MAPPING)
    protected Map<String, Object> map(
            final DetailsMap detailsMap) {
        return DetailsMapper.mapMapValue(detailsMap);
    }

}
