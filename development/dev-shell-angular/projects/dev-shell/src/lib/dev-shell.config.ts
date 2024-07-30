import { ApplicationConfig, EnvironmentProviders, Provider, Type } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './dev-shell.routes';
import { officialGuiClientProvider } from "./official-gui-client-provider";
import { BcUserTask, BcWorkflow, BcWorkflowModule, ToastFunction } from "@vanillabp/bc-shared";

export type AppConfigFunction = (
  officialApiUri: string,
  userTaskForm: Type<{ userTask: BcUserTask }>,
  workFlowPage: Type<{ workflow: BcWorkflow }>,
  additionalComponents?: Record<string, Type<{ workflowModule: BcWorkflowModule, toast: ToastFunction }>>,
  providers?: Array<Provider | EnvironmentProviders>,
) => ApplicationConfig;

export const appConfig: AppConfigFunction = (
  officialApiUri,
  userTaskForm,
  workFlowPage,
  additionalComponents,
  providers = []) => {
  return { providers: [
      provideRouter(routes(userTaskForm, workFlowPage, additionalComponents), withComponentInputBinding()),
      officialGuiClientProvider.officialTasklistApi(officialApiUri),
      officialGuiClientProvider.officialWorkflowlistApi(officialApiUri),
      ...providers
    ] }
};
