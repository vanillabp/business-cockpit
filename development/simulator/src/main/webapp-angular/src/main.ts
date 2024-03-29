import { bootstrapApplication } from '@angular/platform-browser';
// import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

import { appConfig } from '@vanillabp/bc-dev-shell-angular';
import { UserTaskFormComponent } from './app/user-task-form/user-task-form.component';
import { WorkflowPageComponent } from './app/workflow-page/workflow-page.component';

bootstrapApplication(AppComponent, appConfig(UserTaskFormComponent, WorkflowPageComponent))
  .catch((err) => console.error(err));
