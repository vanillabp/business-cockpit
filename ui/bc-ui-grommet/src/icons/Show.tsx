import { BcUiIconFC } from "./index.js";
import { FormView } from "grommet-icons";

const Show: BcUiIconFC = ({ color, disabled }) => {
  const translatedColor = disabled ? 'light-4' : 'dark-3';
  return (
      <FormView
          color={ translatedColor } />);
}

export { Show as BcUiShow };
