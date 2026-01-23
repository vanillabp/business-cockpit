import { Workflow } from '@vanillabp/bc-official-gui-client';
import { Column } from '../list/column';

export type ColumnsOfWorkflowFunction = (
  workflow: Workflow,
) => Column[] | undefined;
