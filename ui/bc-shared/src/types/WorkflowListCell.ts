import { FC } from "react";
import { Workflow } from "@vanillabp/bc-official-gui-client";
import { BcWorkflow, Column } from "./index.js";
import { DefaultListCellAwareProps } from "src/components/DefaultListCell.js";

interface WorkflowCellProps extends DefaultListCellAwareProps<BcWorkflow> {
}

export type WorkflowListCell = FC<WorkflowCellProps>;

export type ColumnsOfWorkflowFunction = (workflow: Workflow) => Column[] | undefined;
