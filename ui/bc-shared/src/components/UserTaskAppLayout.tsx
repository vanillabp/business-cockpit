import React, { PropsWithChildren, ReactNode } from 'react';
import { Box } from 'grommet';

interface FrameProps {
  header?: ReactNode | undefined;
  footer?: ReactNode | undefined;
};

const UserTaskAppLayout = ({
  header,
  footer,
  children
}: PropsWithChildren<FrameProps>) => {

  return (
      <Box
          direction='column'
          fill>
        <Box
            flex="grow"
            background="light-4"
            fill='horizontal'>
          {
            header
          }
        </Box>
        <Box
            overflow={ { vertical: "auto" } }
            fill>
          {
            children
          }
        </Box>
        <Box
            flex="grow"
            background="light-4"
            fill='horizontal'>
          {
            footer
          }
        </Box>
      </Box>);

}

export { UserTaskAppLayout };
