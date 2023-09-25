import { FC } from "react";
import { UserTask } from "@vanillabp/bc-official-gui-client";
import { BcUserTask, Column } from "./index.js";
import { DefaultListCellProps } from "src/components/DefaultListCell.js";

interface UserTaskListCellProps extends DefaultListCellProps<BcUserTask> {
  defaultCell: FC<DefaultListCellProps<BcUserTask>>;
}

export type UserTaskListCell = FC<UserTaskListCellProps>;

export type ColumnsOfUserTaskFunction = (userTask: UserTask) => Column[] | undefined;
