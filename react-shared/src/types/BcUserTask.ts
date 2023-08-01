import { UserTask } from "@vanillabp/bc-official-gui-client";

export type OpenUserTaskFunction = () => void;
export type OpenWorkflowFunction = () => void;

export interface BcUserTask extends UserTask {
  
  open: OpenUserTaskFunction;
  navigateToWorkflow: OpenWorkflowFunction;
  
};
