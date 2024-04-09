import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'lib-header',
  standalone: true,
  imports: [],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  constructor(
    private router: Router,
  ) {
  }

  search(query: string): void {
    if (query) {
      if (this.router.url.toString().startsWith("/task")) {
        query = "/task/" + query;
      } else if (this.router.url.startsWith("/workflow")) {
        query = "/workflow/" + query;
      }

      this.router.navigateByUrl(query).then(() => {
        window.location.reload();
      });
    }
  }
}
