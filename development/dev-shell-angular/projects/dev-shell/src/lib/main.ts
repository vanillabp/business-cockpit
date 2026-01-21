import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './dev-shell.config';
import { DevShellComponent } from './dev-shell.component';
import { ApplicationRef, EnvironmentProviders, Provider, Type } from '@angular/core';
import { BcUserTask, BcWorkflow, BcWorkflowModule } from '@vanillabp/bc-types';

type DevShellConfigFunction = (
  officialApiUri: string,
  userTaskForm: Type<{ userTask: BcUserTask }>,
  workFlowPage: Type<{ workflow: BcWorkflow }>,
  additionalComponents?: Record<string, Type<{ workflowModule: BcWorkflowModule }>>,
  providers?: Array<Provider | EnvironmentProviders>
) => Promise<ApplicationRef>;

// @ts-ignore
export const bootstrapDevShell: DevShellConfigFunction = (
  officalApiUri,
  userTaskForm,
  workFlowPage,
  additionalComponents,
  providers,
) => {
  return bootstrapApplication(DevShellComponent, appConfig(officalApiUri, userTaskForm, workFlowPage, additionalComponents, providers));
};
