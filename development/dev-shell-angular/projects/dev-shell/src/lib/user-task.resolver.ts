import { ResolveFn } from '@angular/router';
import { UserTask, UserTaskAppContextService } from './user-task-app-context.service';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';

export const userTaskResolver: ResolveFn<Observable<UserTask | null>> = (route, state, userTaskAppContextService: UserTaskAppContextService = inject(UserTaskAppContextService)) => {
  return userTaskAppContextService.loadUserTask("/official-api/v1", route.paramMap.get("userTaskId")!)
};
