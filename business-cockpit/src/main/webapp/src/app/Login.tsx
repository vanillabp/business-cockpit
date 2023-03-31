import { useLayoutEffect } from 'react';
import { useAppContext } from '../AppContext';
import { Box, Button } from 'grommet';

const Login = () => {

  const { setAppHeaderTitle } = useAppContext();

  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);
  
  return <Box
        pad="small">
      <Box align="center">Login required!</Box>
    </Box>;

}

export { Login };
