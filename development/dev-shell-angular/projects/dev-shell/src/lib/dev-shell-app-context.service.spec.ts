import { TestBed } from '@angular/core/testing';

import { DevShellAppContextService } from './dev-shell-app-context.service';

describe('DevShellAppContextService', () => {
  let service: DevShellAppContextService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DevShellAppContextService);
    service = TestBed.inject(DevShellAppContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
