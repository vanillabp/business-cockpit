import { FC, PropsWithChildren } from "react";
import { Color } from "./index.js";

interface ButtonGroupProperties {
  disabled?: boolean;
  fill?: boolean;
  color?: Color;
  size?: 'small' | 'medium' | 'large' | string;
}

declare const BcUiButtonGroup: FC<PropsWithChildren<ButtonGroupProperties>>;

export type BcUiButtonGroupFC = typeof BcUiButtonGroup;

export {
  BcUiButtonGroup
}
