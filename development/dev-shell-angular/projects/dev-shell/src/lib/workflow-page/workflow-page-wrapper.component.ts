import { Component, Input } from '@angular/core';
import { NgIf } from '@angular/common';
import { RouterOutlet } from "@angular/router";
import { BcWorkflow } from '@vanillabp/bc-types';

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
