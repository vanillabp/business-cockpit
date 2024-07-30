import { Box, Keyboard, Menu, TextInput } from 'grommet';
import { Search } from 'grommet-icons';
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { useResponsiveScreen } from '@vanillabp/bc-shared';
import { appNs } from '../app/DevShellApp.js';
import i18n from '../i18n.js';
import { ButtonExtendedProps } from "grommet/components/Button";

i18n.addResources('en', 'usertask-header', {
      "views-label": "View",
      "view-form": "form",
      "view-icon": "icon",
      "view-list": "list",
    });
i18n.addResources('de', 'usertask-header', {
      "views-label": "Ansicht",
      "view-form": "Formular",
      "view-icon": "Symbol",
      "view-list": "Liste",
    });

const Header = () => {
  
  const { isPhone } = useResponsiveScreen();
  const navigate = useNavigate();
  const { t: tApp } = useTranslation(appNs);
  const { t } = useTranslation('usertask-header');

  const userTaskId: string | undefined = useParams()['userTaskId'];

  const [ taskId, setTaskId ] = useState(userTaskId);

  const switchView = (target: string) => navigate(`/${ tApp('url-usertask') }/${taskId}${target}`);
  
  const loadUserTask = () => navigate(`/${ tApp('url-usertask') }/${taskId}`, { replace: true });
  
  const viewMenuItems: ButtonExtendedProps[] = [
      { label: t('view-form'), onClick: () => switchView('') },
      { label: t('view-list'), onClick: () => switchView(`/${tApp('url-list')}`) },
      { label: t('view-icon'), onClick: () => switchView(`/${tApp('url-icon')}`) },
    ];

  return (
      <Box
          fill
          direction="row"
          justify="between"
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
        <Box>
          <Menu
              disabled={ !Boolean(userTaskId) }
              label={ t('views-label') }
              items={ viewMenuItems } />
        </Box>
      </Box>);
};

export { Header };
