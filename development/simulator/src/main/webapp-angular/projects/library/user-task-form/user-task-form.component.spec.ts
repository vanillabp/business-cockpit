import { ComponentFixture, TestBed } from "@angular/core/testing";

import { UserTaskFormComponent } from "./user-task-form.component";

describe("UserTaskFormComponent", () => {
    let component: UserTaskFormComponent;
    let fixture: ComponentFixture<UserTaskFormComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [UserTaskFormComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(UserTaskFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it("should create", () => {
        expect(component).toBeTruthy();
    });
});
