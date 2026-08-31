import { InjectionToken } from '@angular/core';

export const WINDOW_REF = new InjectionToken('WINDOW_REF', {providedIn: 'root', factory: () => window})
