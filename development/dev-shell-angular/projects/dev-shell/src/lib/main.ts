import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './dev-shell.config';
import { DevShellComponent } from './dev-shell.component';
import {ApplicationRef, Type} from "@angular/core";


type DevShellConfigFunction = (userTaskForm: Type<{ userProps: string }>, workFlowPage: Type<{ workflowProps: string }>) => Promise<ApplicationRef>;
const bootstrapDevShell: DevShellConfigFunction = (userTaskForm, workFlowPage) => {
  return bootstrapApplication(DevShellComponent, appConfig(userTaskForm, workFlowPage));
}

export { bootstrapDevShell }