import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from "rxjs";
import { NgForOf } from "@angular/common";

@Component({
  selector: 'lib-main',
  standalone: true,
  imports: [
    NgForOf
  ],
  templateUrl: './main.component.html',
  styleUrl: './main.component.css'
})
export class MainComponent {
  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {
  }

  dataObserver: Subscription | undefined = undefined;
  additionalRoutes: string[] = [];

  ngOnInit() {
    this.dataObserver = this.route.data.subscribe(data => this.additionalRoutes = data["additionalRoutes"]);
  }

  ngOnDestroy() {
    this.dataObserver?.unsubscribe();
  }

  navigateTo(target: string) {
    this.router.navigate([`/${target}`])
  }

}
