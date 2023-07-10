import React, { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId, taskListColumns as Test_taskListColumns } from './Test';
import { UserTaskListCell, WarningListCell } from '@vanillabp/bc-shared';

const Test_TaskListCell = lazy(() => import('./Test/List'));

//@ts-expect-error
const buildVersion = process.env.BUILD_VERSION;
//@ts-expect-error
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const taskListColumns = {
  [ Test_bpmnProcessId ]: Test_taskListColumns
};

const UserTaskListCellComponent: UserTaskListCell = ({
  item,
  column,
  defaultCell
}) =>
  item.data.bpmnProcessId === Test_bpmnProcessId
      ? <Test_TaskListCell item={ item } column={ column } defaultCell={ defaultCell } />
      : <WarningListCell message={ `unknown BPMN process ID '${item.data.bpmnProcessId}'` } />;

export {
  buildVersion,
  buildTimestamp,
  taskListColumns,
  UserTaskListCellComponent as TaskListCell
};
