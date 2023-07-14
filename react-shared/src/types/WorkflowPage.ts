import { FC } from "react";
import { Workflow } from "@vanillabp/bc-official-gui-client";

interface WorkflowPageProps {
  workflow: Workflow;
}

export type WorkflowPage = FC<WorkflowPageProps>;
