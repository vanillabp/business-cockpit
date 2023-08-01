import { FC } from "react";
import { UserTask } from "@vanillabp/bc-official-gui-client";
import { BcUserTask, ListItemStatus } from "./index.js";

interface Title {
  [key: string]: string;
}

export interface Column {
  title: Title;
  path: string;
  priority: number;
  width: string;
};

interface ListItem {
  id: string;
  number: number;
  data: BcUserTask;
  status: ListItemStatus;
  selected: boolean;
};

export interface DefaultUserTaskListCellProps {
  item: ListItem;
  column: Column;
}

interface UserTaskListCellProps extends DefaultUserTaskListCellProps {
  defaultCell: FC<DefaultUserTaskListCellProps>;
}

export type UserTaskListCell = FC<UserTaskListCellProps>;

export type ColumnsOfUserTaskFunction = (userTask: UserTask) => Column[] | undefined;
