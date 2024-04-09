import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShellAppComponent } from './shell-app.component';

describe('ShellAppComponent', () => {
  let component: ShellAppComponent;
  let fixture: ComponentFixture<ShellAppComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShellAppComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ShellAppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
