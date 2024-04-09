import { Component, Input, OnInit } from '@angular/core';
import { BcUserTask } from '@vanillabp/bc-shared';
import { UserTaskFormComponent } from '../user-task-form/user-task-form.component';

@Component({
  selector: 'lib-wrapper-user-task-form',
  standalone: true,
  imports: [
    UserTaskFormComponent
  ],
  template: `
    <lib-user-task-form [userTask]="userTask"/>
  `
})
export class WrapperUserTaskFormComponent implements OnInit {
    @Input() userProps: string = "";
    userTask?: BcUserTask;

    ngOnInit(): void {
      try {
        this.userTask = JSON.parse(this.userProps);
      } catch (error) {
        console.error("Failed to parse UserTask")
      }
    }
}
