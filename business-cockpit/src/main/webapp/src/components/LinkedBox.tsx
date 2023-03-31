import React, { PropsWithChildren } from 'react';
import { Box, BoxProps } from 'grommet';

interface LinkedBoxProps extends PropsWithChildren<BoxProps> {
  href?: string,
};

const LinkedBox = ({
  children,
  ...props
}: LinkedBoxProps) => (
  <Box
      {...props}
      style={{ textDecoration: 'none' }}
      as='a'>{ children }</Box>
);

export { LinkedBox };
