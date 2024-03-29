import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { DevShellComponent } from "@vanillabp/bc-dev-shell-angular";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, DevShellComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'webapp-angular';
}
