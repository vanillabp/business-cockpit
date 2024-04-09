import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { userTaskResolver } from './user-task.resolver';

describe('userTaskResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => userTaskResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
