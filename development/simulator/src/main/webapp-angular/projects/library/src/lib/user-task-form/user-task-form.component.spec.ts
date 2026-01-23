import { ComponentFixture, TestBed } from "@angular/core/testing";

import { UserTaskFormComponent } from "./user-task-form.component";
import { BcUserTask } from '@vanillabp/bc-types';

describe("UserTaskFormComponent", () => {
    const userTask: Partial<BcUserTask> = {
      title: {de: 'Aufgabe'},
      workflowTitle: {de: 'Meine Aufgabe'},
      id: 'ID',
      bpmnProcessId: 'PID',
      createdAt: new Date(),
      details: {},
      open() {},
      navigateToWorkflow(){},
    };
    let component: UserTaskFormComponent;
    let fixture: ComponentFixture<UserTaskFormComponent>;

    beforeEach(() => {
        fixture = TestBed.createComponent(UserTaskFormComponent);
        component = fixture.componentInstance;
        component.userTask =  userTask as BcUserTask;
        fixture.detectChanges();
    });

    it("should create", () => {
        expect(component).toBeTruthy();
    });
});
