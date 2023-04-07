import React, { lazy } from 'react';
import { formKey as TestForm1_formKey } from './TestForm1';
import { formKey as TestForm2_formKey } from './TestForm2';
import { formKey as TestForm3_formKey } from './TestForm3';

const TestForm1_TaskListCell = lazy(() => import('./TestForm1/List'));
const TestForm2_TaskListCell = lazy(() => import('./TestForm2/List'));
const TestForm3_TaskListCell = lazy(() => import('./TestForm3/List'));

const TaskListCell = ({
    formKey,
    column
  }: {
    formKey: string,
    column: string
  }) => 
    formKey === TestForm1_formKey
        ? <TestForm1_TaskListCell column={ column } />
        : formKey === TestForm2_formKey
        ? <TestForm2_TaskListCell column={ column } />
        : formKey === TestForm3_formKey
        ? <TestForm3_TaskListCell column={ column } />
        : <></>;

export default TaskListCell;
