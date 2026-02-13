import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

interface ToggleOption {
  label: string;
  value: string;
  background: string;
}

const TASK_OPTIONS: ToggleOption[] = [
  { label: 'All', value: 'all', background: '#f2f2f2' },
  { label: 'Open', value: 'open', background: 'rgba(0, 200, 0, 0.2)' },
  { label: 'Closed', value: 'closed', background: 'rgba(200, 0, 0, 0.2)' }
];

@Component({
  selector: 'app-task-toggle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-toggle.component.html',
  styleUrl: './task-toggle.component.css'
})
export class TaskToggleComponent {
  @Input() value: string = 'all';
  @Input() options: ToggleOption[] = TASK_OPTIONS;
  @Output() onChange = new EventEmitter<string>();
}
