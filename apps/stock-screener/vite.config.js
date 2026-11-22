import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const appRoot = fileURLToPath(new URL('.', import.meta.url));
const repositoryRoot = resolve(appRoot, '../..');

export default defineConfig(({ command }) => ({
  plugins: [react()],
  base: process.env.VITE_BASE_PATH || (command === 'build'
    ? '/daily_custom_stock_screener/stock-screeners/'
    : '/'),
  server: {
    fs: {
      allow: [appRoot, repositoryRoot],
    },
  },
  build: {
    outDir: 'build',
    copyPublicDir: false,
  },
}));
