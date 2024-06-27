import { FC } from "react";
import { ToastFunction } from "../components";

export interface WorkflowModule {
  id: string;
  version: number;
  uri: string;
}

export interface WorkflowModuleComponentProps {
  workflowModule: WorkflowModule,
  toast: ToastFunction,
}

export type WorkflowModuleComponent = FC<WorkflowModuleComponentProps>;
