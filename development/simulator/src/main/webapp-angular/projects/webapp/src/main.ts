import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { appConfig } from '@vanillabp/bc-dev-shell-angular';
import { HeaderComponent, UserTaskFormComponent, WorkflowPageComponent } from '@library';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig('/official-api/v1', UserTaskFormComponent, WorkflowPageComponent,
    {'header': HeaderComponent}, [provideHttpClient()]))
    .catch((err) => console.error(err));
