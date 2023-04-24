import React, { PropsWithChildren } from 'react';
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
            height="4rem"
            background="light-4"
            fill='horizontal'>
          {
            header
          }
        </Box>
        <Box
            fill>
          {
            children
          }
        </Box>
        <Box
            height="2rem"
            background="light-4"
            fill='horizontal'>
          {
            footer
          }
        </Box>
      </Box>);

}

export { UserTaskAppLayout };
