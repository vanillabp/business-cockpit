import { Component, Input } from "@angular/core";
import { BcUserTask } from "@vanillabp/bc-types";

@Component({
    selector: "lib-user-task-form",
    templateUrl: "./user-task-form.component.html",
    styleUrl: "./user-task-form.component.css"
})
export class UserTaskFormComponent {
    @Input() userTask!: BcUserTask;
}
