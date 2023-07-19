import React, { useEffect } from 'react';
import { Box, Text, Button } from 'grommet';
import { useAppContext } from '../AppContext';
import { useTranslation } from 'react-i18next';

const NoElementGivenByModule = ({
  loading = false,
  retry,
  translationNs,
}: {
  loading?: boolean,
  retry?: (callback?: () => void) => void
  translationNs: string;
}) => {
  
  const { t } = useTranslation(translationNs);
  
  const { showLoadingIndicator } = useAppContext();
  
  useEffect(() => {
      showLoadingIndicator(loading);
    }, [ showLoadingIndicator, loading ]);

  const retryLoadingModule = () => {
    showLoadingIndicator(true);
    if (retry) {
      retry(() => showLoadingIndicator(false));
    }
  };
  
  return (
      <Box
         fill='horizontal'
         margin='large'
         align="center">
        {
          loading
              ? undefined
              : retry
              ? <Box
                    direction="column"
                    gap='medium'>
                  <Box>
                    { t('hint') }
                  </Box>
                  <Button
                      label={ t('retry') }
                      onClick={ retryLoadingModule } />
                </Box>
              : <Text
                    weight='bold'>
                  { t('does-not-exist') }
                </Text>
        }
      </Box>);

}

export { NoElementGivenByModule };
