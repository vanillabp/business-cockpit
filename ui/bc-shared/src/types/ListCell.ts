
interface Title {
  [key: string]: string;
}

export interface Column {
  title: Title;
  path: string;
  type?: 'value' | 'i18n' | 'person' | 'date' | 'date-time' | 'time';
  priority: number;
  width: string;
  show: boolean;
  sortable: boolean;
  filterable: boolean;
  resizeable: boolean;
}

export interface Person {
  id: string;
  display?: string;
  displayShort?: string;
  email?: string;
  avatar?: number;
  details?: { [key: string]: any; };
}

export enum ListItemStatus {
  INITIAL,
  NEW,
  UPDATED,
  ENDED,
  REMOVED_FROM_LIST,
};

export interface ListItem<D> {
  id: string;
  number: number;
  data: D;
  status: ListItemStatus;
  selected: boolean;
  read?: Date;
};
