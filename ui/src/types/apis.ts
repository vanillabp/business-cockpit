import { WakeupSseCallback } from "@vanillabp/bc-shared";
import { OfficialTasklistApi, OfficialWorkflowlistApi } from "@vanillabp/bc-official-gui-client";

/**
 * This helps to initialize the given API hook late by
 * first passing the empty reference and setting
 * 'current' property later.
 */
export interface WakeupSseCallbackReference {
  current: WakeupSseCallback | undefined;
}

export type WorkflowlistApiHook = (wakeupSseCallback?: WakeupSseCallbackReference) => OfficialWorkflowlistApi;

export type TasklistApiHook = (wakeupSseCallback?: WakeupSseCallbackReference) => OfficialTasklistApi;
