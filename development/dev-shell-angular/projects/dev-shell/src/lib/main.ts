import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './dev-shell.config';
import { DevShellComponent } from './dev-shell.component';
import { ApplicationRef, Type } from "@angular/core";
import { BcUserTask, BcWorkflow } from "@vanillabp/bc-shared";

type DevShellConfigFunction = (
  officialApiUri: string,
  userTaskForm: Type<{ userTask: BcUserTask }>,
  workFlowPage: Type<{ workflow: BcWorkflow }>
) => Promise<ApplicationRef>;

// @ts-ignore
const bootstrapDevShell: DevShellConfigFunction = (officalApiUri, userTaskForm, workFlowPage) => {
  return bootstrapApplication(DevShellComponent, appConfig(officalApiUri, userTaskForm, workFlowPage));
}

export { bootstrapDevShell }
