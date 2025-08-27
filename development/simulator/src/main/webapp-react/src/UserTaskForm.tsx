import { Text } from 'grommet';
import { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId } from './Test';
import { UserTaskForm } from '@vanillabp/bc-shared';

const TestUserTaskForm = lazy(() => import('./Test/UserTaskForm'));

const buildVersion = process.env.BUILD_VERSION;
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const UserTaskFormComponent: UserTaskForm = ({ userTask }) =>
    userTask.bpmnProcessId === Test_bpmnProcessId
        ? <TestUserTaskForm userTask={ userTask } />
        : <Text>{ `unknown BPMN process ID '${userTask.bpmnProcessId}'` }</Text>;

export {
  buildVersion,
  buildTimestamp,
  UserTaskFormComponent as UserTaskForm
};
