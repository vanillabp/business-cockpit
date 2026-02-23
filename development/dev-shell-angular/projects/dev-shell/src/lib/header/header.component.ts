import { Component, computed, effect, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TaskToggleComponent } from '../components/task-toggle.component';
import { Subscription } from 'rxjs';
import { AutoComplete } from 'primeng/autocomplete';
import { WINDOW_REF } from '../window-ref';
import {
  UserTasks,
  UserTask,
  Workflow,
  UserTasksRequest,
  WorkflowsRequest,
  Workflows,
  UserTaskRetrieveMode
} from '@vanillabp/bc-official-gui-client';

interface SelectOption {
  label: string;
  value: string;
}

enum ContentType {
  UserTask = 'usertask',
  Workflow = 'workflow'
}

@Component({
  selector: 'lib-header',
  imports: [CommonModule, FormsModule, TaskToggleComponent, AutoComplete],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit, OnDestroy {
  // Shared properties
  private readonly contentType = signal(ContentType.UserTask);
  page = 0;
  hasMorePages = true;
  isLoading = false;
  protected readonly taskFilter = signal<'all'|'open'|'closed'>('all');
  private routeSubscription: Subscription|undefined;

  // User task properties
  private readonly userTasks = signal<UserTask[]>([]);
  protected readonly userTaskOptions = computed<SelectOption[]>(() => this.userTasks().map(task => ({
    label: `${task.businessId || ''} | ${task.taskDefinition}/${task.bpmnProcessId} | (${task.id})`,
    value: task.id
  })));
  protected readonly selectedTaskId = signal<string|undefined>(undefined);

  // Workflow properties
  private readonly workflows = signal<Workflow[]>([]);
  protected readonly workflowOptions = computed<SelectOption[]>(() => this.workflows().map(workflow => ({
    label: `${workflow.businessId || ''} | ${workflow.bpmnProcessId} (${workflow.id})`,
    value: workflow.id
  })));
  protected readonly selectedWorkflowId = signal<string|undefined>(undefined);

  private readonly windowRef = inject(WINDOW_REF);

  protected readonly isPhone = signal(false);

  private resizeListener() {
    this.isPhone.set(this.windowRef.innerWidth < 768);
  }

  readonly isWorkflowView = computed(() => this.contentType() === ContentType.Workflow);
  readonly isUserTaskView = computed(() => this.contentType() === ContentType.UserTask);

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
  ) {
    effect(() => console.log('UT Options: ', this.userTaskOptions()))
    effect(() => console.log('WF Options: ', this.workflowOptions()))
  }

  ngOnInit(): void {
    // Determine content type based on URL and load initial data
    const url = this.router.url;
    if (url.includes('/workflow')) {
      this.contentType.set(ContentType.Workflow);
      this.selectedWorkflowId.set(this.route.snapshot.paramMap.get('workflowId') ?? undefined);
      this.fetchWorkflows(0);
    } else {
      this.contentType.set(ContentType.UserTask);
      this.selectedTaskId.set(this.route.snapshot.paramMap.get('userTaskId') ?? undefined);
      this.fetchUserTasks(0);
    }

    // Subscribe to route parameter changes
    this.routeSubscription = this.route.params.subscribe(params => {
      const newTaskId = params['userTaskId'];
      const newWorkflowId = params['workflowId'];

      if (this.isUserTaskView() && newTaskId !== this.selectedTaskId) {
        this.selectedTaskId.set(newTaskId);
      }

      if (this.isWorkflowView() && newWorkflowId !== this.selectedWorkflowId) {
        this.selectedWorkflowId.set(newWorkflowId);
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

  // User Task methods
  fetchUserTasks(pageToFetch: number): void {
    if (this.isLoading) return;
    this.isLoading = true;

    let mode: UserTaskRetrieveMode;
    if (this.taskFilter() === 'open') mode = UserTaskRetrieveMode.OpenTasks;
    else if (this.taskFilter() === 'closed') mode = UserTaskRetrieveMode.ClosedTasksOnly;
    else mode = UserTaskRetrieveMode.All;

    const request: UserTasksRequest = {
      pageNumber: pageToFetch, pageSize: 20, sort: 'createdAt', sortAscending: false, mode: mode
    };

    this.http.post<UserTasks>('/official-api/v1/usertask', request)
      .subscribe({
        next: (data) => {
          this.userTasks.set(pageToFetch === 0 ? data.userTasks : [...this.userTasks(), ...data.userTasks]);

          this.page = pageToFetch;
          this.hasMorePages = data.page.number + 1 < data.page.totalPages;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error fetching tasks:', error);
          if (pageToFetch === 0) {
            this.userTasks.set([]);
          }
          this.isLoading = false;
          this.hasMorePages = false;
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

    this.http.post<Workflows>('/official-api/v1/workflow', request)
      .subscribe({
        next: (data) => {
          this.workflows.set(pageToFetch === 0 ? data.workflows : [...this.workflows(), ...data.workflows]);

          this.page = pageToFetch;
          this.hasMorePages = data.page.number + 1 < data.page.totalPages;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error fetching workflows:', error);
          if (pageToFetch === 0) {
            this.workflows.set([]);
          }
          this.isLoading = false;
          this.hasMorePages = false;
        }
      });
  }

  loadMore(): void {
    if (!this.isLoading && this.hasMorePages) {
      const nextPage = this.page + 1;
      if (this.isUserTaskView()) {
        this.fetchUserTasks(nextPage);
      } else {
        this.fetchWorkflows(nextPage);
      }
    }
  }

  onItemSelect(itemId?: string): void {
    if (this.isUserTaskView() && itemId) {
      this.loadUserTask(itemId);
    } else if (this.isWorkflowView() && itemId) {
      this.loadWorkflow(itemId);
    } else if (!itemId) {
      if (this.isUserTaskView()) this.router.navigate(['/task']);
      else if (this.isWorkflowView()) this.router.navigate(['/workflow']);
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

  changeFilter(filter: 'all'|'open'|'closed'): void {
    this.taskFilter.set(filter);
    this.page = 0;
    this.hasMorePages = true;
    this.isLoading = false;

    if (this.isUserTaskView()) {
      this.userTasks.set([]);
      this.fetchUserTasks(0);
    }
  }

  navigateToView(view: string): void {
    const currentBase = this.isUserTaskView() ? `/task/${this.selectedTaskId}` : `/workflow/${this.selectedWorkflowId}`;
    const targetRoute = view === 'form' || view === 'page' ? [currentBase] : [currentBase, view];

    const currentUrl = this.router.url;
    const targetUrl = this.router.createUrlTree(targetRoute).toString();

    if (currentUrl !== targetUrl) {
      this.router.navigate(targetRoute);
    }
  }
}

