import { ResolveFn } from '@angular/router';
import { DevShellAppContextService } from './dev-shell-app-context.service';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BcUserTask } from "@vanillabp/bc-shared";

export const userTaskResolver: ResolveFn<Observable<BcUserTask | null>> = (route, state, userTaskAppContextService: DevShellAppContextService = inject(DevShellAppContextService)) => {
  return userTaskAppContextService.loadUserTask(route.paramMap.get("userTaskId")!)
};
