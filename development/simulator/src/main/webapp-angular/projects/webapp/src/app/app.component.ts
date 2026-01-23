import { Component } from '@angular/core';

import { DevShellComponent } from "@vanillabp/bc-dev-shell-angular";

@Component({
  selector: 'app-root',
  imports: [DevShellComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
}
