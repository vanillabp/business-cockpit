package io.vanillabp.cockpit.tasklist.api.v1;

import java.util.List;

import org.mapstruct.Mapper;

import io.vanillabp.cockpit.gui.api.v1.UserTask;

@Mapper
public interface GuiApiMapper {

	UserTask toApi(
			io.vanillabp.cockpit.tasklist.model.UserTask userTask);

	List<UserTask> toApi(
			List<io.vanillabp.cockpit.tasklist.model.UserTask> userTasks);
	
}
