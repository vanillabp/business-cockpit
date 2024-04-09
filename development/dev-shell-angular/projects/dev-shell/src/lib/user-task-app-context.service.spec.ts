import { TestBed } from '@angular/core/testing';

import { UserTaskAppContextService } from './user-task-app-context.service';

describe('UserTaskAppContextService', () => {
  let service: UserTaskAppContextService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserTaskAppContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
