import { Text, TextProps } from 'grommet';
import React, { PropsWithChildren } from 'react';
import styled from 'styled-components';
import { normalizeColor } from "grommet/utils/index.js";

interface LinkProps extends PropsWithChildren<TextProps> {
  href?: string;
  target?: string;
};

const StyledLink = styled(Text)<LinkProps>`
  text-decoration: none;
  color: ${ props => props.color
          ? normalizeColor(props.color, props.theme)
          : normalizeColor(props.theme.global.colors['link'], props.theme) };
  cursor: pointer;

  &:hover {
    text-decoration: underline;
    color: ${ props => props.color
            ? normalizeColor(props.color, props.theme)
            : normalizeColor(props.theme.global.colors['link'], props.theme) };
  }

  &:visited {
    color: ${ props => props.color
            ? normalizeColor(props.color, props.theme)
            : normalizeColor(props.theme.global.colors['link'], props.theme) };
  }
`;

const Link = ({ ...props }: LinkProps) => <StyledLink
    as="a"
    { ...props } />

export { Link };

