import { ResolveFn } from '@angular/router';
import { UserTaskAppContextService, Workflow } from './user-task-app-context.service';
import { Observable } from 'rxjs';
import { inject } from '@angular/core';


export const workflowResolver: ResolveFn<Observable<Workflow | null>> = (route, state, userTaskAppContextService: UserTaskAppContextService = inject(UserTaskAppContextService)) => {
  return userTaskAppContextService.loadWorkflow("/official-api/v1", route.paramMap.get("workflowId")!)
};
