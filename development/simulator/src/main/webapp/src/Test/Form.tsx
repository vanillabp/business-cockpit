import React, { lazy } from 'react';
import { Text } from 'grommet';
import { taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { taskDefinition as TestForm3_taskDefinition } from './TestForm3';
import { UserTaskForm as UserTaskFormComponent } from '@bc/shared';

const TestForm1 = lazy(() => import('./TestForm1/Form'));
const TestForm2 = lazy(() => import('./TestForm2/Form'));
const TestForm3 = lazy(() => import('./TestForm3/Form'));

const UserTaskForm: UserTaskFormComponent = ({ userTask }) =>
    userTask.taskDefinition === TestForm1_taskDefinition
        ? <TestForm1 userTask={ userTask } />
        : userTask.taskDefinition === TestForm2_taskDefinition
        ? <TestForm2 userTask={ userTask } />
        : userTask.taskDefinition === TestForm3_taskDefinition
        ? <TestForm3 userTask={ userTask } />
        : <Text>{ `unknown task '${userTask.taskDefinition}'` }</Text>;

export default UserTaskForm;
