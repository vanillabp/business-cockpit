import { NoElementGivenByModule } from './index.js';
import { ShowLoadingIndicatorFunction, TranslationFunction } from "@vanillabp/bc-shared";

const NoWorkflowGiven = ({
  loading = false,
  retry,
  showLoadingIndicator,
  t,
}: {
  loading?: boolean,
  retry?: (callback?: () => void) => void,
  showLoadingIndicator: ShowLoadingIndicatorFunction,
  t: TranslationFunction,
}) => {
  return (
      <NoElementGivenByModule
          loading={ loading }
          showLoadingIndicator={ showLoadingIndicator }
          retry={ retry }
          t={ t } />);

}

export { NoWorkflowGiven };
