import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

const HOST_DEV_PORT = 5174;
const DEFAULT_REMOTE_ENTRY = 'http://localhost:5173/assets/remoteEntry.js';

/**
 * Example host that consumes Queriva SearchWidget via Module Federation (SPEC §11).
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const remoteEntry = env.VITE_QUERIVA_REMOTE_ENTRY || DEFAULT_REMOTE_ENTRY;

  return {
    plugins: [
      react(),
      federation({
        name: 'queriva_host',
        remotes: {
          queriva: remoteEntry,
        },
        shared: ['react', 'react-dom'],
      }),
    ],
    server: {
      port: HOST_DEV_PORT,
      strictPort: true,
    },
    preview: {
      port: HOST_DEV_PORT,
      strictPort: true,
    },
    build: {
      target: 'esnext',
      minify: false,
      modulePreload: false,
    },
  };
});
