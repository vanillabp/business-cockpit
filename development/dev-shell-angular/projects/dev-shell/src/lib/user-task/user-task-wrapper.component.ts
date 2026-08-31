import { Component, Input } from '@angular/core';

import { RouterOutlet } from "@angular/router";
import { BcUserTask } from '@vanillabp/bc-types';

@Component({
    selector: 'user-task-wrapper',
    imports: [
    RouterOutlet
],
    templateUrl: './user-task-wrapper.component.html',
    styleUrl: './user-task-wrapper.component.css'
})
export class UserTaskWrapperComponent {
  @Input() userTask?: BcUserTask;
}
