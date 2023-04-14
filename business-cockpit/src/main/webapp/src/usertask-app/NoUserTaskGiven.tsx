import React, { useEffect } from 'react';
import { Box, Text } from 'grommet';
import { useAppContext } from '../AppContext';
import { UserTaskAppLayout } from './UserTaskAppLayout';

const NoUserTaskGiven = ({
  loading = false
}) => {
  
  const { showLoadingIndicator } = useAppContext();
  
  useEffect(() => {
      showLoadingIndicator(loading);
    }, []);

  return (
      <UserTaskAppLayout>
        {
          loading
              ? <></>
              : <Box
                   fill='horizontal'
                   pad='medium'
                   align="center">
                 <Text
                    weight='bold'>Die Aufgabe existiert nicht!</Text>
                </Box>
        }
      </UserTaskAppLayout>);

}

export { NoUserTaskGiven };
