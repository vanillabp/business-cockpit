import React, { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId, userTaskListColumns as Test_userTaskListColumns } from './Test';
import { ColumnsOfUserTaskFunction, UserTaskListCell, WarningListCell } from '@vanillabp/bc-shared';

const Test_UserTaskListCell = lazy(() => import('./Test/UserTaskList'));

//@ts-expect-error
const buildVersion = process.env.BUILD_VERSION;
//@ts-expect-error
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const userTaskListColumns: ColumnsOfUserTaskFunction = userTask => {
  if (userTask.bpmnProcessId === Test_bpmnProcessId) {
    return Test_userTaskListColumns(userTask);
  }
  return undefined;
};

const UserTaskListCellComponent: UserTaskListCell = ({
  item,
  column,
  defaultCell
}) =>
  item.data.bpmnProcessId === Test_bpmnProcessId
      ? <Test_UserTaskListCell item={ item } column={ column } defaultCell={ defaultCell } />
      : <WarningListCell message={ `unknown BPMN process ID '${item.data.bpmnProcessId}'` } />;

export {
  buildVersion,
  buildTimestamp,
  userTaskListColumns,
  UserTaskListCellComponent as UserTaskListCell
};
