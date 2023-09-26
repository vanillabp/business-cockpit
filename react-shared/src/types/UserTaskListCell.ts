import { FC } from "react";
import { UserTask } from "@vanillabp/bc-official-gui-client";
import { BcUserTask, Column } from "./index.js";
import { DefaultListCellAwareProps } from "../components";

interface UserTaskListCellProps extends DefaultListCellAwareProps<BcUserTask> {
}

export type UserTaskListCell = FC<UserTaskListCellProps>;

export type ColumnsOfUserTaskFunction = (userTask: UserTask) => Column[] | undefined;
