import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';

import { appConfig  } from '@vanillabp/bc-dev-shell-angular';
import { UserTaskFormComponent } from '../../library/user-task-form/user-task-form.component';
import { WorkflowPageComponent } from '../../library/workflow-page/workflow-page.component';

bootstrapApplication(AppComponent, appConfig(UserTaskFormComponent, WorkflowPageComponent))
  .catch((err) => console.error(err));
