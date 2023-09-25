import { FC } from "react";
import { BcWorkflow } from "./BcWorkflow";

interface WorkflowPageProps {
  workflow: BcWorkflow;
}

export type WorkflowPage = FC<WorkflowPageProps>;
