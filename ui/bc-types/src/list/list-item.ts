import { ListItemStatus } from './list-item-status';

export interface ListItem<T> {
  id: string;
  number: number;
  data: T;
  status: ListItemStatus;
  selected: boolean;
  read?: Date;
}
