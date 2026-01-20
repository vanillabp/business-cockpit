import { Translatable } from '../i18n/translatable';

export interface Column {
  title: Translatable;
  path: string;
  type?: 'value' | 'i18n' | 'person' | 'date' | 'date-time' | 'time';
  priority: number;
  width: string;
  show: boolean;
  sortable: boolean;
  filterable: boolean;
  resizeable: boolean;
}
