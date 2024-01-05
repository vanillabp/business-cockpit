import React from "react";
import { Box, Layer, Spinner } from "grommet";

export type ShowLoadingIndicatorFunction = (show: boolean) => void;

const LoadingIndicator = () => (
    <Layer
        plain
        animate={false}
        background={ { color: 'rgba(0, 0, 0, 0)' } }
        responsive={false}
        modal={true}>
      <Box
          round='medium'
          background={ { color: 'rgba(0, 0, 0, 0.3)' } }>
        <Box
            animation="rotateRight"
            pad="medium">
          <Spinner
              size="medium"
              border={[
                { side: 'all', color: 'rgba(0, 0, 0, 0)', size: 'medium' },
                { side: 'right', color: 'white', size: 'medium' },
                { side: 'top', color: 'white', size: 'medium' },
                { side: 'left', color: 'white', size: 'medium' },
              ]}
          />
        </Box>
      </Box>
    </Layer>);

export { LoadingIndicator };
