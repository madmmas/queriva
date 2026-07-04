import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { DEV_SERVER_PORT } from './src/constants/search';

export default defineConfig({
  plugins: [react()],
  server: {
    port: DEV_SERVER_PORT,
  },
});
