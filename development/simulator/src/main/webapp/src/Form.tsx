import React, { lazy } from 'react';
import { bpmnProcessId as Test_bpmnProcessId } from './Test';

const Test_UserTaskForm = lazy(() => import('./Test/Form'));

const UserTaskForm = ({
    bpmnProcessId,
    formKey
  }: {
    bpmnProcessId: string,
    formKey: string
  }) =>
    bpmnProcessId === Test_bpmnProcessId
        ? <Test_UserTaskForm formKey={ formKey } />
        : <></>;

export {
  UserTaskForm
};
