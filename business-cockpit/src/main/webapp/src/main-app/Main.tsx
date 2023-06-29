import React, { useLayoutEffect } from 'react';
import { useAppContext } from '../AppContext';
import { Box } from 'grommet';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';

i18n.addResources('en', 'main/welcome', {
    "hello": "Welcome to {{title}}"
  });
i18n.addResources('de', 'main/welcome', {
    "hello": "Willkommen beim {{title}}"
  });
  
const Main = () => {

  const { state, setAppHeaderTitle } = useAppContext();
  const { t } = useTranslation('main/welcome');
  const { t: tTitle } = useTranslation(state.title);
  
  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);
  
  return (
    <Box
        fill
        pad={ { top: 'large' } }
        align='center'>
      <Box>{ t('hello', { title: tTitle('title.long') } ) }</Box>
    </Box>);

}

export { Main };
