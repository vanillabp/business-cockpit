import { Text } from 'grommet';
import React, { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId } from './Test';
import { WorkflowPage } from '@vanillabp/bc-shared';

const Test_WorkflowPage = lazy(() => import('./Test/WorkflowPage'));

//@ts-expect-error
const buildVersion = process.env.BUILD_VERSION;
//@ts-expect-error
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const WorkflowPageComponent: WorkflowPage = ({ workflow }) =>
    workflow.bpmnProcessId === Test_bpmnProcessId
        ? <Test_WorkflowPage workflow={ workflow } />
        : <Text>{ `unknown BPMN process ID '${workflow.bpmnProcessId}'` }</Text>;

export {
  buildVersion,
  buildTimestamp,
  WorkflowPageComponent as WorkflowPage
};
