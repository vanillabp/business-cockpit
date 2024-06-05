import { FC } from "react";
import { BcWorkflowModule } from "./BcWorkflowModule";
import { ToastFunction } from "../components";

interface WorkflowModuleComponentProps {
  workflowModule: BcWorkflowModule,
  toast: ToastFunction,
}

export type WorkflowModuleComponent = FC<WorkflowModuleComponentProps>;
