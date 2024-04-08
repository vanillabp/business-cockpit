import {Component, Input, OnInit} from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UserTask } from "../user-task-form/user-task-form.component";

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
    title: { [key: string]: string };
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
    details?: { [key: string]: any };
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

export interface WrapWorkflow extends Workflow {
    navigateToWorkflow: string;
    getUserTasks: string;
}

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

@Component({
    selector: "lib-workflow-page",
    standalone: true,
    imports: [],
    templateUrl: "./workflow-page.component.html",
    styleUrl: "./workflow-page.component.css"
})
export class WorkflowPageComponent implements OnInit {
    @Input() workflowProps: string = "";
    workflow: BcWorkflow | WrapWorkflow | null = null;

    constructor(private activatedRoute: ActivatedRoute) {
    }

    ngOnInit(): void {
        this.activatedRoute.data.subscribe(({ workflow }) => {
            this.workflow = workflow;
        })
        
        try {
            this.workflow = JSON.parse(this.workflowProps);
        } catch (e) {
            console.error("error: ", e);
        }
    }

    navigateToWorkflow(): void {
        if (!this.workflow || !this.workflow.navigateToWorkflow) return;
        if (typeof this.workflow.navigateToWorkflow != "string") {
            this.workflow.navigateToWorkflow();
            return;
        }

        document.dispatchEvent(
            new CustomEvent(this.workflow.navigateToWorkflow, {})
        );
    }
}
