import { Grid, Text } from 'grommet';
import { User as UserDto } from '../../client/gui';
import { UserAvatar } from '../../components/UserAvatar';

type CurrentUserProps = {
  user: UserDto;
};

const User = ({
  user,
}: CurrentUserProps) => (
  <Grid fill="horizontal"
    gap="small" columns={['xxsmall', 'auto', 'flex']} align="center">
    <UserAvatar user={ user } />
    <Text truncate="tip">
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

export default User;
