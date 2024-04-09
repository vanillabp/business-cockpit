import { ApplicationConfig, Type } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { BcUserTask, BcWorkflow } from '@vanillabp/bc-shared'
import { provideHttpClient, withFetch } from "@angular/common/http";

import { routes } from './dev-shell.routes';


type AppConfigFunction = (userTaskForm: Type<{ userTask?: BcUserTask }>, workFlowPage: Type<{ workflow?: BcWorkflow }>) => ApplicationConfig;
export const appConfig: AppConfigFunction = (userTaskForm, workFlowPage) => {
  return { providers: [provideRouter(routes(userTaskForm, workFlowPage), withComponentInputBinding()), provideHttpClient(withFetch())] }
};
