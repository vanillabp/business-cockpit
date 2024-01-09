
interface Title {
  [key: string]: string;
}

export interface Column {
  title: Title;
  path: string;
  priority: number;
  width: string;
  show: boolean;
  sortable: boolean;
  filterable: boolean;
};

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
