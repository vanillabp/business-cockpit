import { UserTask } from '@vanillabp/bc-official-gui-client';
import { Column } from '../list/column';

export type ColumnsOfUserTaskFunction = (
  userTask: UserTask,
) => Column[] | undefined;
