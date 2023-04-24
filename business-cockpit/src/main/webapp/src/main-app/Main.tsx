import { useLayoutEffect } from 'react';
import { useAppContext } from '../AppContext';
import { Box } from 'grommet';
import { Test } from '@bc/shared';

const Main = () => {

  const { setAppHeaderTitle } = useAppContext();

  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);
  
  return <Box>Main: <Test /></Box>;

}

export { Main };
