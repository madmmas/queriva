import type { SearchResponse } from '../types/api';

/** Demo search response for MSW handlers and component tests. */
export const demoSearchResponse: SearchResponse = {
  query: 'floods in Dhaka last week',
  mode: 'search',
  summary: null,
  results: [
    {
      id: 'cluster-001',
      score: 0.92,
      title: 'Buriganga river overflows, floods low-lying Dhaka areas',
      snippet:
        'Heavy monsoon rains caused the Buriganga river to overflow its banks on June 15, submerging large parts of Old Dhaka and Demra.',
      source: 'prothomalo.com',
      language: 'bn',
      published_at: '2026-06-15T08:30:00Z',
      url: 'https://www.prothomalo.com/bangladesh/article-cluster-001',
    },
    {
      id: 'cluster-002',
      score: 0.87,
      title: '200,000 residents displaced by Dhaka flooding, BDRCS responds',
      snippet:
        'The Bangladesh Red Crescent Society deployed emergency teams across six flood-affected upazilas.',
      source: 'thedailystar.net',
      language: 'en',
      published_at: '2026-06-17T11:15:00Z',
      url: 'https://www.thedailystar.net/news/bangladesh/disaster/cluster-002',
    },
  ],
  latency_ms: {
    embed: 45,
    search: 23,
    total: 1908,
  },
};
