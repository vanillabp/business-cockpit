import { Text } from 'grommet';
import React, { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId, taskListColumns as Test_taskListColumns } from './Test';

const Test_TaskListCell = lazy(() => import('./Test/List'));

//@ts-expect-error
const buildVersion = process.env.BUILD_VERSION;
//@ts-expect-error
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const taskListColumns = {
  [ Test_bpmnProcessId ]: Test_taskListColumns
};

const TaskListCell = ({
    bpmnProcessId,
    taskDefinition,
    column
  }: {
    bpmnProcessId: string,
    taskDefinition: string,
    column: string
  }) =>
    bpmnProcessId === Test_bpmnProcessId
        ? <Test_TaskListCell taskDefinition={ taskDefinition } column={ column } />
        : <Text>{ `unknown BPMN process ID '${bpmnProcessId}'` }</Text>;

export {
  buildVersion,
  buildTimestamp,
  taskListColumns,
  TaskListCell
};
