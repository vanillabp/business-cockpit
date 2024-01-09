import { FC } from "react";
import { Color } from "./index.js";

interface IconProperties {
  color?: Color;
}

declare const BcUiIcon: FC<IconProperties>;
export type BcUiIconFC = typeof BcUiIcon;

declare const BcUiShow: BcUiIconFC;
export type BcUiShowFC = typeof BcUiShow;
declare const BcUiHide: BcUiIconFC;
export type BcUiHideFC = typeof BcUiHide;

export {
  BcUiShow,
  BcUiHide,
}
