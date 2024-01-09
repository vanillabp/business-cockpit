import { BcUiButtonFC } from '@vanillabp/bc-ui-types';
import { Button as MuiButton, Tooltip } from '@mui/material';

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
  const button = (
      <MuiButton
          disabled={ disabled }
          fullWidth={ fill }
          size={ size === undefined
              ? undefined
              : size === 'small'
              ? 'small'
              : size === 'medium'
              ? 'medium'
              : size === 'large'
              ? 'large'
              : undefined }
          startIcon={ children === undefined ? undefined : icon }
          onClick={ onClick }>
        {
          children === undefined ? icon : children
        }
      </MuiButton>);
  return tip === undefined
      ? button
      : <Tooltip title={ tip }>
        {
          button
        }
        </Tooltip>
};

export { Button as BcUiButton };
