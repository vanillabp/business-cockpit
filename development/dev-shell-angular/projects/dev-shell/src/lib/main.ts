import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig, AppConfigParamsFunction } from './dev-shell.config';
import { DevShellComponent } from './dev-shell.component';
import { ApplicationRef } from "@angular/core";

type DevShellConfigFunction = AppConfigParamsFunction<Promise<ApplicationRef>>;

// @ts-ignore
const bootstrapDevShell: DevShellConfigFunction = (officalApiUri, userTaskForm, workFlowPage) => {
  return bootstrapApplication(DevShellComponent, appConfig(officalApiUri, userTaskForm, workFlowPage));
}

export { bootstrapDevShell }
