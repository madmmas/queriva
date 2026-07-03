import { useEffect, useState } from 'react';
import type { HealthResponse } from '../types/api';

const DEFAULT_API_BASE_URL = '';

interface UseHealthState {
  health: HealthResponse | null;
  loading: boolean;
  error: string | null;
}

/**
 * Fetches GET /api/health for dependency status pills (issue #20).
 */
export function useHealth(apiBaseUrl: string = DEFAULT_API_BASE_URL): UseHealthState {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function fetchHealth(): Promise<void> {
      setLoading(true);
      setError(null);

      try {
        const response = await fetch(`${apiBaseUrl}/api/health`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`Health check failed with status ${response.status}`);
        }

        const body = (await response.json()) as HealthResponse;
        setHealth(body);
      } catch (fetchError) {
        if (fetchError instanceof Error && fetchError.name === 'AbortError') {
          return;
        }
        setHealth(null);
        setError(fetchError instanceof Error ? fetchError.message : 'Health check failed');
      } finally {
        setLoading(false);
      }
    }

    void fetchHealth();

    return () => {
      controller.abort();
    };
  }, [apiBaseUrl]);

  return { health, loading, error };
}
