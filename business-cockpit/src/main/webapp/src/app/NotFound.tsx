import React from 'react';
import { Box, Heading, Text } from 'grommet';
import { useTranslation } from 'react-i18next';

const NotFound = () => {
  
  const { t } = useTranslation('app');

  return (
    <Box
        direction='column'
        fill='horizontal'
        flex='shrink'
        align='center'
        gap='medium'
        pad='medium'
        width='medium'>
      <Heading level='3'>{ t('not-found') }</Heading>
      <Text>{ t('not-found hint') }</Text>
    </Box>);

}

export { NotFound };
