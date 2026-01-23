import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';

// import { appConfig } from '@vanillabp/bc-dev-shell-angular';
import { HeaderComponent, UserTaskFormComponent, WorkflowPageComponent } from '@library';
import { provideHttpClient } from "@angular/common/http";
//
// const config = appConfig("/official-api/v1", UserTaskFormComponent, WorkflowPageComponent, fixme angular 20
//     { 'header': HeaderComponent }, [ provideHttpClient() ]);

bootstrapApplication(AppComponent)
  .catch((err) => console.error(err));
