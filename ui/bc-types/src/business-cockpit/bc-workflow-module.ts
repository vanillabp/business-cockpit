export interface BcWorkflowModule {
  /**
   * The workflow module of this usertask
   * @type {string}
   * @memberof UserTask
   */
  workflowModuleId: string;

  /**
   * Where the user interface of this item is found, the way the workflow module reported it. The
   * cockpit hands the value out unchanged, and the user interface builds the address from it.
   * @type {string}
   * @memberof UserTask
   */
  uiUri: string;
  /**
   * Which kind of user interface the workflow module brings. The cockpit carries the value and
   * does not read it, so this is any string the module and the user interface agreed on.
   * @type {string}
   * @memberof UserTask
   */
  uiUriType: string;
  /**
   * An URI pointing to the workflow-module's own API (maybe used by user-task forms)
   * @type {string}
   * @memberof UserTask
   */
  workflowModuleUri: string;
}
