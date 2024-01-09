import { FC } from "react";
import { Color } from "../index.js";

interface IconProperties {
  color?: Color;
  disabled?: boolean;
}

declare const BcUiIcon: FC<IconProperties>;
export type BcUiIconFC = typeof BcUiIcon;

export * from './Show.js';
export * from './Hide.js';
