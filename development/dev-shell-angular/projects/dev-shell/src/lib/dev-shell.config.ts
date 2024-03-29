import { ApplicationConfig, Type } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './dev-shell.routes';
import { provideHttpClient, withFetch } from "@angular/common/http";

type AppConfigFunction = (userTaskForm: Type<{ userProps: string }>, workFlowPage: Type<{ workflowProps: string }>) => ApplicationConfig;
export const appConfig: AppConfigFunction = (userTaskForm, workFlowPage) => {
  return { providers: [provideRouter(routes(userTaskForm, workFlowPage)), provideHttpClient(withFetch())] }
};
