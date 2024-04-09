import { createApplication } from "@angular/platform-browser";
// import { appConfig } from "./main.config";
import { appConfig } from "@vanillabp/bc-dev-shell-angular"
import { createCustomElement } from "@angular/elements";
import { ApplicationRef } from "@angular/core";
import { LibraryComponent } from "../../library/src/lib/library.component";
import { UserTaskFormComponent } from "../../library/src/lib/user-task-form/user-task-form.component";
import { WorkflowPageComponent } from "../../library/src/lib/workflow-page/workflow-page.component";
import { WrapperUserTaskFormComponent } from "../../library/src/lib/wrapper-user-task-form/wrapper-user-task-form.component";
import { WrapperWorkflowPageComponent } from "../../library/src/lib/wrapper-workflow-page/wrapper-workflow-page.component";

(async () => {
    const app: ApplicationRef = await createApplication(appConfig(UserTaskFormComponent, WorkflowPageComponent));

    // Define Web Components
    const libraryComponent = createCustomElement(LibraryComponent, {
        injector: app.injector
    });

    const userTaskFormComponent = createCustomElement(WrapperUserTaskFormComponent, {
        injector: app.injector
    });

    const workflowPageComponent = createCustomElement(WrapperWorkflowPageComponent, {
        injector: app.injector
    });

    customElements.define("library-component", libraryComponent);
    customElements.define("library-user-task-form", userTaskFormComponent);
    customElements.define("library-workflow-page", workflowPageComponent);
})();
