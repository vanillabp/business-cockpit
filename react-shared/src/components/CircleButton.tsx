import { Box, Button, ButtonType } from "grommet";
import React, { createRef, MouseEvent } from "react";

const CircleButton = ({
  style,
  className,
  icon,
  onClick,
  color = 'accent-3'
}: ButtonType) => {
  const buttonRef = createRef<HTMLButtonElement & HTMLAnchorElement>();
  return (
        <Box
            style={style}
            className={className}
            margin="medium"
            round="full"
            height="xxsmall"
            width="xxsmall"
            overflow="hidden"
            background={ color }
            align="center">
          <Button
              size='large'
              icon={icon}
              ref={buttonRef}
              color={ color }
              hoverIndicator
              style={ { border: 'none', height: '4rem' } }
              onClick={ (event: MouseEvent<HTMLButtonElement & HTMLAnchorElement>) => {
                buttonRef.current?.blur();
                if (onClick) onClick(event);
              }} />
        </Box>);
}

export { CircleButton };
