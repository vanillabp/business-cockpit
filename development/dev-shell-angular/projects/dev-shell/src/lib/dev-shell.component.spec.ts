import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DevShellComponent } from './dev-shell.component';

describe('DevShellComponent', () => {
  let component: DevShellComponent;
  let fixture: ComponentFixture<DevShellComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DevShellComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DevShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
