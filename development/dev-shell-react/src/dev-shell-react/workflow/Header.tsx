import { Box, Keyboard, TextInput, Menu } from 'grommet';
import { Search } from 'grommet-icons';
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { useResponsiveScreen } from '@vanillabp/bc-shared';
import { appNs } from '../app/DevShellApp.js';
import i18n from '../i18n.js';

i18n.addResources('en', 'workflow-header', {
      "views-label": "View",
      "view-page": "page",
      "view-icon": "icon",
      "view-list": "list",
    });
i18n.addResources('de', 'workflow-header', {
      "views-label": "Ansicht",
      "view-page": "Seite",
      "view-icon": "Symbol",
      "view-list": "Liste",
    });

const Header = () => {
  
  const { isPhone } = useResponsiveScreen();
  const navigate = useNavigate();
  const { t: tApp } = useTranslation(appNs);
  const { t } = useTranslation('workflow-header');

  const workflowIdParam: string | undefined = useParams()['workflowId'];

  const [ workflowId, setWorkflowId ] = useState(workflowIdParam);

  const switchView = (target: string) => navigate(`/${ tApp('url-workflow') }/${workflowId}${target}`);
  
  const loadWorkflow = () => navigate(`/${ tApp('url-workflow') }/${workflowId}`, { replace: true });
  
  const viewMenuItems = [
      { label: t('view-page'), onClick: () => switchView('') },
      { label: t('view-list'), onClick: () => switchView(`/${tApp('url-list')}`) },
      { label: t('view-icon'), onClick: () => switchView(`/${tApp('url-icon')}`) }
    ];
  
  return (
      <Box
          fill
          direction="row"
          justify="between"
          pad="xsmall">
        <Box
            width={ isPhone ? '15rem' : '26rem' }
            height={ { max: 'xsmall' } }
            pad={ isPhone ? 'small' : 'xsmall' }
            border="all"
            round="small"
            justify='center'>
          <Keyboard
              onEnter={ (event) => loadWorkflow() }>
            <TextInput
                icon={<Search />}
                reverse
                size={ isPhone ? '1rem' : 'medium' }
                plain="full"
                value={ workflowId }
                onChange={ (event) => setWorkflowId(event.target.value) }
                placeholder="workflow ID" />
          </Keyboard>
        </Box>
        <Box>
          <Menu
              disabled={ !Boolean(workflowId) }
              label={ t('views-label') }
              items={ viewMenuItems } />
        </Box>
      </Box>);
};

export { Header };
