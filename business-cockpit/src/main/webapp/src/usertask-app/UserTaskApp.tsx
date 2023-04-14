import React, { Suspense, useLayoutEffect } from 'react';
import { Main } from './Main';
import { Box } from 'grommet';
import { Route, Routes } from 'react-router-dom';
import { useAppContext } from '../AppContext';
import { NoUserTaskGiven } from './NoUserTaskGiven';
import { UserTaskAppContextProvider } from './UserTaskAppContext';

const UserTaskApp = () => {

  const { setAppHeaderTitle } = useAppContext();

  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);

  return (
    <Suspense fallback={ <NoUserTaskGiven loading={ true } /> }>
      <UserTaskAppContextProvider>
        <Box
            direction='row'
            fill
            style={ { display: 'unset' } } /* to avoid removing bottom margin of inner boxes */
            overflow={ { horizontal: 'hidden' } }>
          <Routes>
            <Route index element={<NoUserTaskGiven />} />
            <Route path="/:userTaskId" element={<Main />} />
          </Routes>
        </Box>
      </UserTaskAppContextProvider>
    </Suspense>);

};

export default UserTaskApp;
