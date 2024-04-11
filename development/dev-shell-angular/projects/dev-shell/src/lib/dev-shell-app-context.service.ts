import { Injectable } from '@angular/core';
import { BehaviorSubject, catchError, map, of } from 'rxjs';
import { OfficialTasklistApi, OfficialWorkflowlistApi, UserTask, Workflow } from "@vanillabp/bc-official-gui-client";
import { BcUserTask, BcWorkflow } from '@vanillabp/bc-shared';
import { fromPromise } from "rxjs/internal/observable/innerFrom";

@Injectable({
  providedIn: 'root'
})
export class DevShellAppContextService {
  private userTaskSubject = new BehaviorSubject<BcUserTask | null>(null);
  public userTask = this.userTaskSubject.asObservable();

  private workflowSubject = new BehaviorSubject<BcWorkflow | null>(null);
  public workflow = this.workflowSubject.asObservable();

  constructor(private tasklistApi: OfficialTasklistApi, private workflowlistApi: OfficialWorkflowlistApi) {
  }

  mapUserTask(userTask: UserTask | null): BcUserTask {
    return {
      ...userTask,
      open: () => alert('Will open task'),
      navigateToWorkflow: () => alert('Will navigate to workflow'),
      unassign: userId => alert(`Unassigned task for user ${ userId }`)
    } as BcUserTask;
  }

  loadUserTask(userTaskId: string) {

    return fromPromise(this.tasklistApi.getUserTask({ userTaskId }))
      .pipe(
        catchError(err => {
          console.error(`Error loading task '${ userTaskId }`, err)
          return of(null)
        }),
        map(this.mapUserTask)
      );

  }

  loadUserTasks(workflowId: string, activeOnly: boolean, llatcup: boolean): Promise<Array<BcUserTask>> {

    return new Promise<Array<BcUserTask>>((resolve, reject) => {
      this.workflowlistApi.getUserTasksOfWorkflow({
          workflowId: workflowId!,
          llatcup, userTasksRequest: {
            mode: activeOnly
              ? "OpenTasks"
              : "All"
          }
        })
        .then(userTasks => {
          resolve(userTasks.map(this.mapUserTask));
        })
        .catch(err => {
          console.error(`Error loading tasks for workflow '${ workflowId }'`, err);
          reject(err);
        });
    })

  }

  mapWorkflow(workflow: Workflow | null): BcWorkflow {
    return {
        ...workflow!,
        navigateToWorkflow: () => alert('Will navigate to workflow'),
        getUserTasks: (activeOnly, limitListAccordingToCurrentUsersPermissions) =>
            this.loadUserTasks(workflow!.id, activeOnly, limitListAccordingToCurrentUsersPermissions)
      };
  }
/*

 */
  loadWorkflow(workflowId: string) {

    return fromPromise(this.workflowlistApi.getWorkflow({ workflowId }))
      .pipe(
        catchError(err => {
          console.error("Error loading workflow: ", err)
          return of(null)
        }),
        map(this.mapWorkflow)
      );

  }

}
