import { Component, OnInit } from '@angular/core';
import { HeaderComponent } from "../header/header.component";
import { NgIf } from '@angular/common';
import { ActivatedRoute, Router, RouterOutlet } from '@angular/router';
import { UserTaskAppContextService } from '../user-task-app-context.service';

@Component({
    selector: 'lib-shell-app',
    standalone: true,
    imports: [
      RouterOutlet,
      HeaderComponent,
      NgIf,
    ],
    templateUrl: './shell-app.component.html',
    styleUrl: './shell-app.component.css',
})
export class ShellAppComponent implements OnInit {
  apiUrl = "/official-api/v1"
  userTaskId: string | null = null;
  workflowId: string | null = null;

  constructor(
    private userTaskAppContextService: UserTaskAppContextService,
    private route: ActivatedRoute,
    protected router: Router
  ) {}

  
  ngOnInit(): void {
    this.userTaskId = this.route.snapshot.paramMap.get("userTaskId")
    this.loadUserTask();

    this.workflowId = this.route.snapshot.paramMap.get("workflowId")
    this.loadWorkflow()
  }

  loadUserTask() {
    if (this.userTaskId) {
      this.userTaskAppContextService.loadUserTask(this.apiUrl, this.userTaskId)
    }
  }

  loadWorkflow() {
    if (this.workflowId) {
      this.userTaskAppContextService.loadWorkflow(this.apiUrl, this.workflowId)
    }
  }
}
