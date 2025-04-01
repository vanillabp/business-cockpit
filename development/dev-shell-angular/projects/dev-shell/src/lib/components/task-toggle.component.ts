import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

type TaskFilter = 'all' | 'open' | 'closed';

interface Option {
  label: string;
  value: TaskFilter;
  background: string;
}

@Component({
  selector: 'app-task-toggle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-toggle.component.html',
  styleUrl: './task-toggle.component.css'
})
export class TaskToggleComponent {
  @Input() value: TaskFilter = 'all';
  @Output() onChange = new EventEmitter<TaskFilter>();

  options: Option[] = [
    { label: 'All Tasks', value: 'all', background: '#f2f2f2' },
    { label: 'Open Tasks', value: 'open', background: 'rgba(0, 200, 0, 0.2)' },
    { label: 'Closed Tasks', value: 'closed', background: 'rgba(200, 0, 0, 0.2)' }
  ];
} 