import { Grid, Text } from "grommet";
import React from "react";
import { Person as ListCellPerson, TranslationFunction } from '../types/index.js';
import { Person } from "@vanillabp/bc-official-gui-client";

const UserDetailsBox = ({
  t,
  user
}: {
  t: TranslationFunction;
  user: Person | ListCellPerson;
}) => {

  return (
      <Grid
          columns={['auto', 'auto']}
          rows={['auto', 'auto', 'auto']}
          areas={ [ { name: 'person', start: [0, 0], end: [1, 0] } ] }
          gap="xsmall">
        <Text
            gridArea="person"
            style={ { fontStyle: "italic" } }>{ user.display }</Text>
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
      </Grid>);

};

export { UserDetailsBox };
