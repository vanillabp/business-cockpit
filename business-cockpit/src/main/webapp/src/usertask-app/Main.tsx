import React, { useEffect } from 'react';
import { useUserTaskAppContext } from './UserTaskAppContext';
import { UserTaskAppLayout } from '@bc/shared';
import { useFederationModule } from '../utils/module-federation';
import { useAppContext } from '../AppContext';
import { NoUserTaskGiven } from './NoUserTaskGiven';

const Main = () => {

  const { showLoadingIndicator } = useAppContext();
  const { userTask } = useUserTaskAppContext();
  
  document.title = userTask!.title.de;

  const module = useFederationModule(userTask.workflowModule, 'Form');

  useEffect(() => {
      if (!module) {
        showLoadingIndicator(true);
        return;
      }
      showLoadingIndicator(false);
    }, [ module, showLoadingIndicator ]);
  
  if (!module) {
    return <NoUserTaskGiven loading />
  }
  if (module.retry) {
    return <NoUserTaskGiven retry={ module.retry } />
  }
    
  const Form = module.UserTaskForm!;
  
  return (
      <UserTaskAppLayout>
        <Form userTask={ userTask } />
      </UserTaskAppLayout>);

}

export { Main };
