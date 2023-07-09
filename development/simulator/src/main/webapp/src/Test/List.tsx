import React, { lazy } from 'react';
import { Text } from 'grommet';
import { taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { taskDefinition as TestForm3_taskDefinition } from './TestForm3';

const TestForm1_TaskListCell = lazy(() => import('./TestForm1/List'));
const TestForm2_TaskListCell = lazy(() => import('./TestForm2/List'));
const TestForm3_TaskListCell = lazy(() => import('./TestForm3/List'));

const TaskListCell = ({
    taskDefinition,
    path
  }: {
    taskDefinition: string,
    path: string
  }) => 
    taskDefinition === TestForm1_taskDefinition
        ? <TestForm1_TaskListCell path={ path } />
        : taskDefinition === TestForm2_taskDefinition
        ? <TestForm2_TaskListCell path={ path } />
        : taskDefinition === TestForm3_taskDefinition
        ? <TestForm3_TaskListCell path={ path } />
        : <Text>{ `unknown task '${taskDefinition}'` }</Text>;

export default TaskListCell;
