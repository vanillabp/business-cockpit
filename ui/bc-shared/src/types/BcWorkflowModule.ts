import type { UiUriType } from "@vanillabp/bc-official-gui-client";

export interface BcWorkflowModule {

  /**
   * The workflow module of this usertask
   * @type {string}
   * @memberof UserTask
   */
  workflowModuleId: string;
  
  /**
   * An URI as an entrypoint URI for UI components. Maybe a technical URL (e.g. for WEBPACK) or an URL targeting a human readable form (e.g. EXTERNAL)
   * @type {string}
   * @memberof UserTask
   */
  uiUri: string;
  /**
   *
   * @type {UiUriType}
   * @memberof UserTask
   */
  uiUriType: UiUriType;
  /**
   * An URI pointing to the workflow-module's own API (maybe used by user-task forms)
   * @type {string}
   * @memberof UserTask
   */
  workflowModuleUri: string;

}