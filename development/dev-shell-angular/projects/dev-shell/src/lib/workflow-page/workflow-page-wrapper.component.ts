import { Component, Input } from '@angular/core';
import { NgIf } from '@angular/common';
import { BcWorkflow } from "@vanillabp/bc-shared";
import { RouterOutlet } from "@angular/router";

@Component({
    selector: 'workflow-page-wrapper',
    standalone: true,
    imports: [
      RouterOutlet,
      NgIf,
    ],
    templateUrl: './workflow-page-wrapper.component.html',
    styleUrl: './workflow-page-wrapper.component.css',
})
export class WorkflowPageWrapperComponent {
  @Input() workflow?: BcWorkflow;
}
