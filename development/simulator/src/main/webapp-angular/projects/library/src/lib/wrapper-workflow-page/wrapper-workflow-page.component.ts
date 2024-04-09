import { Component, Input } from '@angular/core';
import { WorkflowPageComponent } from '../workflow-page/workflow-page.component';
import { BcWorkflow } from '@vanillabp/bc-shared';

@Component({
  selector: 'lib-wrapper-workflow-page',
  standalone: true,
  imports: [
    WorkflowPageComponent
  ],
  template: `
  `
})
export class WrapperWorkflowPageComponent {
  @Input() workflowProps: string = "";
  workflow?: BcWorkflow;

  ngOnInit(): void {
    try {
      this.workflow = JSON.parse(this.workflowProps);
    } catch (error) {
      console.error("Failed to parse UserTask")
    }
  }
}
