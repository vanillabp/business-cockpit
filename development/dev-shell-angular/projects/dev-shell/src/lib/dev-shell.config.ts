import { ApplicationConfig, Type } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './dev-shell.routes';
import { officialGuiClientProvider } from "./official-gui-client-provider";
import { BcUserTask, BcWorkflow } from "@vanillabp/bc-shared";

export interface AppConfigParamsFunction<R> {
  (officialApiUri: string, userTaskForm: Type<{ userTask: BcUserTask }>, workFlowPage: Type<{ workflow: BcWorkflow }>): R;
}
export type AppConfigFunction = AppConfigParamsFunction<ApplicationConfig>;

export const appConfig: AppConfigFunction = (officialApiUri, userTaskForm, workFlowPage) => {
  return { providers: [
      provideRouter(routes(userTaskForm, workFlowPage), withComponentInputBinding()),
      officialGuiClientProvider.officialTasklistApi(officialApiUri),
      officialGuiClientProvider.officialWorkflowlistApi(officialApiUri)
    ] }
};
