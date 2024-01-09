import { BcUiButtonGroupFC } from '@vanillabp/bc-ui-types';
import { Box } from "grommet";
import { Children, cloneElement, ReactElement } from "react";

const ButtonGroup: BcUiButtonGroupFC = ({
  disabled,
  fill,
  size,
  color,
  children
}) => {
  const translatedColor = disabled ? 'light-4' : 'dark-3';
  //const color = disabled ? 'light-4' : 'dark-3';
  const arrayChildren = Children.toArray(children);
  return (
      <Box
          direction="row"
          round={ { size: '0.4rem' } }
          border={ { color: translatedColor } }
          elevation="small">
        {
          Children.map(arrayChildren, (child, index) => {
            const group = index === 0
                ? "first"
                : index === arrayChildren.length - 1
                    ? "last"
                    : "middle";
            return cloneElement(child as ReactElement, { group, disabled });
          })
        }
      </Box>);
};

export { ButtonGroup as BcUiButtonGroup };
