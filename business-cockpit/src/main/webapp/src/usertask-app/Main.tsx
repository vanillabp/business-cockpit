import React, { useEffect } from 'react';
import { useUserTaskAppContext } from './UserTaskAppContext';
import { UserTaskAppLayout } from '@vanillabp/bc-shared';
import { ModuleDefinition, useFederationModule } from '../utils/module-federation';
import { useAppContext } from '../AppContext';
import { NoUserTaskGiven } from './NoUserTaskGiven';

const Main = () => {

  const { showLoadingIndicator } = useAppContext();
  const { userTask } = useUserTaskAppContext();
  
  document.title = userTask!.title.de;

  const module = useFederationModule(userTask as ModuleDefinition, 'UserTaskForm');

  useEffect(() => {
      if (!module) {
        showLoadingIndicator(true);
        return;
      }
      showLoadingIndicator(false);
    }, [ module, showLoadingIndicator ]);
  
  if (module?.retry) {
    return <NoUserTaskGiven retry={ module.retry } />
  }
  if (!module || (module.buildTimestamp === undefined)) {
    return <NoUserTaskGiven loading />
  }
    
  const Form = module.UserTaskForm!;
  
  return (
      <UserTaskAppLayout>
        <Form userTask={ userTask } />
      </UserTaskAppLayout>);

}

export { Main };
