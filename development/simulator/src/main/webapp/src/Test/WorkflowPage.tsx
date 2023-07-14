import React from 'react';
import { buildTimestamp, buildVersion } from '../WorkflowPage';
import { WorkflowPage } from '@vanillabp/bc-shared';

const TestWorkflowPage: WorkflowPage = ({ workflow }) => <div>Workflow: '{workflow.title.de}' { buildVersion } from { buildTimestamp.toLocaleString() }</div>

export default TestWorkflowPage;
