import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TaskToggleComponent } from '../components/task-toggle.component';
import { DropdownModule } from 'primeng/dropdown';
import { Subscription } from 'rxjs';

interface SelectOption {
  label: string;
  value: string;
}

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
  imports: [CommonModule, FormsModule, TaskToggleComponent, DropdownModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit, OnDestroy {
  // Shared properties
  contentType: ContentType = ContentType.UserTask;
  page = 0;
  hasMorePages = true;
  isLoading = false;
  taskFilter: 'all' | 'open' | 'closed' = 'all';
  isPhone = window.innerWidth < 768;
  private routeSubscription: Subscription | undefined;
  private resizeListener: () => void;

  // User task properties
  userTasks: UserTask[] = [];
  userTaskOptions: SelectOption[] = [];
  selectedTaskId?: string;

  // Workflow properties
  workflows: Workflow[] = [];
  workflowOptions: SelectOption[] = [];
  selectedWorkflowId?: string;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private cdRef: ChangeDetectorRef
  ) {
    this.resizeListener = () => {
      this.isPhone = window.innerWidth < 768;
      this.cdRef.detectChanges();
    };
  }

  ngOnInit(): void {
    // Determine content type based on URL and load initial data
    const url = this.router.url;
    if (url.includes('/workflow')) {
      this.contentType = ContentType.Workflow;
      this.selectedWorkflowId = this.route.snapshot.paramMap.get('workflowId') || undefined;
      this.fetchWorkflows(0);
    } else {
      this.contentType = ContentType.UserTask;
      this.selectedTaskId = this.route.snapshot.paramMap.get('userTaskId') || undefined;
      this.fetchUserTasks(0);
    }

    // Subscribe to route parameter changes
    this.routeSubscription = this.route.params.subscribe(params => {
      const newTaskId = params['userTaskId'];
      const newWorkflowId = params['workflowId'];

      if (this.contentType === ContentType.UserTask && newTaskId !== this.selectedTaskId) {
        this.selectedTaskId = newTaskId;
        this.cdRef.detectChanges();
      }

      if (this.contentType === ContentType.Workflow && newWorkflowId !== this.selectedWorkflowId) {
        this.selectedWorkflowId = newWorkflowId;
        this.cdRef.detectChanges();
      }
    });

    // Responsive handler
    window.addEventListener('resize', this.resizeListener);
  }

  ngOnDestroy(): void {
    if (this.routeSubscription) {
      this.routeSubscription.unsubscribe();
    }
    window.removeEventListener('resize', this.resizeListener);
  }

  get isUserTaskView(): boolean {
    return this.contentType === ContentType.UserTask;
  }

  get isWorkflowView(): boolean {
    return this.contentType === ContentType.Workflow;
  }

  // User Task methods
  fetchUserTasks(pageToFetch: number): void {
    if (this.isLoading) return;
    this.isLoading = true;

    let mode: UserTaskRetrieveMode;
    if (this.taskFilter === 'open') mode = UserTaskRetrieveMode.OpenTasks;
    else if (this.taskFilter === 'closed') mode = UserTaskRetrieveMode.ClosedTasksOnly;
    else mode = UserTaskRetrieveMode.All;

    const request: UserTasksRequest = {
      pageNumber: pageToFetch, pageSize: 20, sort: 'createdAt', sortAscending: false, mode: mode
    };

    this.http.post<UserTasksResponse>('/official-api/v1/usertask', request)
      .subscribe({
        next: (data: UserTasksResponse) => {
          this.userTasks = pageToFetch === 0 ? data.userTasks : [...this.userTasks, ...data.userTasks];

          this.userTaskOptions = this.userTasks.map(task => ({
            label: `${task.businessId || ''} | ${task.taskDefinition}/${task.bpmnProcessId} | (${task.id})`,
            value: task.id
          }));

          this.page = pageToFetch;
          this.hasMorePages = data.page.number + 1 < data.page.totalPages;
          this.isLoading = false;
          this.cdRef.detectChanges();
        },
        error: (error) => {
          console.error('Error fetching tasks:', error);
          if (pageToFetch === 0) {
            this.userTasks = [];
            this.userTaskOptions = [];
          }
          this.isLoading = false;
          this.hasMorePages = false;
          this.cdRef.detectChanges();
        }
      });
  }

  // Workflow methods
  fetchWorkflows(pageToFetch: number): void {
    if (this.isLoading) return;
    this.isLoading = true;

    const request: WorkflowsRequest = {
      pageNumber: pageToFetch, pageSize: 20, sort: 'createdAt', sortAscending: false
    };

    this.http.post<WorkflowsResponse>('/official-api/v1/workflow', request)
      .subscribe({
        next: (data: WorkflowsResponse) => {
          this.workflows = pageToFetch === 0 ? data.workflows : [...this.workflows, ...data.workflows];

          this.workflowOptions = this.workflows.map(workflow => ({
            label: `${workflow.businessId || ''} | ${workflow.bpmnProcessId} (${workflow.id})`,
            value: workflow.id
          }));

          this.page = pageToFetch;
          this.hasMorePages = data.page.number + 1 < data.page.totalPages;
          this.isLoading = false;
          this.cdRef.detectChanges();
        },
        error: (error) => {
          console.error('Error fetching workflows:', error);
          if (pageToFetch === 0) {
            this.workflows = [];
            this.workflowOptions = [];
          }
          this.isLoading = false;
          this.hasMorePages = false;
          this.cdRef.detectChanges();
        }
      });
  }

  loadMore(): void {
    if (!this.isLoading && this.hasMorePages) {
      const nextPage = this.page + 1;
      if (this.isUserTaskView) {
        this.fetchUserTasks(nextPage);
      } else {
        this.fetchWorkflows(nextPage);
      }
    }
  }

  onItemSelect(itemId: string | undefined): void {
    if (this.isUserTaskView && itemId) {
      this.loadUserTask(itemId);
    } else if (this.isWorkflowView && itemId) {
      this.loadWorkflow(itemId);
    } else if (!itemId) {
      if (this.isUserTaskView) this.router.navigate(['/task']);
      else if (this.isWorkflowView) this.router.navigate(['/workflow']);
    }
  }

  loadUserTask(taskId: string): void {
    if (this.route.snapshot.paramMap.get('userTaskId') !== taskId) {
      this.router.navigate(['/task', taskId]);
    }
  }

  loadWorkflow(workflowId: string): void {
    if (this.route.snapshot.paramMap.get('workflowId') !== workflowId) {
      this.router.navigate(['/workflow', workflowId]);
    }
  }

  onFilterChange(filter: 'all' | 'open' | 'closed'): void {
    this.taskFilter = filter;
    this.page = 0;
    this.hasMorePages = true;
    this.isLoading = false;

    if (this.isUserTaskView) {
      this.userTasks = [];
      this.userTaskOptions = [];
      this.fetchUserTasks(0);
    } else {
      // Filter only applies to user task page atm.
    }
    this.cdRef.detectChanges();
  }

  navigateToView(view: string): void {
    const currentBase = this.isUserTaskView ? `/task/${this.selectedTaskId}` : `/workflow/${this.selectedWorkflowId}`;
    const targetRoute = view === 'form' || view === 'page' ? [currentBase] : [currentBase, view];

    const currentUrl = this.router.url;
    const targetUrl = this.router.createUrlTree(targetRoute).toString();

    if (currentUrl !== targetUrl) {
      this.router.navigate(targetRoute);
    }
  }
}

