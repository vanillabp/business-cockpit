import React, { useLayoutEffect } from 'react';
import { Box } from 'grommet';
import { useAppContext } from '../AppContext';

const WorkflowlistApp = () => {

  const { setAppHeaderTitle } = useAppContext();

  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);

  return (
    <Box>Workflowlistapp</Box>);

};

export default WorkflowlistApp;
