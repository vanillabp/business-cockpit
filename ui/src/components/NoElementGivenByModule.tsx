import { useEffect } from 'react';
import { Box, Button, Text } from 'grommet';
import { useTranslation } from 'react-i18next';
import { ShowLoadingIndicatorFunction } from "@vanillabp/bc-shared";

const NoElementGivenByModule = ({
  loading = false,
  retry,
  translationNs,
  showLoadingIndicator,
}: {
  loading?: boolean,
  retry?: (callback?: () => void) => void
  translationNs: string;
  showLoadingIndicator: ShowLoadingIndicatorFunction;
}) => {
  
  const { t } = useTranslation(translationNs);

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
