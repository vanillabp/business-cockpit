import { ShowLoadingIndicatorFunction, TranslationFunction, UserTaskAppLayout } from '@vanillabp/bc-shared';
import { NoElementGivenByModule } from './index.js';

const NoUserTaskGiven = ({
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
      <UserTaskAppLayout>
        <NoElementGivenByModule
            loading={ loading }
            t={ t }
            showLoadingIndicator={ showLoadingIndicator }
            retry={ retry } />
      </UserTaskAppLayout>);
}

export { NoUserTaskGiven };
