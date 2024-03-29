import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { workflowResolver } from './workflow.resolver';

describe('workflowResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => workflowResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
