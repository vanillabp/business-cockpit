import {Component, Input} from "@angular/core";

import { BcUserTask } from "@vanillabp/bc-shared"

@Component({
    selector: "lib-user-task-form",
    standalone: true,
    imports: [],
    templateUrl: "./user-task-form.component.html",
    styleUrl: "./user-task-form.component.css"
})
export class UserTaskFormComponent {
    @Input() userTask?: BcUserTask;


    open(): void {
        console.log(this.userTask)
        if (!this.userTask || !this.userTask.open) return;
        if (typeof this.userTask.open != "string") {
            this.userTask.open();
            return
        }
        document.dispatchEvent(new CustomEvent(this.userTask.open, {}));
    }

    navigateWorkflow(): void {
        if (!this.userTask || !this.userTask.navigateToWorkflow) return;
        if (typeof this.userTask.navigateToWorkflow != "string") {
            this.userTask.navigateToWorkflow();
            return
        }
        document.dispatchEvent(
            new CustomEvent(this.userTask.navigateToWorkflow, {})
        );
    }
}
