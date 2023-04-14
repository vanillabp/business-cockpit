import React from 'react';
import { Box } from 'grommet';
import { useUserTaskAppContext } from './UserTaskAppContext';
import { UserTaskAppLayout } from './UserTaskAppLayout';

const Main = () => {

  const { userTask } = useUserTaskAppContext();
  
  document.title = userTask.title.de;

  return (
      <UserTaskAppLayout>
        { userTask.title.de }
      </UserTaskAppLayout>);

}

export { Main };
