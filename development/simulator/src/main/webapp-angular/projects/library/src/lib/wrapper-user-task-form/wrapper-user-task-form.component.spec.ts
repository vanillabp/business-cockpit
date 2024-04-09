import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WrapperUserTaskFormComponent } from './wrapper-user-task-form.component';

describe('WrapperUserTaskFormComponent', () => {
  let component: WrapperUserTaskFormComponent;
  let fixture: ComponentFixture<WrapperUserTaskFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WrapperUserTaskFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(WrapperUserTaskFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
