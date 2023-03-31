import React, { PropsWithChildren } from 'react';
import { TextProps } from 'grommet';
import styled from 'styled-components';

const LighterWeightText = ({
  children,
  ...props
}: PropsWithChildren<TextProps>) => (
  <Paragraph
      {...props}
      weight='lighter'>{ children }</Paragraph>
);

const Paragraph = styled(LighterWeightText)`
  font-style: italic;
`;

export default Paragraph;