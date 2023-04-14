import { useLayoutEffect } from 'react';
import { useAppContext } from '../AppContext';
import { Box } from 'grommet';

const Main = () => {

  const { setAppHeaderTitle } = useAppContext();

  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);
  
  return <Box>Main</Box>;

}

export { Main };
