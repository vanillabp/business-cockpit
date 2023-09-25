import React, { lazy } from 'react';
import { Main } from './Main';
import { Box } from 'grommet';
import { Route, Routes } from 'react-router-dom';
import { AppHeader } from './menu/AppHeader';
import { useTranslation } from 'react-i18next';
import { NotFound } from '../app/NotFound';

const TaskList = lazy(() => import('./tasklist/Main'));
const WorkflowList = lazy(() => import('./workflowlist/Main'));

const MainApp = () => {

  const { t } = useTranslation('app');

  return (
    <>
      <AppHeader />
      <Box
          direction='row'
          fill
          style={ { display: 'unset' } } /* to avoid removing bottom margin of inner boxes */
          overflow={ { horizontal: 'hidden' } }>
        <Routes>
          <Route path={ `${ t('url-tasklist') }/*` } element={<TaskList />} />
          <Route path={ `${ t('url-workflowlist') }/*` } element={<WorkflowList />} />
          <Route index element={<Main />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Box>
    </>);

};

export default MainApp;
