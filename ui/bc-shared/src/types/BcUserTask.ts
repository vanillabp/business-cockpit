import { UserTask } from "@vanillabp/bc-official-gui-client";

export type OpenUserTaskFunction = () => void;
export type OpenWorkflowFunction = () => void;
export type AssignFunction = (userId: string) => void;
export type UnassignFunction = (userId: string) => void;

export interface BcUserTask extends UserTask {
  
  open: OpenUserTaskFunction;
  navigateToWorkflow: OpenWorkflowFunction;
  assign: AssignFunction;
  unassign: UnassignFunction;

};
