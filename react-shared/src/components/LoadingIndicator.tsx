import React from "react";
import { Box, Layer } from "grommet";
import { Cycle } from "grommet-icons";

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
            pad='medium'>
          <Cycle
              style={ { marginTop: '3px' } }
              color="white"
              size="large" />
        </Box>
      </Box>
    </Layer>);

export { LoadingIndicator };
