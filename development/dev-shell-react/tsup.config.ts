import type { Options } from 'tsup';

// @ts-ignore
const env = process.env.NODE_ENV;

export const tsup: Options = {
  splitting: true,
  clean: true,
  dts: true,
  format: ['esm'],
  minify: env === 'production',
  bundle: env === 'production',
  skipNodeModulesBundle: true,
  entryPoints: ['src/index.ts'],
  target: 'es6',
  outDir: 'dist',
  entry: ['src/**/*.ts*'],
};
