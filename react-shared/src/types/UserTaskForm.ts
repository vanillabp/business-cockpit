import { FC } from "react";
import { UserTask } from "@vanillabp/bc-official-gui-client";

interface UserTaskFormProps {
  userTask: UserTask;
}

export type UserTaskForm = FC<UserTaskFormProps>;
