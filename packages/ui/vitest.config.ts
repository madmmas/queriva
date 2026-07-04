/// <reference types="vitest/config" />
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    env: {
      VITE_API_URL: '',
      VITE_DEFAULT_COLLECTION: 'news_radar',
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.d.ts',
        'src/main.tsx',
        'src/**/*.test.{ts,tsx}',
        'src/setupTests.ts',
        'src/__tests__/*',
        'src/SearchWidget.tsx',
      ],
      thresholds: {
        lines: 80,
      },
    },
  },
});
