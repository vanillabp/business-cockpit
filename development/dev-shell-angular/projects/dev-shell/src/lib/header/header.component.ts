import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TaskToggleComponent } from '../components/task-toggle.component';

interface UserTask {
  id: string;
  businessId: string;
  taskDefinition: string;
  bpmnProcessId: string;
}

interface Page {
  number: number;
  totalPages: number;
}

interface UserTasksResponse {
  userTasks: UserTask[];
  page: Page;
}

enum UserTaskRetrieveMode {
  All = 'All',
  OpenTasks = 'OpenTasks',
  ClosedTasksOnly = 'ClosedTasksOnly'
}

interface UserTasksRequest {
  pageNumber: number;
  pageSize: number;
  sort: string;
  sortAscending: boolean;
  mode: UserTaskRetrieveMode;
}

@Component({
  selector: 'lib-header',
  standalone: true,
  imports: [CommonModule, FormsModule, TaskToggleComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit {
  userTasks: UserTask[] = [];
  taskOptions: { label: string, value: string }[] = [];
  selectedTaskId?: string;
  page = 0;
  hasMorePages = true;
  taskFilter: 'all' | 'open' | 'closed' = 'all';
  isPhone = window.innerWidth < 768;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.selectedTaskId = params['userTaskId'];
      this.fetchTasks(0, this.selectedTaskId);
    });

    window.addEventListener('resize', () => {
      this.isPhone = window.innerWidth < 768;
    });
  }

  fetchTasks(pageToFetch: number = 0, currentTaskId?: string): void {
    let mode: UserTaskRetrieveMode;
    
    if (this.taskFilter === 'open') {
      mode = UserTaskRetrieveMode.OpenTasks;
    } else if (this.taskFilter === 'closed') {
      mode = UserTaskRetrieveMode.ClosedTasksOnly;
    } else {
      mode = UserTaskRetrieveMode.All;
    }

    const request: UserTasksRequest = {
      pageNumber: pageToFetch,
      pageSize: 20,
      sort: 'createdAt',
      sortAscending: false,
      mode: mode
    };

    this.http.post<UserTasksResponse>('/official-api/v1/usertask', request)
      .subscribe({
        next: (data: UserTasksResponse) => {
          this.userTasks = data.userTasks;

          if (currentTaskId) {
            const stillExists = data.userTasks.some(task => task.id === currentTaskId);
            if (!stillExists) {
              this.router.navigate(['/task'], { replaceUrl: true });
              this.selectedTaskId = undefined;
            }
          }

          if (pageToFetch === 0) {
            this.taskOptions = data.userTasks.map(task => ({
              label: `${task.businessId || ''} | ${task.taskDefinition}/${task.bpmnProcessId} | (${task.id})`,
              value: task.id
            }));
          } else {
            const newOptions = data.userTasks.map(task => ({
              label: `${task.businessId || ''} | ${task.taskDefinition}/${task.bpmnProcessId} | (${task.id})`,
              value: task.id
            }));
            this.taskOptions = [...this.taskOptions, ...newOptions];
          }

          this.page = pageToFetch;
          this.hasMorePages = data.page.number + 1 < data.page.totalPages;
        },
        error: (error) => {
          console.error('Error fetching tasks:', error);
          this.taskOptions = [];
        }
      });
  }

  loadMoreTasks(): void {
    if (this.hasMorePages) {
      this.fetchTasks(this.page + 1);
    }
  }

  onTaskSelect(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.selectedTaskId = selectElement.value;
    this.loadUserTask();
  }

  loadUserTask(taskId?: string): void {
    this.router.navigate(['/task', taskId || this.selectedTaskId], { replaceUrl: true });
  }

  onFilterChange(filter: 'all' | 'open' | 'closed'): void {
    this.taskFilter = filter;
    this.fetchTasks(0, this.selectedTaskId);
  }

  navigateToView(view: string): void {
    if (!this.selectedTaskId) return;
    
    if (view === 'form') {
      this.router.navigate(['/task', this.selectedTaskId]);
    } else if (view === 'list') {
      this.router.navigate(['/task', this.selectedTaskId, 'list']);
    } else if (view === 'icon') {
      this.router.navigate(['/task', this.selectedTaskId, 'icon']);
    }
  }
}

