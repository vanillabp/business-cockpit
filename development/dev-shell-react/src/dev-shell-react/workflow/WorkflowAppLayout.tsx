import React, { PropsWithChildren, ReactNode } from 'react';
import { Box, Header } from 'grommet';

interface FrameProps {
  header?: ReactNode | undefined;
};

const WorkflowAppLayout = ({
  header,
  children
}: PropsWithChildren<FrameProps>) => {

  return (
      <Box
          direction='column'
          fill>
        <Header
            tag='header'
            background='white'
            elevation='medium'
            height='xxsmall'
            pad='xxsmall'>
          {
            header
          }
        </Header>
        <Box
            fill
            overflow={ { vertical: 'auto' } }>
          {
            children
          }
        </Box>
      </Box>);

}

export { WorkflowAppLayout };
