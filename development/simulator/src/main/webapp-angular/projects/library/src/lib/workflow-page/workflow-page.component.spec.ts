import { ComponentFixture, TestBed } from "@angular/core/testing";

import { WorkflowPageComponent } from "./workflow-page.component";

describe("WorkflowPageComponent", () => {
    let component: WorkflowPageComponent;
    let fixture: ComponentFixture<WorkflowPageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [WorkflowPageComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(WorkflowPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it("should create", () => {
        expect(component).toBeTruthy();
    });
});
