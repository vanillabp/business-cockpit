export type Color = 'primary' | 'secondary';

const translateColor = (color?: Color): string | undefined => {
  return color === 'primary'
      ? 'brand'
      : color === 'secondary'
      ? 'accent-2'
      : undefined;
};

export { translateColor };
