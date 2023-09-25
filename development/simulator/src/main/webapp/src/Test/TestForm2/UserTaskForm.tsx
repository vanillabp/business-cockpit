import React from 'react';
import { buildTimestamp, buildVersion } from '../../UserTaskForm';
import { UserTaskForm } from '@vanillabp/bc-shared';

const TestForm2: UserTaskForm = ({ userTask }) => <div>TestForm2: '{userTask.title.de}' { buildVersion } from { buildTimestamp.toLocaleString() }</div>

export default TestForm2;
