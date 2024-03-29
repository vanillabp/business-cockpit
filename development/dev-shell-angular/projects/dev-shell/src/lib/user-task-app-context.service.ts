import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, catchError, firstValueFrom, map, of } from 'rxjs';

export interface UserTask {
  /**
   * user task id
   * @type {string}
   * @memberof UserTask
   */
  id: string;
  /**
   * revision of the usertask record
   * @type {number}
   * @memberof UserTask
   */
  version?: number;
  /**
   * The user who triggered the update. Null if the update is done by the system.
   * @type {string}
   * @memberof UserTask
   */
  initiator?: string;
  /**
   * The time the task was created
   * @type {Date}
   * @memberof UserTask
   */
  createdAt: Date;
  /**
   * The time the task was updated
   * @type {Date}
   * @memberof UserTask
   */
  updatedAt: Date;
  /**
   * The time the task was ended
   * @type {Date}
   * @memberof UserTask
   */
  endedAt?: Date;
  /**
   * The workflow module of this usertask
   * @type {string}
   * @memberof UserTask
   */
  workflowModule: string;
  /**
   *
   * @type {string}
   * @memberof UserTask
   */
  comment?: string;
  /**
   * BPMN process ID
   * @type {string}
   * @memberof UserTask
   */
  bpmnProcessId: string;
  /**
   * Version of the BPMN process
   * @type {string}
   * @memberof UserTask
   */
  bpmnProcessVersion?: string;
  /**
   * BPMN process title
   * @type {{ [key: string]: string; }}
   * @memberof UserTask
   */
  workflowTitle?: { [key: string]: string; };
  /**
   * The unique key of the workflow
   * @type {string}
   * @memberof UserTask
   */
  workflowId?: string;
  /**
   * The natural ID of the workflow (e.g. order-id)
   * @type {string}
   * @memberof UserTask
   */
  businessId?: string;
  /**
   * The rendered title of the user-task (may contain specific data)
   * @type {{ [key: string]: string; }}
   * @memberof UserTask
   */
  title: { [key: string]: string; };
  /**
   * The BPMN user task's ID
   * @type {string}
   * @memberof UserTask
   */
  bpmnTaskId?: string;
  /**
   * The task's formkey
   * @type {string}
   * @memberof UserTask
   */
  taskDefinition: string;
  /**
   * The generic title of the user-task (must not contain specific data)
   * @type {{ [key: string]: string; }}
   * @memberof UserTask
   */
  taskDefinitionTitle?: { [key: string]: string; };
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
  /**
   *
   * @type {string}
   * @memberof UserTask
   */
  assignee?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof UserTask
   */
  candidateUsers?: Array<string>;
  /**
   *
   * @type {Array<string>}
   * @memberof UserTask
   */
  candidateGroups?: Array<string>;
  /**
   *
   * @type {Date}
   * @memberof UserTask
   */
  dueDate?: Date;
  /**
   *
   * @type {Date}
   * @memberof UserTask
   */
  followUpDate?: Date;
  /**
   * Properties for individual searches
   * @type {{ [key: string]: any; }}
   * @memberof UserTask
   */
  details?: { [key: string]: any; };
  /**
   * List of words for fulltext searching details
   * @type {string}
   * @memberof UserTask
   */
  detailsFulltextSearch?: string;
  /**
   *
   * @type {Date}
   * @memberof UserTask
   */
  read?: Date;
}

