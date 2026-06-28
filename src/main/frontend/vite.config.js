import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  build: {
    outDir: resolve(__dirname, '../resources/static'),
    emptyOutDir: false,
    lib: {
      entry: resolve(__dirname, 'js/main.js'),
      name: 'RestUnitTest',
      fileName: (format) => `scripts.js`,
      formats: ['iife']
    },
    rollupOptions: {
      output: {
        extend: true,
      }
    }
  }
});
