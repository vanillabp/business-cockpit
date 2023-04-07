import React from 'react';
import { createRoot } from 'react-dom/client';
import { UserTaskForm } from './Form';
import "@fontsource/roboto/latin-300.css";
import "@fontsource/roboto/files/roboto-latin-300-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-300-normal.woff";
import "@fontsource/roboto/latin-400.css";
import "@fontsource/roboto/files/roboto-latin-400-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-400-normal.woff";
import "@fontsource/roboto/latin-500.css";
import "@fontsource/roboto/files/roboto-latin-500-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-500-normal.woff";

// adopt ISO string according to server which is using localdate.
// therefore we assume that user have the same timezone as the
// server.
// eslint-disable-next-line
Date.prototype.toISOString = function() {
    return `${this.getFullYear()}-${String(this.getMonth() + 1).padStart(2, '0')}-${String(this.getDate()).padStart(2, '0')}T${String(this.getHours()).padStart(2, '0')}:${String(this.getMinutes()).padStart(2, '0')}:${String(this.getSeconds()).padStart(2, '0')}.${String(this.getMilliseconds()).padStart(3, '0')}`;
  };

const container = document.getElementById('root');
const root = createRoot(container!);
root.render(<UserTaskForm bpmnProcessId='Test' formKey='TestForm' />);
