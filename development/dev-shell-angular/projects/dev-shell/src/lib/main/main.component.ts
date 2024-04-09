import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'lib-main',
  standalone: true,
  imports: [],
  templateUrl: './main.component.html',
  styleUrl: './main.component.css'
})
export class MainComponent {
  constructor(
    private readonly router: Router,
  ) {
  }

  navigateUsertask() {
    this.router.navigate(["/task"])
  }

  navigateWorkflow() {
    this.router.navigate(["/workflow"])
  }
}
