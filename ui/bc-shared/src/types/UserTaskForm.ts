import { FC } from "react";
import { BcUserTask } from "./BcUserTask";

interface UserTaskFormProps {
  userTask: BcUserTask;
}

export type UserTaskForm = FC<UserTaskFormProps>;
