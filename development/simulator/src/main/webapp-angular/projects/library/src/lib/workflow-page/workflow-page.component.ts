import { Component, Input } from "@angular/core";
import { BcWorkflow } from "@vanillabp/bc-types";

@Component({
    selector: "lib-workflow-page",
    standalone: true,
    templateUrl: "./workflow-page.component.html",
    styleUrl: "./workflow-page.component.css"
})
export class WorkflowPageComponent {
    @Input() workflow!: BcWorkflow;
}
