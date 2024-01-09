import { FC, PropsWithChildren, ReactNode } from "react";

type BorderType = 'left' | 'right' | 'top' | 'bottom';
interface ButtonProperties {
  disabled?: boolean;
  icon?: ReactNode;
  fill?: boolean;
  size?: 'small' | 'medium' | 'large' | string;
  tip?: string;
  onClick?: (...args: any[]) => any;
  group?: 'first' | 'middle' | 'last';
}

declare const BcUiButton: FC<PropsWithChildren<ButtonProperties>>;

export type BcUiButtonFC = typeof BcUiButton;

export {
  BcUiButton
}
