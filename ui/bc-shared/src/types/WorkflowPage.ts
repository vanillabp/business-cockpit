import { FC } from 'react';
import { BcWorkflow } from '@vanillabp/bc-types';

interface WorkflowPageProps {
  workflow: BcWorkflow;
}

export type WorkflowPage = FC<WorkflowPageProps>;
