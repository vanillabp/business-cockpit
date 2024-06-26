import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';

import { appConfig } from '@vanillabp/bc-dev-shell-angular';
import { HeaderComponent, UserTaskFormComponent, WorkflowPageComponent } from '@library';
import { provideHttpClient } from "@angular/common/http";

bootstrapApplication(AppComponent, appConfig("/official-api/v1", UserTaskFormComponent, WorkflowPageComponent,
    { 'header': HeaderComponent }, [ provideHttpClient() ]))
  .catch((err) => console.error(err));
