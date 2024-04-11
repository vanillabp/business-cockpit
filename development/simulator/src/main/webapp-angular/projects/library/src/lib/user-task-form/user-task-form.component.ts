import { Component, Input } from "@angular/core";

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
}
