import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';
import { DEV_SERVER_PORT, REMOTE_PREVIEW_PORT } from './src/constants/search';

/**
 * Standalone SPA (port 3000) + Module Federation remote (preview port 5173).
 * SPEC §11 / ADR-010 — exposes ./SearchWidget as queriva remote.
 */
export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'queriva',
      filename: 'remoteEntry.js',
      exposes: {
        './SearchWidget': './src/SearchWidget.tsx',
      },
      shared: ['react', 'react-dom'],
    }),
  ],
  server: {
    port: DEV_SERVER_PORT,
    cors: true,
  },
  preview: {
    port: REMOTE_PREVIEW_PORT,
    strictPort: true,
    cors: true,
  },
  build: {
    target: 'esnext',
    minify: false,
    cssCodeSplit: false,
    modulePreload: false,
  },
});
