import React, { Dispatch } from "react";
import { Box, Layer } from "grommet";
import { Cycle } from "grommet-icons";
import { Action } from '../types/AppContext';

export type LoadingIndicatorDispatchType = 'toast';
export interface LoadingIndicatorAction extends Action<LoadingIndicatorDispatchType> {
  toast: boolean | undefined;
};

export interface LoadingIndicatorDispatch extends Dispatch<LoadingIndicatorAction> {
};

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
