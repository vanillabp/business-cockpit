import { Text } from 'grommet';
import React, { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId } from './Test';

const Test_UserTaskForm = lazy(() => import('./Test/Form'));

//@ts-expect-error
const buildVersion = process.env.BUILD_VERSION;
//@ts-expect-error
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const UserTaskForm = ({
    bpmnProcessId,
    taskDefinition
  }: {
    bpmnProcessId: string,
    taskDefinition: string
  }) =>
    bpmnProcessId === Test_bpmnProcessId
        ? <Test_UserTaskForm taskDefinition={ taskDefinition } />
        : <Text>{ `unknown BPMN process ID '${bpmnProcessId}'` }</Text>;

export {
  buildVersion,
  buildTimestamp,
  UserTaskForm
};
