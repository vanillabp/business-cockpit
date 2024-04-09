import { bootstrapApplication } from '@angular/platform-browser';
import {ApplicationRef, Type} from "@angular/core";

import { BcUserTask, BcWorkflow } from '@vanillabp/bc-shared';

import { appConfig } from './dev-shell.config';
import { DevShellComponent } from './dev-shell.component';


type DevShellConfigFunction = (userTaskForm: Type<{ userTask?: BcUserTask }>, workFlowPage: Type<{ workflow: BcWorkflow }>) => Promise<ApplicationRef>;
const bootstrapDevShell: DevShellConfigFunction = (userTaskForm, workFlowPage) => {
  return bootstrapApplication(DevShellComponent, appConfig(userTaskForm, workFlowPage));
}

export { bootstrapDevShell }