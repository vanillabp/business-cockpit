import { ResolveFn } from '@angular/router';
import { Observable } from 'rxjs';
import { inject } from '@angular/core';
import { DevShellAppContextService } from "./dev-shell-app-context.service";
import { BcWorkflow } from '@vanillabp/bc-types';

export const workflowResolver: ResolveFn<Observable<BcWorkflow | null>> = (route, state, userTaskAppContextService: DevShellAppContextService = inject(DevShellAppContextService)) => {
  return userTaskAppContextService.loadWorkflow(route.paramMap.get("workflowId")!)
};
