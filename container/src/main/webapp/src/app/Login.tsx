import { useEffect, useLayoutEffect } from 'react';
import { useAppContext } from '../AppContext';
import { Anchor, Box } from 'grommet';
import { useNavigate } from "react-router-dom";

const Login = () => {

  const { setAppHeaderTitle, state } = useAppContext();
  const navigate = useNavigate();

  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);

  useEffect(() => {
    if (Boolean(state.currentUser)) {
      navigate('/');
    }
  }, [ state, navigate ]);

  return <Box
        align="center"
        pad="small">
      <Box
          direction='row'
          gap='xsmall'>
        <Anchor
            color='accent-2'
            weight='normal'
            href='/login'>
          Login
        </Anchor>
        required!
      </Box>
    </Box>;

}

export { Login };
