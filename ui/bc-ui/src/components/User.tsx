import { Grid, Text } from 'grommet';
import { Person } from '@vanillabp/bc-official-gui-client';
import { UserAvatar } from './UserAvatar.js';
import { TranslationFunction, UserDetailsBox } from "@vanillabp/bc-shared";
import React from "react";

type UserProps = {
  t: TranslationFunction;
  user: Person;
  isUserLoggedIn?: boolean;
  size?: 'xsmall' | 'small' | 'medium' | 'large' | 'xlarge' | string;
  iconSize?: 'xsmall' | 'small' | 'medium' | 'large' | 'xlarge' | string;
};

const User = ({
  t,
  user,
  isUserLoggedIn = false,
  size = 'medium',
  iconSize,
}: UserProps) => (
  <Grid
      fill="horizontal"
      gap="small"
      columns={['auto', 'auto', 'flex']}
      align="center">
    {
      user.avatar === undefined
          ? undefined
          : <UserAvatar
                t={ t }
                user={ user }
                isUserLoggedIn={ isUserLoggedIn }
                size={ iconSize !== undefined ? iconSize : size } />
    }
    <Text
        size={ size }
        tip={ { content: <UserDetailsBox user={ user } t={ t } /> } }
        truncate>
      {
        user.displayShort ?? user.email ?? user.id
      }
    </Text>
  </Grid>
);

export { User };
