import type { Options } from 'tsup';

// @ts-ignore
const env = process.env.NODE_ENV;

export const tsup: Options = {
  splitting: true,
  clean: true,
  dts: {
    compilerOptions: {
      isolatedModules: false,
    }
  },
  format: ['esm'],
  outExtension: ({ format }) => ({
    js: `.${format === 'esm' ? 'js' : format}`,
  }),
  minify: env === 'production',
  bundle: env === 'production',
  skipNodeModulesBundle: true,
  entryPoints: ['src/index.ts'],
  target: 'es6',
  outDir: 'dist',
  entry: ['src/**/*.ts*'],
};
