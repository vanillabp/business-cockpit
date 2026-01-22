import { TestBed } from '@angular/core/testing';

import { DevShellAppContextService } from './dev-shell-app-context.service';
import { OfficialTasklistApi, OfficialWorkflowlistApi } from '@vanillabp/bc-official-gui-client';

describe('DevShellAppContextService', () => {
  let service: DevShellAppContextService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {provide: OfficialTasklistApi, useValue: {}},
        {provide: OfficialWorkflowlistApi, useValue: {}},
      ]
    });
    service = TestBed.inject(DevShellAppContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
