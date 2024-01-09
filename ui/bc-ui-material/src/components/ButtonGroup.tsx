import { BcUiButtonGroupFC } from '@vanillabp/bc-ui-types';
import { ButtonGroup as MuiButtonGroup } from '@mui/material';

const ButtonGroup: BcUiButtonGroupFC = ({
  disabled,
  fill,
  size,
  color,
  children
}) => {
  return (
      <MuiButtonGroup
          fullWidth={ fill }
          color={ color }
          size={ size === undefined
              ? undefined
              : size === 'small'
              ? 'small'
              : size === 'medium'
              ? 'medium'
              : size === 'large'
              ? 'large'
              : undefined }>
        {
          children
        }
      </MuiButtonGroup>);
};

export { ButtonGroup as BcUiButtonGroup };
