import React from 'react';
import { buildTimestamp, buildVersion } from '../../UserTaskForm';
import { UserTaskForm } from '@vanillabp/bc-shared';

const TestForm1: UserTaskForm = ({ userTask }) => <div>TestForm1: '{userTask.title.de}' { buildVersion } from { buildTimestamp.toLocaleString() }</div>

export default TestForm1;
