import { Component, Input } from '@angular/core';
import { NgIf } from '@angular/common';
import { BcUserTask } from "@vanillabp/bc-shared";
import { RouterOutlet } from "@angular/router";

@Component({
    selector: 'user-task-wrapper',
    standalone: true,
    imports: [
      RouterOutlet,
      NgIf,
    ],
    templateUrl: './user-task-wrapper.component.html',
    styleUrl: './user-task-wrapper.component.css',
})
export class UserTaskWrapperComponent {
  @Input() userTask?: BcUserTask;
}
