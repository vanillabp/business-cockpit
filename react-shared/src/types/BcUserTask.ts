import { UserTask } from "@vanillabp/bc-official-gui-client";

export type OpenUserTaskFunction = () => void;

export interface BcUserTask extends UserTask {
  
  open: OpenUserTaskFunction;
  
};
