import { FC } from "react";
import { Workflow } from "@vanillabp/bc-official-gui-client";
import { Column, ListItemStatus } from "./index.js";

interface Title {
  [key: string]: string;
}

interface ListItem {
  id: string;
  number: number;
  data: Workflow;
  status: ListItemStatus;
  selected: boolean;
};

export interface DefaultWorkflowListCellProps {
  item: ListItem;
  column: Column;
}

interface WorkflowCellProps extends DefaultWorkflowListCellProps {
  defaultCell: FC<DefaultWorkflowListCellProps>;
}

export type WorkflowListCell = FC<WorkflowCellProps>;
