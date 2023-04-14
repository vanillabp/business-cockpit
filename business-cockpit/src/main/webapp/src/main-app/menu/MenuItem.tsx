import React, { PropsWithChildren } from 'react';
import { AnchorExtendedProps, Box, BoxProps } from 'grommet';
import { Role } from '../../client/gui';
import { BackgroundType } from 'grommet/utils';
import { useCurrentUserRoles } from '../../utils/roleUtils';

interface MenuItemProps extends PropsWithChildren<BoxProps> {
  href?: string;
  roles?: Array<Role> | null;
  background?: BackgroundType;
};

const MenuItem = ({
  children,
  href,
  background = 'light-4',
  roles = [],
  ...props
}: MenuItemProps) => {
  
  const { hasOneOfRoles } = useCurrentUserRoles();
  if (!hasOneOfRoles(roles)) {
    return <></>;
  }
  
  (props as AnchorExtendedProps).href = href;
  return (
  <Box
      {...props}
      round
      hoverIndicator='brand'
      direction='row'
      background={ background }
      pad={{ top: 'small', bottom: 'small', left: 'medium' }}
      gap='small'
      margin={{ top: 'medium' }}
      style={{ textDecoration: 'none' }}
      as={ href !== undefined ? 'a' : undefined }>{ children }</Box>
)};

export { MenuItem };
