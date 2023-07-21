import React, { useEffect, useState } from 'react';
import { buildTimestamp, buildVersion } from '../WorkflowPage';
import { WorkflowPage } from '@vanillabp/bc-shared';
import { Box, List, Text } from 'grommet';
import { UserTask } from "@vanillabp/bc-official-gui-client";

const TestWorkflowPage: WorkflowPage = ({ workflow }) => {
  const [ loaded, setLoaded ] = useState(false);
  const [ userTasks, setUserTasks ] = useState<Array<UserTask> | undefined>(undefined);
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
  return (<Box>
            <Box>
              Workflow: '{workflow.title.de}' { buildVersion } from { buildTimestamp.toLocaleString() }
            </Box>
            {
              userTasks === undefined
                  ? <Text>Loading user tasks...</Text>
                  : userTasks.length === 0
                  ? <Text>No active user tasks found for this workflow!</Text>
                  : <List
                          primaryKey="id"
                          secondaryKey="title.de"
                          data={ userTasks } />
            }
          </Box>);
}

export default TestWorkflowPage;
