import React from 'react';
import { buildTimestamp, buildVersion } from '../../Form';
import { UserTaskForm } from '@bc/shared';

const TestForm3: UserTaskForm = ({ userTask }) => <div>TestForm3: '{userTask.title.de}' { buildVersion } from { buildTimestamp.toLocaleString() }</div>

export default TestForm3;
