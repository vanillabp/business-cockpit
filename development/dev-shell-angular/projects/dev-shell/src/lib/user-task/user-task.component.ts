import { Component, OnInit } from '@angular/core';
import { HeaderComponent } from "../header/header.component";
import { NgIf } from '@angular/common';
import { ActivatedRoute, Router, RouterOutlet } from '@angular/router';

@Component({
    selector: 'user-task',
    standalone: true,
    imports: [
      RouterOutlet,
      HeaderComponent,
      NgIf,
    ],
    templateUrl: './user-task.component.html',
    styleUrl: './user-task.component.css',
})
export class UserTaskComponent implements OnInit {
  userTaskId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.userTaskId = this.route.snapshot.paramMap.get("userTaskId");
    
    // Force reload of the header when directly navigating to user task page
    if (!this.router.url.includes('task') && this.userTaskId) {
      this.router.navigate(['/task', this.userTaskId], { replaceUrl: true });
    }
  }
}
