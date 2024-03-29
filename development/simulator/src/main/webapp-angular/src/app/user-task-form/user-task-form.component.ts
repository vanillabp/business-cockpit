import {Component, Input, OnInit} from "@angular/core";
import { ActivatedRoute } from "@angular/router";

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
    workflowTitle?: { [key: string]: string };
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
    title: { [key: string]: string };
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
    taskDefinitionTitle?: { [key: string]: string };
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
    details?: { [key: string]: any };
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

export const UiUriType = {
    External: "EXTERNAL",
    WebpackMfReact: "WEBPACK_MF_REACT"
} as const;

export type UiUriType = (typeof UiUriType)[keyof typeof UiUriType];

export interface WrapUserTask extends UserTask {
    open: string;
    navigateToWorkflow: string;
    unassign: string;
}


export type OpenUserTaskFunction = () => void;
export type OpenWorkflowFunction = () => void;
export type UnassignFunction = (userId: string) => void;

export interface BcUserTask extends UserTask {
  open: OpenUserTaskFunction;
  navigateToWorkflow: OpenWorkflowFunction;
  unassign: UnassignFunction;
}

@Component({
    selector: "lib-user-task-form",
    standalone: true,
    imports: [],
    templateUrl: "./user-task-form.component.html",
    styleUrl: "./user-task-form.component.css"
})
export class UserTaskFormComponent implements OnInit {
    @Input() userProps: string = "";
    userTask: BcUserTask | WrapUserTask | null = null;

    constructor(private activatedRoute: ActivatedRoute) {
    }

    ngOnInit(): void {
        this.activatedRoute.data.subscribe(({ userTask }) => {
            this.userTask = userTask;
        })

        try {
            this.userTask = JSON.parse(this.userProps);
        } catch (error) {
            console.error("error:", error);
        }
    }

    open(): void {
        console.log(this.userTask)
        if (!this.userTask || !this.userTask.open) return;
        if (typeof this.userTask.open != "string") {
            this.userTask.open();
            return
        }
        document.dispatchEvent(new CustomEvent(this.userTask.open, {}));
    }

    navigateWorkflow(): void {
        if (!this.userTask || !this.userTask.navigateToWorkflow) return;
        if (typeof this.userTask.navigateToWorkflow != "string") {
            this.userTask.navigateToWorkflow();
            return
        }
        document.dispatchEvent(
            new CustomEvent(this.userTask.navigateToWorkflow, {})
        );
    }
}
