import React from 'react';
import { Anchor, Box, Text } from 'grommet';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

const Main = () => {
  
  const { t } = useTranslation('app');
  const navigate = useNavigate();
  
  return (
      <Box
          margin="large"
          gap="medium">
        <Text weight='bold'>
          { t('title.long') }
        </Text>
        <Anchor
            color='accent-2'
            onClick={ () => navigate(t('url-usertask') as string) }>
          { t('link-usertask') }
        </Anchor>
        <Anchor
            color='accent-2'
            onClick={ () => navigate(t('url-workflow') as string) }>
          { t('link-workflow') }
        </Anchor>
      </Box>);
  
};

export { Main };
