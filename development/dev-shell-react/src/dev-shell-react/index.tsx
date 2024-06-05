import { createRoot } from 'react-dom/client';
import { DevShellApp } from './app/DevShellApp.js';
import { AppContextProvider } from './DevShellAppContext.js';
import {
  BcWorkflowModule,
  ColumnsOfUserTaskFunction,
  ColumnsOfWorkflowFunction,
  UserTaskForm,
  UserTaskListCell,
  WorkflowListCell,
  WorkflowModuleComponent,
  WorkflowPage
} from '@vanillabp/bc-shared';
import "@fontsource/roboto/latin-300.css";
import "@fontsource/roboto/files/roboto-latin-300-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-300-normal.woff";
import "@fontsource/roboto/latin-400.css";
import "@fontsource/roboto/files/roboto-latin-400-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-400-normal.woff";
import "@fontsource/roboto/latin-500.css";
import "@fontsource/roboto/files/roboto-latin-500-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-500-normal.woff";
import { ThemeType } from "grommet";


const bootstrapDevShell = (
  elementId: string,
  workflowModule: BcWorkflowModule,
  theme: ThemeType,
  officialGuiApiUrl: string,
  userTaskForm: UserTaskForm,
  userTaskListColumns: ColumnsOfUserTaskFunction,
  userTaskListCell: UserTaskListCell,
  workflowListColumns: ColumnsOfWorkflowFunction,
  workflowListCell: WorkflowListCell,
  workflowPage: WorkflowPage,
  additionalComponents?: Record<string, WorkflowModuleComponent>,
) => {
  const container = document.getElementById(elementId);
  const root = createRoot(container!);
  root.render(
    <AppContextProvider>
      <DevShellApp
          workflowModule={ workflowModule }
          theme={ theme }
          userTaskForm={ userTaskForm }
          userTaskListCell={ userTaskListCell }
          userTaskListColumns={ userTaskListColumns }
          workflowListCell={ workflowListCell }
          workflowListColumns={ workflowListColumns }
          workflowPage={ workflowPage }
          officialGuiApiUrl={ officialGuiApiUrl }
          additionalComponents={ additionalComponents } />
    </AppContextProvider>)
};

export { bootstrapDevShell };
