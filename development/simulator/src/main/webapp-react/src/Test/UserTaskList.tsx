import { lazy } from 'react';
import { UserTaskListCell, WarningListCell } from '@vanillabp/bc-shared';
import { taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { taskDefinition as TestForm3_taskDefinition } from './TestForm3';

const TestForm1UserTaskListCell = lazy(() => import('./TestForm1/UserTaskList'));
const TestForm2UserTaskListCell = lazy(() => import('./TestForm2/UserTaskList'));
const TestForm3UserTaskListCell = lazy(() => import('./TestForm3/UserTaskList'));

const TaskListCell: UserTaskListCell = ({
    item,
    ...props
  }) => 
    item.data.taskDefinition === TestForm1_taskDefinition
        ? <TestForm1UserTaskListCell item={ item } { ...props } />
        : item.data.taskDefinition === TestForm2_taskDefinition
        ? <TestForm2UserTaskListCell item={ item } { ...props } />
        : item.data.taskDefinition === TestForm3_taskDefinition
        ? <TestForm3UserTaskListCell item={ item } { ...props } />
        : <WarningListCell message={ `unknown task '${item.data.taskDefinition}'` } />;

export default TaskListCell;
