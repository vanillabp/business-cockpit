import { Box, Keyboard, TextInput } from 'grommet';
import { Search } from 'grommet-icons';
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { useResponsiveScreen } from '../../utils/responsiveUtils.js';
import { appNs } from './DevShellApp.js';

const Header = () => {
  
  const { isPhone } = useResponsiveScreen();
  const navigate = useNavigate();
  const { t: tApp } = useTranslation(appNs);
  const userTaskId: string | undefined = useParams()['userTaskId'];

  const [ taskId, setTaskId ] = useState(userTaskId);

  const loadUserTask = () => navigate(`/${ tApp('url-usertask') }/${taskId}`);
  
  return (
      <Box
          fill
          direction="column"
          justify="center"
          pad="small">
        <Box
            width={ isPhone ? '15rem' : '26rem' }
            height={ { max: 'xsmall' } }
            pad={ isPhone ? 'small' : 'xsmall' }
            border="all"
            round="small"
            justify='center'>
          <Keyboard
              onEnter={ (event) => loadUserTask() }>
            <TextInput
                icon={<Search />}
                reverse
                size={ isPhone ? '1rem' : 'medium' }
                plain="full"
                value={ taskId }
                onChange={ (event) => setTaskId(event.target.value) }
                placeholder="task ID" />
          </Keyboard>
        </Box>
      </Box>);
};

export { Header };
