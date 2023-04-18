import React from 'react';
import { buildTimestamp, buildVersion } from '../../Form';

const TestForm2 = () => <div>TestForm2: { buildVersion } from { buildTimestamp.toLocaleString() }</div>

export default TestForm2;
