import { ShowLoadingIndicatorFunction, UserTaskAppLayout } from '@vanillabp/bc-shared';
import { NoElementGivenByModule } from './index.js';
import { TranslationFunction } from "../types/translate";

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
