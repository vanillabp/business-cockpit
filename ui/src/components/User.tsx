import { Grid, Text } from 'grommet';
import { User as UserDto } from '@vanillabp/bc-official-gui-client';
import { UserAvatar } from './UserAvatar.js';

type CurrentUserProps = {
  user: UserDto;
  isUserLoggedIn?: boolean;
  size?: 'xsmall' | 'small' | 'medium' | 'large' | 'xlarge' | string;
  iconSize?: 'xsmall' | 'small' | 'medium' | 'large' | 'xlarge' | string;
};

const User = ({
  user,
  isUserLoggedIn = false,
  size = 'medium',
  iconSize,
}: CurrentUserProps) => (
  <Grid
      fill="horizontal"
      gap="small"
      columns={['auto', 'auto', 'flex']}
      align="center">
    <UserAvatar
        user={ user }
        isUserLoggedIn={ isUserLoggedIn }
        size={ iconSize !== undefined ? iconSize : size }/>
    <Text
        size={ size }
        truncate="tip">
    {
      !Boolean(user.lastName)
          ? !Boolean(user.email)
            ? user.id
            : user.email
          : Boolean(user.firstName)
          ? `${user.firstName} ${user.lastName}`
          : user.lastName
    }
    </Text>
  </Grid>
);

export { User };
