import { Component, Input } from "@angular/core";

import { BcWorkflowModule, ToastFunction } from "@vanillabp/bc-shared";

@Component({
    selector: "lib-header",
    standalone: true,
    imports: [],
    templateUrl: "./header.component.html",
    styleUrl: "./header.component.css"
})
export class HeaderComponent {
    @Input() workflowModule!: BcWorkflowModule;
    @Input() toast!: ToastFunction;
}
