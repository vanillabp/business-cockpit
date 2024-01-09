import { BcUiIconFC } from "./index.js";
import { Hide as HideIcon } from "grommet-icons";

const Hide: BcUiIconFC = ({ color, disabled }) => {
  const translatedColor = disabled ? 'light-4' : 'dark-3';
  return (
      <HideIcon
          color={ translatedColor } />);
}

export { Hide as BcUiHide };
