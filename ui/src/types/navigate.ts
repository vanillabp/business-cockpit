import { UserTask, Workflow } from "@vanillabp/bc-official-gui-client";

export type OpenTaskFunction = (
    userTask: UserTask
) => void;

export type NavigateToWorkflowFunction = (
    workflowDefinition: UserTask | Workflow
) => void;
