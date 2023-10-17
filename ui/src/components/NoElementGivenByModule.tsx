import { useEffect } from 'react';
import { Box, Button, Text } from 'grommet';
import { ShowLoadingIndicatorFunction } from "@vanillabp/bc-shared";
import { TranslationFunction } from "../types/translate";

const NoElementGivenByModule = ({
  loading = false,
  retry,
  showLoadingIndicator,
  t,
}: {
  loading?: boolean,
  retry?: (callback?: () => void) => void
  showLoadingIndicator: ShowLoadingIndicatorFunction;
  t: TranslationFunction;
}) => {
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
                    { t('retry-loading-module-hint') }
                  </Box>
                  <Button
                      label={ t('retry-loading-module') }
                      onClick={ retryLoadingModule } />
                </Box>
              : <Text
                    weight='bold'>
                  { t('module-unknown') }
                </Text>
        }
      </Box>);

}

export { NoElementGivenByModule };
