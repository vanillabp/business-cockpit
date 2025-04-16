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

interface Workflow {
  id: string;
  businessId: string;
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

interface WorkflowsResponse {
  workflows: Workflow[];
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

interface WorkflowsRequest {
  pageNumber: number;
  pageSize: number;
  sort: string;
  sortAscending: boolean;
}

enum ContentType {
  UserTask = 'usertask',
  Workflow = 'workflow'
}

@Component({
  selector: 'lib-header',
  standalone: true,
  imports: [CommonModule, FormsModule, TaskToggleComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit {
  // Shared properties
  contentType: ContentType = ContentType.UserTask;
  page = 0;
  hasMorePages = true;
  taskFilter: 'all' | 'open' | 'closed' = 'all';
  isPhone = window.innerWidth < 768;
  
  // User task properties
  userTasks: UserTask[] = [];
  userTaskOptions: { label: string, value: string }[] = [];
  selectedTaskId?: string;
  
  // Workflow properties
  workflows: Workflow[] = [];
  workflowOptions: { label: string, value: string }[] = [];
  selectedWorkflowId?: string;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    // Determine content type based on URL
    const url = this.router.url;
    if (url.includes('/workflow')) {
      this.contentType = ContentType.Workflow;
    } else {
      this.contentType = ContentType.UserTask;
    }

    // Set up event listeners for route parameters
    this.route.params.subscribe(params => {
      if (this.contentType === ContentType.UserTask) {
        this.selectedTaskId = params['userTaskId'];
        this.fetchUserTasks(0, this.selectedTaskId);
      } else {
        this.selectedWorkflowId = params['workflowId'];
        this.fetchWorkflows(0, this.selectedWorkflowId);
      }
    });

    // Responsive handler
    window.addEventListener('resize', () => {
      this.isPhone = window.innerWidth < 768;
    });
  }

  get isUserTaskView(): boolean {
    return this.contentType === ContentType.UserTask;
  }

  get isWorkflowView(): boolean {
    return this.contentType === ContentType.Workflow;
  }

  // User Task methods
  fetchUserTasks(pageToFetch: number = 0, currentTaskId?: string): void {
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
            this.userTaskOptions = data.userTasks.map(task => ({
              label: `${task.businessId || ''} | ${task.taskDefinition}/${task.bpmnProcessId} | (${task.id})`,
              value: task.id
            }));
          } else {
            const newOptions = data.userTasks.map(task => ({
              label: `${task.businessId || ''} | ${task.taskDefinition}/${task.bpmnProcessId} | (${task.id})`,
              value: task.id
            }));
            this.userTaskOptions = [...this.userTaskOptions, ...newOptions];
          }

          this.page = pageToFetch;
          this.hasMorePages = data.page.number + 1 < data.page.totalPages;
        },
        error: (error) => {
          console.error('Error fetching tasks:', error);
          this.userTaskOptions = [];
        }
      });
  }

  // Workflow methods
  fetchWorkflows(pageToFetch: number = 0, currentWorkflowId?: string): void {
    const request: WorkflowsRequest = {
      pageNumber: pageToFetch,
      pageSize: 20,
      sort: 'createdAt',
      sortAscending: false
    };

    this.http.post<WorkflowsResponse>('/official-api/v1/workflow', request)
      .subscribe({
        next: (data: WorkflowsResponse) => {
          this.workflows = data.workflows;

          if (currentWorkflowId) {
            const stillExists = data.workflows.some(workflow => workflow.id === currentWorkflowId);
            if (!stillExists) {
              this.router.navigate(['/workflow'], { replaceUrl: true });
              this.selectedWorkflowId = undefined;
            }
          }

          if (pageToFetch === 0) {
            this.workflowOptions = data.workflows.map(workflow => ({
              label: `${workflow.businessId || ''} | ${workflow.bpmnProcessId} (${workflow.id})`,
              value: workflow.id
            }));
          } else {
            const newOptions = data.workflows.map(workflow => ({
              label: `${workflow.businessId || ''} | ${workflow.bpmnProcessId} (${workflow.id})`,
              value: workflow.id
            }));
            this.workflowOptions = [...this.workflowOptions, ...newOptions];
          }

          this.page = pageToFetch;
          this.hasMorePages = data.page.number + 1 < data.page.totalPages;
        },
        error: (error) => {
          console.error('Error fetching workflows:', error);
          this.workflowOptions = [];
        }
      });
  }

  loadMoreItems(): void {
    if (this.hasMorePages) {
      if (this.isUserTaskView) {
        this.fetchUserTasks(this.page + 1);
      } else {
        this.fetchWorkflows(this.page + 1);
      }
    }
  }

  onItemSelect(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const selectedId = selectElement.value;
    
    if (this.isUserTaskView) {
      this.selectedTaskId = selectedId;
      this.loadUserTask();
    } else {
      this.selectedWorkflowId = selectedId;
      this.loadWorkflow();
    }
  }

  loadUserTask(taskId?: string): void {
    this.router.navigate(['/task', taskId || this.selectedTaskId], { replaceUrl: true });
  }

  loadWorkflow(workflowId?: string): void {
    this.router.navigate(['/workflow', workflowId || this.selectedWorkflowId], { replaceUrl: true });
  }

  onFilterChange(filter: 'all' | 'open' | 'closed'): void {
    this.taskFilter = filter;
    
    if (this.isUserTaskView) {
      this.fetchUserTasks(0, this.selectedTaskId);
    } else {
      this.fetchWorkflows(0, this.selectedWorkflowId);
    }
  }

  navigateToView(view: string): void {
    if (this.isUserTaskView && this.selectedTaskId) {
      if (view === 'form') {
        this.router.navigate(['/task', this.selectedTaskId]);
      } else if (view === 'list') {
        this.router.navigate(['/task', this.selectedTaskId, 'list']);
      } else if (view === 'icon') {
        this.router.navigate(['/task', this.selectedTaskId, 'icon']);
      }
    } else if (this.isWorkflowView && this.selectedWorkflowId) {
      if (view === 'page') {
        this.router.navigate(['/workflow', this.selectedWorkflowId]);
      } else if (view === 'list') {
        this.router.navigate(['/workflow', this.selectedWorkflowId, 'list']);
      } else if (view === 'icon') {
        this.router.navigate(['/workflow', this.selectedWorkflowId, 'icon']);
      }
    }
  }
}

