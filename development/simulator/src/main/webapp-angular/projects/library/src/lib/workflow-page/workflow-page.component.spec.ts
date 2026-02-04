import { ComponentFixture, TestBed } from "@angular/core/testing";

import { WorkflowPageComponent } from "./workflow-page.component";
import { BcWorkflow } from "@vanillabp/bc-types";

describe("WorkflowPageComponent", () => {
    const workflow: Partial<BcWorkflow> = {
        title: {de: 'Prozess'},
        version: 2,
        createdAt: new Date(),
        navigateToWorkflow(){},
    };
    let component: WorkflowPageComponent;
    let fixture: ComponentFixture<WorkflowPageComponent>;

    beforeEach(() => {
        fixture = TestBed.createComponent(WorkflowPageComponent);
        component = fixture.componentInstance;
        component.workflow = workflow as BcWorkflow;
        fixture.detectChanges();
    });

    it("should create", () => {
        expect(component).toBeTruthy();
    });
});
