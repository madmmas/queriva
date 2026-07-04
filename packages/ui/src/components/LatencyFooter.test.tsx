import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { demoRagSearchResponse } from '../fixtures/demoRagSearchResponse';
import { LatencyFooter } from './LatencyFooter';

describe('LatencyFooter', () => {
  it('should show embed search and llm latency breakdown', () => {
    render(<LatencyFooter latencyMs={demoRagSearchResponse.latency_ms} />);

    expect(screen.getByText(/embed/i)).toBeInTheDocument();
    expect(screen.getByText(/search/i)).toBeInTheDocument();
    expect(screen.getByText(/llm/i)).toBeInTheDocument();
    expect(screen.getByText('43ms')).toBeInTheDocument();
    expect(screen.getByText('21ms')).toBeInTheDocument();
    expect(screen.getByText('1.8s')).toBeInTheDocument();
  });
});
