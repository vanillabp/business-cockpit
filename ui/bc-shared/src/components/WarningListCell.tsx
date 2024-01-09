import React from 'react';
import { Box, Text } from 'grommet';
import { Alert, StatusCritical } from 'grommet-icons';
import { useResponsiveScreen } from '../utils/index.js';

const WarningListCell = ({
  message,
  error = false,
}: {
  message: string;
  error?: boolean;
}) => {
  const { isNotPhone } = useResponsiveScreen();

  const Icon = error ? StatusCritical : Alert;
  const color = error ? 'status-critical' : 'status-warning';

  return (
      <Box
          fill
          align="center"
          direction='row'
          background={ { color, opacity: 'medium' } }
          justify='start'
          pad="xxsmall"
          gap="xsmall">
        <Icon
            color={ color }
            size="15rem"
            a11yTitle={ message } />
        {
          isNotPhone
              ? <Text truncate="tip">{ message }</Text>
              : undefined
        }
      </Box>);
}

export { WarningListCell };
