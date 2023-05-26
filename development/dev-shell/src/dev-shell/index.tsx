import { createRoot } from 'react-dom/client';
import { DevShellApp } from './app/DevShellApp.js';
import { AppContextProvider } from './DevShellAppContext.js';
import { UserTaskForm } from '@vanillabp/bc-shared';
import "@fontsource/roboto/latin-300.css";
import "@fontsource/roboto/files/roboto-latin-300-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-300-normal.woff";
import "@fontsource/roboto/latin-400.css";
import "@fontsource/roboto/files/roboto-latin-400-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-400-normal.woff";
import "@fontsource/roboto/latin-500.css";
import "@fontsource/roboto/files/roboto-latin-500-normal.woff2";
import "@fontsource/roboto/files/roboto-latin-500-normal.woff";

const bootstrapDevShell = (
  elementId: string,
  officialGuiApiUrl: string,
  userTaskForm: UserTaskForm,
) => {
  const container = document.getElementById(elementId);
  const root = createRoot(container!);
  root.render(
    <AppContextProvider>
      <DevShellApp
          userTaskForm={ userTaskForm }
          officialGuiApiUrl={ officialGuiApiUrl } />
    </AppContextProvider>)
};

export { bootstrapDevShell };
