import React, { lazy } from 'react';
import { Text } from 'grommet';
import { UserTaskListCell, WarningListCell } from '@vanillabp/bc-shared';
import { taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { taskDefinition as TestForm3_taskDefinition } from './TestForm3';

const TestForm1_TaskListCell = lazy(() => import('./TestForm1/List'));
const TestForm2_TaskListCell = lazy(() => import('./TestForm2/List'));
const TestForm3_TaskListCell = lazy(() => import('./TestForm3/List'));

const TaskListCell: UserTaskListCell = ({
    item,
    column,
    defaultCell
  }) => 
    item.data.taskDefinition === TestForm1_taskDefinition
        ? <TestForm1_TaskListCell item={ item } column={ column } defaultCell={ defaultCell } />
        : item.data.taskDefinition === TestForm2_taskDefinition
        ? <TestForm2_TaskListCell item={ item } column={ column } defaultCell={ defaultCell } />
        : item.data.taskDefinition === TestForm3_taskDefinition
        ? <TestForm3_TaskListCell item={ item } column={ column } defaultCell={ defaultCell } />
        : <WarningListCell message={ `unknown task '${item.data.taskDefinition}'` } />;

export default TaskListCell;
