import { Text } from 'grommet';
import React, { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId } from './Test';
import { UserTaskForm } from '@vanillabp/bc-shared';

const Test_UserTaskForm = lazy(() => import('./Test/UserTaskForm'));

//@ts-expect-error
const buildVersion = process.env.BUILD_VERSION;
//@ts-expect-error
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const UserTaskFormComponent: UserTaskForm = ({ userTask }) =>
    userTask.bpmnProcessId === Test_bpmnProcessId
        ? <Test_UserTaskForm userTask={ userTask } />
        : <Text>{ `unknown BPMN process ID '${userTask.bpmnProcessId}'` }</Text>;

export {
  buildVersion,
  buildTimestamp,
  UserTaskFormComponent as UserTaskForm
};
