import React from 'react';
import { Text, Grid } from 'grommet';
import { User as UserDto } from '../../client/gui';
import { UserAvatar } from '../../components/UserAvatar';

type CurrentUserProps = {
  user: UserDto;
};

const User = ({
  user,
}: CurrentUserProps) => {
  return <Grid fill="horizontal"
          gap="small" columns={['xxsmall', 'auto', 'flex']} align="center">
          <UserAvatar user={ user } />
          <Text truncate="tip">{user.email}</Text>
          <Text>{ user.memberId !== undefined ? `(${user.memberId})` : '' }</Text>
        </Grid>;
}

export default User;
