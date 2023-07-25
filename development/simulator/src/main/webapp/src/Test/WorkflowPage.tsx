import React, { useEffect, useState } from 'react';
import { buildTimestamp, buildVersion } from '../WorkflowPage';
import { BcUserTask, WorkflowPage, theme } from '@vanillabp/bc-shared';
import { Box, Grommet, Heading, Text } from 'grommet';
import { Share } from 'grommet-icons';

const TestWorkflowPage: WorkflowPage = ({ workflow }) => {
  const [ loaded, setLoaded ] = useState(false);
  const [ userTasks, setUserTasks ] = useState<Array<BcUserTask> | undefined>(undefined);

  useEffect(() => {
    const loadUserTasks = async () => {
      const tasks = await workflow.getUserTasks(true, false);
      setUserTasks(tasks);
    };
    if (!loaded) {
      setLoaded(true);
      loadUserTasks();
    }
  }, [ setUserTasks, workflow, loaded, setLoaded ]);
  
  return (
        <Grommet
            theme={theme}
            full>
          <Box
            gap='small'
            margin="medium">
            <Heading
                level="2">
              '{workflow.title.de}' { buildVersion } from { buildTimestamp.toLocaleString() }
            </Heading>
            {
              userTasks === undefined
                  ? <Text>Loading user tasks...</Text>
                  : userTasks.length === 0
                  ? <Text>No active user tasks found for this workflow!</Text>
                  : <Box
                        margin="medium"
                        direction='column'>
                      {
                        userTasks.map(userTask => (
                          <Box
                              gap="small"
                              direction="row">
                            <Share
                                color="dark-2"
                                size="15rem"
                                style={ { position: 'relative', top: '-0.1rem' } }
                                onClick={ () => userTask.open() }
                              />
                            <Box>{ userTask.id }</Box>
                          </Box>))
                      }
                    </Box>
            }
          </Box>
        </Grommet>);
}

export default TestWorkflowPage;
