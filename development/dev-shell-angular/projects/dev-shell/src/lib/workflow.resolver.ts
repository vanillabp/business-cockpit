import { ResolveFn } from '@angular/router';
import { Observable } from 'rxjs';
import { inject } from '@angular/core';
import { BcWorkflow } from "@vanillabp/bc-shared";
import { DevShellAppContextService } from "./dev-shell-app-context.service";

export const workflowResolver: ResolveFn<Observable<BcWorkflow | null>> = (route, state, userTaskAppContextService: DevShellAppContextService = inject(DevShellAppContextService)) => {
  return userTaskAppContextService.loadWorkflow(route.paramMap.get("workflowId")!)
};
