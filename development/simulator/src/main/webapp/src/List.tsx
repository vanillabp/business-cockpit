import { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId } from './Test';

const Test_TaskListCell = lazy(() => import('./Test/List'));

const TaskListCell = ({
    bpmnProcessId,
    formKey,
    column
  }: {
    bpmnProcessId: string,
    formKey: string,
    column: string
  }) =>
    bpmnProcessId === Test_bpmnProcessId
        ? <Test_TaskListCell formKey={ formKey } column={ column } />
        : <></>;

export {
  TaskListCell
};
