import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WrapperWorkflowPageComponent } from './wrapper-workflow-page.component';

describe('WrapperWorkflowPageComponent', () => {
  let component: WrapperWorkflowPageComponent;
  let fixture: ComponentFixture<WrapperWorkflowPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WrapperWorkflowPageComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(WrapperWorkflowPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
