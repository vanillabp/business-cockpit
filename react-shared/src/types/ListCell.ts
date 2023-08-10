
interface Title {
  [key: string]: string;
}

export interface Column {
  title: Title;
  path: string;
  priority: number;
  width: string;
};

export enum ListItemStatus {
  INITIAL,
  NEW,
  UPDATED,
  ENDED,
};

export interface ListItem<D> {
  id: string;
  number: number;
  data: D;
  status: ListItemStatus;
  selected: boolean;
};
