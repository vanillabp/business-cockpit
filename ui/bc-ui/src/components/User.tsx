import { Grid, Text } from 'grommet';
import { Person } from '@vanillabp/bc-official-gui-client';
import { UserAvatar } from './UserAvatar.js';
import { TranslationFunction } from "@vanillabp/bc-shared";
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
    <UserAvatar
        user={ user }
        isUserLoggedIn={ isUserLoggedIn }
        size={ iconSize !== undefined ? iconSize : size }/>
    <Text
        size={ size }
        tip={ { content: <Grid columns={['auto', 'auto']} gap="xsmall">
            <Text>{ t('person-id') }:</Text>
            <Text weight="bold">{ user.id }</Text>
            {
              user.email !== null
                  ? <>
                    <Text>{ t('person-email') }:</Text>
                    <Text weight="bold">{ user.email }</Text>
                  </>
                  : undefined
            }
          </Grid> } }
        truncate>
    {
      user.display ?? user.email ?? user.id
    }
    </Text>
  </Grid>
);

export { User };
