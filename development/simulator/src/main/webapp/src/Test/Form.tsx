import React, { lazy } from 'react';
import { Text } from 'grommet';
import { taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { taskDefinition as TestForm3_taskDefinition } from './TestForm3';

const TestForm1 = lazy(() => import('./TestForm1/Form'));
const TestForm2 = lazy(() => import('./TestForm2/Form'));
const TestForm3 = lazy(() => import('./TestForm3/Form'));

const UserTaskForm = ({ taskDefinition }: { taskDefinition: string }) =>
    taskDefinition === TestForm1_taskDefinition
        ? <TestForm1 />
        : taskDefinition === TestForm2_taskDefinition
        ? <TestForm2 />
        : taskDefinition === TestForm3_taskDefinition
        ? <TestForm3 />
        : <Text>{ `unknown task '${taskDefinition}'` }</Text>;

export default UserTaskForm;
