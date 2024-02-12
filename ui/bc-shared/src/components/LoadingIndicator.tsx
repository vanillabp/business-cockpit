import React from "react";
import { Box, Layer, Spinner } from "grommet";
import styled from "styled-components";

export type ShowLoadingIndicatorFunction = (show: boolean) => void;

// Grommet includes a hidden a-tag to modal layers setting the focus on
// if the modal content does not catch the focus. This brings a focus indicator
// at least to Firefox browsers which has to be disabled.
// see https://github.com/grommet/grommet/issues/7128
const NoFocusIndicatorLayer = styled(Layer)`
  a:focus {
    outline: none;
  }
`;

const LoadingIndicator = () => (
    <NoFocusIndicatorLayer
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
    </NoFocusIndicatorLayer>);

export { LoadingIndicator };
