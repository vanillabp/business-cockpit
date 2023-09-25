import type { Options } from 'tsup';

// @ts-ignore
const env = process.env.NODE_ENV;

export const tsup: Options = {
  splitting: true,
  clean: false,
  dts: true,
  format: ['esm'],
  minify: false,
  bundle: false,
  skipNodeModulesBundle: true,
  entryPoints: ['src/index.ts'],
  target: 'es6',
  outDir: 'dist',
  entry: ['src/**/*.ts*'],
};