export interface Workflow {
  /**
   * workflow id
   * @type {string}
   * @memberof Workflow
   */
  id: string;
  /**
   * revision of the usertask record
   * @type {number}
   * @memberof Workflow
   */
  version?: number;
  /**
   * The user who triggered the update. Null if the update is done by the system.
   * @type {string}
   * @memberof Workflow
   */
  initiator?: string;
  /**
   * The time the task was created
   * @type {Date}
   * @memberof Workflow
   */
  createdAt: Date;
  /**
   * The time the task was updated
   * @type {Date}
   * @memberof Workflow
   */
  updatedAt: Date;
  /**
   * The time the task was ended
   * @type {Date}
   * @memberof Workflow
   */
  endedAt?: Date;
  /**
   * The workflow module of this usertask
   * @type {string}
   * @memberof Workflow
   */
  workflowModule: string;
  /**
   *
   * @type {string}
   * @memberof Workflow
   */
  comment?: string;
  /**
   * BPMN process ID
   * @type {string}
   * @memberof Workflow
   */
  bpmnProcessId: string;
  /**
   * Version of the BPMN process and tag
   * @type {string}
   * @memberof Workflow
   */
  bpmnProcessVersion?: string;
  /**
   * The natural ID of the workflow (e.g. order-id)
   * @type {string}
   * @memberof Workflow
   */
  businessId?: string;
  /**
   * The rendered title of the user-task (may contain specific data)
   * @type {{ [key: string]: string; }}
   * @memberof Workflow
   */
  title: { [key: string]: string; };
  /**
   * An URI as an entrypoint URI for UI components. Maybe a technical URL (e.g. for WEBPACK) or an URL targeting a human readable form (e.g. EXTERNAL)
   * @type {string}
   * @memberof Workflow
   */
  uiUri: string;
  /**
   *
   * @type {UiUriType}
   * @memberof Workflow
   */
  uiUriType: UiUriType;
  /**
   * An URI pointing to the workflow-modules's own API (maybe used by workflow pages)
   * @type {string}
   * @memberof Workflow
   */
  workflowModuleUri: string;
  /**
   *
   * @type {Array<string>}
   * @memberof Workflow
   */
  accessibleToUsers?: Array<string>;
  /**
   *
   * @type {Array<string>}
   * @memberof Workflow
   */
  accessibleToGroups?: Array<string>;
  /**
   * Properties for individual searches
   * @type {{ [key: string]: any; }}
   * @memberof Workflow
   */
  details?: { [key: string]: any; };
  /**
   * List of words for fulltext searching details
   * @type {string}
   * @memberof Workflow
   */
  detailsFulltextSearch?: string;
}

export const UiUriType = {
  External: "EXTERNAL",
  WebpackMfReact: "WEBPACK_MF_REACT"
} as const;

export type UiUriType = (typeof UiUriType)[keyof typeof UiUriType];

export type OpenUserTaskFunction = () => void;
export type OpenWorkflowFunction = () => void;
export type UnassignFunction = (userId: string) => void;

export interface BcUserTask extends UserTask {
  open: OpenUserTaskFunction;
  navigateToWorkflow: OpenWorkflowFunction;
  unassign: UnassignFunction;
}

export type GetUserTasksFunction = (
  activeOnly: boolean,
  limitListAccordingToCurrentUsersPermissions: boolean,
) => Promise<Array<BcUserTask>>;

export interface BcWorkflow extends Workflow {
  navigateToWorkflow: OpenWorkflowFunction
  getUserTasks: GetUserTasksFunction;
}

@Injectable({
  providedIn: 'root'
})
export class UserTaskAppContextService {
  private userTaskSubject = new BehaviorSubject<BcUserTask | null>(null);
  public userTask = this.userTaskSubject.asObservable();

  private workflowSubject = new BehaviorSubject<BcWorkflow | null>(null);
  public workflow = this.workflowSubject.asObservable();

  constructor(private http: HttpClient) {
  }

  loadUserTask(apiUrl: string, userTaskId: string) {
    const url = `${apiUrl}/usertask/${userTaskId}`

    return this.http.get<UserTask>(url).pipe(
      catchError(err => {
        console.error("Error loading task: ", err)
        return of(null)
      }), map(task => {
        return {
          ...task,
          open: () => openTask(task?.id!),
          navigateToWorkflow: () => openWorkflow(task?.workflowId!),
          unassign: userId => {
          },
        } as BcUserTask
      }))
  }

  loadWorkflow(apiUrl: string, workflowId: string) {
    const url = `${apiUrl}/workflow/${workflowId}`

    return this.http.get<Workflow>(url).pipe(
      catchError(err => {
        console.error("Error loading workflow: ", err)
        return of(null)
      }), map(workflow => {
        const getUserTasksFunction: GetUserTasksFunction = async (activeOnly, limitListAccordingToCurrentUsersPermissions) => {
          let obs = this.http.get<Array<UserTask>>(url + `/usertasks?activeOnly=${activeOnly}&llatcup=${limitListAccordingToCurrentUsersPermissions}`)
          return (await firstValueFrom(obs)).map(userTask => ({
            ...userTask,
            open: () => openTask(userTask.id),
            navigateToWorkflow: () => openWorkflow(userTask.workflowId!),
            unassign: userId => {
            },
          }) as BcUserTask);
        }
  
        return {
          ...workflow,
          navigateToWorkflow: () => openWorkflow(workflowId),
          getUserTasks: getUserTasksFunction,
        } as BcWorkflow
      }))
  }
}


const openTask = (userTaskId: string) => window.location.href = `/task/${userTaskId}`;
const openWorkflow = (workflowId: string) => window.location.href = `/workflow/${workflowId}`;
