import { Component, Input } from "@angular/core";
import { BcWorkflowModule } from "@vanillabp/bc-types";

@Component({
    selector: "lib-header",
    imports: [],
    templateUrl: "./header.component.html",
    styleUrl: "./header.component.css"
})
export class HeaderComponent {
    @Input() workflowModule!: BcWorkflowModule;
}
