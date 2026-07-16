import { describe, expect, it } from 'vitest';
import {
  SearchWidget,
  type SearchMode,
  type SearchResult,
  type SearchWidgetProps,
} from './mfeExports';

describe('mfeExports', () => {
  it('should export SearchWidget component for host consumption', () => {
    expect(typeof SearchWidget).toBe('function');
  });

  it('should export SearchWidgetProps, SearchResult, and SearchMode types', () => {
    const mode: SearchMode = 'rag';
    const result: SearchResult = {
      id: 'cluster-001',
      score: 0.91,
      title: 'Buriganga river overflows, floods low-lying Dhaka areas',
      snippet: 'Heavy monsoon rains caused the Buriganga river to overflow...',
      source: 'prothomalo.com',
      language: 'bn',
      published_at: '2026-06-15T08:30:00Z',
      url: 'https://example.com/cluster-001',
    };
    const props: SearchWidgetProps = {
      apiUrl: 'http://localhost:8080',
      collection: 'news_radar',
      defaultMode: mode,
      theme: 'light',
      filters: { language: 'bn', category: 'weather' },
      onResultClick: (clicked) => {
        expect(clicked.id).toBe(result.id);
      },
    };

    expect(props.collection).toBe('news_radar');
    props.onResultClick?.(result);
  });
});
