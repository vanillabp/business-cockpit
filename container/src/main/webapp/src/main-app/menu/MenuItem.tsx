import { PropsWithChildren } from 'react';
import { AnchorExtendedProps, Box, BoxProps } from 'grommet';
import { BackgroundType } from 'grommet/utils';
import { useCurrentUserGroups } from "../../utils/roleUtils";

interface MenuItemProps extends PropsWithChildren<BoxProps> {
  href?: string;
  groups?: Array<string> | null;
  background?: BackgroundType;
};

const MenuItem = ({
  children,
  href,
  background = 'light-4',
  groups = [],
  ...props
}: MenuItemProps) => {
  
  const { hasOneOfGroups } = useCurrentUserGroups();
  if (!hasOneOfGroups(groups)) {
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
