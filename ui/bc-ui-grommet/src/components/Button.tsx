import { BcUiButtonFC } from '@vanillabp/bc-ui-types';
import { Box, Tip } from "grommet";
import { cloneElement, ReactElement } from "react";

const Button: BcUiButtonFC = ({
    disabled,
    fill,
    size,
    icon,
    tip,
    onClick,
    group,
    children
}) => {
  const color = disabled ? 'light-4' : 'dark-3';
  const button = (
      <Box
          hoverIndicator="light-2"
          focusIndicator={ false }
          onClick={ onClick }
          width="2rem"
          height="2rem"
          round={ group === "first"
              ? { size: '0.4rem', corner: 'left' }
              : group === "last"
              ? { size: '0.4rem', corner: 'right' }
              : undefined }
          border={ group === "first" ? undefined : { color, side: "left" } }
          align="center"
          justify="center">
        {
            icon === undefined ? undefined : cloneElement(icon as ReactElement, { disabled })
        }
        {
            children
        }
      </Box>);
  return tip === undefined
      ? button
      : <Tip
            content={ tip }>
          {
            button
          }
        </Tip>
};

export { Button as BcUiButton };
