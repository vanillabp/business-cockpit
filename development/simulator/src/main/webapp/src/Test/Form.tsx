import React, { lazy } from 'react';
import { formKey as TestForm1_formKey } from './TestForm1';
import { formKey as TestForm2_formKey } from './TestForm2';
import { formKey as TestForm3_formKey } from './TestForm3';

const TestForm1 = lazy(() => import('./TestForm1/Form'));
const TestForm2 = lazy(() => import('./TestForm2/Form'));
const TestForm3 = lazy(() => import('./TestForm3/Form'));

const UserTaskForm = ({ formKey }: { formKey: string }) =>
    formKey === TestForm1_formKey
        ? <TestForm1 />
        : formKey === TestForm2_formKey
        ? <TestForm2 />
        : formKey === TestForm3_formKey
        ? <TestForm3 />
        : <></>;

export default UserTaskForm;
