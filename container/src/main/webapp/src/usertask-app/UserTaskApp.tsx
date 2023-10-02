import { useLayoutEffect } from 'react';
import { Box } from 'grommet';
import { Route, Routes, useParams } from 'react-router-dom';
import { useAppContext } from '../AppContext';
import { NoUserTaskGiven, UserTaskPage } from '@vanillabp/bc-ui';
import { navigateToWorkflow, openTask } from "../utils/navigate";
import { useTasklistApi } from "../utils/apis";

const RouteBasedUserTaskApp = () => {
  const { showLoadingIndicator, toast } = useAppContext();
  const userTaskId: string | undefined = useParams()['*'];

  if (userTaskId === undefined) {
    return <NoUserTaskGiven
              showLoadingIndicator={ showLoadingIndicator } />;
  }

  return (<UserTaskPage
              userTaskId={ userTaskId }
              useTasklistApi={ useTasklistApi }
              showLoadingIndicator={ showLoadingIndicator }
              toast={ toast }
              openTask={ openTask }
              navigateToWorkflow={ navigateToWorkflow } />)
}

const UserTaskApp = () => {

  const { setAppHeaderTitle, showLoadingIndicator } = useAppContext();

  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);

  return (
      <Box
          direction='row'
          fill
          style={ { display: 'unset' } } /* to avoid removing bottom margin of inner boxes */
          overflow={ { horizontal: 'hidden' } }>
        <Routes>
          <Route index element={<NoUserTaskGiven showLoadingIndicator={ showLoadingIndicator } />} />
          <Route path="/:userTaskId" element={ <RouteBasedUserTaskApp /> } />
        </Routes>
      </Box>);

};

export default UserTaskApp;
