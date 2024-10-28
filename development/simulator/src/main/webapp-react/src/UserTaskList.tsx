import { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId, userTaskListColumns as Test_userTaskListColumns } from './Test';
import { ColumnsOfUserTaskFunction, UserTaskListCell, WarningListCell } from '@vanillabp/bc-shared';

const Test_UserTaskListCell = lazy(() => import('./Test/UserTaskList'));

const buildVersion = process.env.BUILD_VERSION;
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const userTaskListColumns: ColumnsOfUserTaskFunction = userTask => {
  if (userTask.bpmnProcessId === Test_bpmnProcessId) {
    return Test_userTaskListColumns(userTask);
  }
  return undefined;
};

const UserTaskListCellComponent: UserTaskListCell = ({
  item,
  ...props
}) => {
  const DefaultCell = props.defaultCell;
  return props.column.path === 'id'
      ? <DefaultCell item={ item } { ...props } />
      : item.data.bpmnProcessId === Test_bpmnProcessId
      ? <Test_UserTaskListCell item={ item } { ...props } />
      : props.column.path === 'title'
      ? <DefaultCell item={ item } { ...props } />
      : <WarningListCell message={ `unknown BPMN process ID '${ item.data.bpmnProcessId }'` }/>;
};

export {
  buildVersion,
  buildTimestamp,
  userTaskListColumns,
  UserTaskListCellComponent as UserTaskListCell
};
