import { Component, OnInit } from '@angular/core';
import { HeaderComponent } from "../header/header.component";
import { NgIf } from '@angular/common';
import { ActivatedRoute, Router, RouterOutlet } from '@angular/router';

@Component({
    selector: 'workflow-page',
    standalone: true,
    imports: [
      RouterOutlet,
      HeaderComponent,
      NgIf,
    ],
    templateUrl: './workflow-page.component.html',
    styleUrl: './workflow-page.component.css',
})
export class WorkflowPageComponent implements OnInit {
  workflowId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.workflowId = this.route.snapshot.paramMap.get("workflowId");
    
    // Force reload of the header when directly navigating to workflow page
    if (!this.router.url.includes('workflow') && this.workflowId) {
      this.router.navigate(['/workflow', this.workflowId], { replaceUrl: true });
    }
  }
}
