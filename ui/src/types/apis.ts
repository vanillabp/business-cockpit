import { WakeupSseCallback } from "@vanillabp/bc-shared";
import { OfficialTasklistApi, OfficialWorkflowlistApi } from "@vanillabp/bc-official-gui-client";
import { MutableRefObject } from "react";

export type WorkflowlistApiHook = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>) => OfficialWorkflowlistApi;

export type TasklistApiHook = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>) => OfficialTasklistApi;
