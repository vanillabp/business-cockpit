import { createApplication } from "@angular/platform-browser";
// import { appConfig } from "./main.config";
import { createCustomElement } from "@angular/elements";
import { ApplicationRef } from "@angular/core";
import { LibraryComponent } from "../../library/src/lib/library.component";
import { UserTaskFormComponent } from "../../library/src/lib/user-task-form/user-task-form.component";
import { WorkflowPageComponent } from "../../library/src/lib/workflow-page/workflow-page.component";
import { appConfig } from "@vanillabp/bc-dev-shell-angular";

(async () => {
    const app: ApplicationRef = await createApplication(
        appConfig(
            "/official-api/v1",
            UserTaskFormComponent,
            WorkflowPageComponent,
            {},
            []
        )
    );

    // Define Web Components
    const libraryComponent = createCustomElement(LibraryComponent, {
        injector: app.injector
    });

    const userTaskFormComponent = createCustomElement(UserTaskFormComponent, {
        injector: app.injector
    });

    const workflowPageComponent = createCustomElement(WorkflowPageComponent, {
        injector: app.injector
    });

    const headerComponent = createCustomElement(LibraryComponent, {
        injector: app.injector
    });

    customElements.define("library-component", libraryComponent);
    customElements.define("library-user-task-form", userTaskFormComponent);
    customElements.define("library-workflow-page", workflowPageComponent);
    customElements.define("library-header", headerComponent);
})();
