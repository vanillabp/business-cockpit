import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from "rxjs";
import { NgForOf, NgIf } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpClient, HttpClientModule } from "@angular/common/http";

interface User {
  id: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  groups?: string[];
  attributes?: Record<string, string[]> | null;
}

@Component({
  selector: 'lib-main',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    FormsModule,
    HttpClientModule
  ],
  templateUrl: './main.component.html',
  styleUrl: './main.component.css'
})
export class MainComponent {
  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly http: HttpClient
  ) {
  }

  dataObserver: Subscription | undefined = undefined;
  additionalRoutes: string[] = [];
  users: User[] = [];
  currentUser: string | undefined = undefined;
  baseUrl = "/dev-shell";

  ngOnInit() {
    this.dataObserver = this.route.data.subscribe(data => this.additionalRoutes = data["additionalRoutes"]);
    this.loadUsers();
  }

  ngOnDestroy() {
    this.dataObserver?.unsubscribe();
  }

  navigateTo(target: string) {
    this.router.navigate([`/${target}`]);
  }

  loadUsers() {
    this.http.get<User[]>(`${this.baseUrl}/user/all`)
      .subscribe({
        next: (allUsers: User[]) => {
          this.users = allUsers;
          this.getCurrentUser();
        },
        error: (error) => {
          console.error('Error fetching users:', error);
        }
      });
  }

  getCurrentUser() {
    this.http.get(`${this.baseUrl}/user/`, {
      responseType: 'text',
      withCredentials: true
    }).subscribe({
      next: (user: string) => {
        this.currentUser = user;
      },
      error: () => {
        this.currentUser = undefined;
      }
    });
  }

  changeUser(event: Event) {
    const userId = (event.target as HTMLSelectElement).value;

    this.http.post(`${this.baseUrl}/user/${userId}`, {}, {
      withCredentials: true
    }).subscribe({
      next: () => {
        window.location.reload();
      },
      error: (error) => {
        console.error('Error changing user:', error);
      }
    });
  }
}
