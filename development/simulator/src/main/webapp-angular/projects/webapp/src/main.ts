import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';

import { appConfig } from '@vanillabp/bc-dev-shell-angular';
import { UserTaskFormComponent, WorkflowPageComponent } from '@library';
import { OfficialTasklistApi } from "@vanillabp/bc-official-gui-client";

const api = new OfficialTasklistApi();

bootstrapApplication(AppComponent, appConfig("/official-api/v1", UserTaskFormComponent, WorkflowPageComponent))
  .catch((err) => console.error(err));
