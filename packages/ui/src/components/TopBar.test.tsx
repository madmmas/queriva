import { render, screen, waitFor } from '@testing-library/react';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { HEALTH_LABEL_QDRANT, STATUS_CHECKING } from '../constants/ui';
import { server } from '../setupTests';
import { TopBar } from './TopBar';

describe('TopBar', () => {
  it('should render health services from msw when health check completes', async () => {
    render(<TopBar theme="light" onThemeToggle={() => undefined} />);

    await waitFor(() => {
      expect(screen.getByText(HEALTH_LABEL_QDRANT)).toBeInTheDocument();
    });
  });

  it('should show checking status before health response arrives', async () => {
    server.use(
      http.get('/api/health', async () => {
        await delay(200);
        return HttpResponse.json({
          status: 'ok',
          qdrant: 'connected',
          ollama: 'connected',
          embed_sidecar: 'connected',
        });
      }),
    );

    render(<TopBar theme="light" onThemeToggle={() => undefined} />);

    expect(screen.getByText(STATUS_CHECKING)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByText(STATUS_CHECKING)).not.toBeInTheDocument();
    });
  });
});
