import React from "react";
import { Box, Layer, ThemeType, ThemeContext } from "grommet";
import { Cycle } from "grommet-icons";
import { theme as appTheme } from '../app/App';
import { deepMerge } from 'grommet/utils';

const LoadingIndicator = () => {
  const theme: ThemeType = deepMerge(appTheme, {
    layer: {
      overlay: {
        background: 'rgba(0, 0, 0, 0)',
      },
    },
  });
  
  return (
      <ThemeContext.Extend value={ theme }>
        <Layer
            plain
            animate={false}
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
        </Layer>
      </ThemeContext.Extend>);
};

export { LoadingIndicator };
