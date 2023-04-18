import React from 'react';
import { buildTimestamp, buildVersion } from '../../Form';

const TestForm1 = () => <div>TestForm1: { buildVersion } from { buildTimestamp.toLocaleString() }</div>

export default TestForm1;
