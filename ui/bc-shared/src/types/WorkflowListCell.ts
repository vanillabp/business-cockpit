import { FC } from 'react';
import { Workflow } from '@vanillabp/bc-official-gui-client';
import { DefaultListCellAwareProps } from 'src/components/DefaultListCell.js';
import { BcWorkflow, Column } from '@vanillabp/bc-types';

interface WorkflowCellProps extends DefaultListCellAwareProps<BcWorkflow> {
}

export type WorkflowListCell = FC<WorkflowCellProps>;

export type ColumnsOfWorkflowFunction = (workflow: Workflow) => Column[]|undefined;
