import { Button } from 'grommet';
import styled from "styled-components";

const CodeButton = styled(Button)`
  position: relative;
  white-space: nowrap;
  &:after {
    content: '';
    width: 110%;
    height: 2px;
    background: white;
    position: absolute;
    bottom: calc(${(props) => props.theme.button.secondary.border.width} * -1 - 2px);
    left: -5%;
  }
`;

export { CodeButton };
