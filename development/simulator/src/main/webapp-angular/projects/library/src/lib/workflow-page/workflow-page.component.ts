import { Component, Input } from "@angular/core";

import { BcWorkflow } from "@vanillabp/bc-shared";

@Component({
    selector: "lib-workflow-page",
    standalone: true,
    imports: [],
    templateUrl: "./workflow-page.component.html",
    styleUrl: "./workflow-page.component.css"
})
export class WorkflowPageComponent {
    @Input() workflow?: BcWorkflow;
}
