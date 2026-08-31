import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
    selector: 'lib-dev-shell',
    imports: [
        RouterOutlet
    ],
    template: `
    <router-outlet />
  `,
    styles: ``
})
export class DevShellComponent {
}
