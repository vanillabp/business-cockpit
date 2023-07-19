import React from 'react';
import { buildTimestamp, buildVersion } from '../WorkflowPage';
import { WorkflowPage } from '@vanillabp/bc-shared';
import { Box } from 'grommet';

const TestWorkflowPage: WorkflowPage = ({ workflow }) => <Box>
    Workflow: '{workflow.title.de}' { buildVersion } from { buildTimestamp.toLocaleString() }
  </Box>

export default TestWorkflowPage;
