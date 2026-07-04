import type { SearchResponse } from '../types/api';
import { demoSearchResponse } from './demoSearchResponse';

/** Demo RAG response for MSW handlers and component tests. */
export const demoRagSearchResponse: SearchResponse = {
  ...demoSearchResponse,
  mode: 'rag',
  summary:
    'Three flood events hit Dhaka between June 14–20 following 340mm of rain in 48 hours. [3] ' +
    'The Buriganga overflowed on June 15, submerging Old Dhaka and Demra. [1] ' +
    'Over 200,000 residents were displaced, prompting a Red Crescent response. [2] ' +
    "WASA's 14 pumping stations were overwhelmed [3] and army boats deployed after road links were severed. [4]",
  latency_ms: {
    embed: 43,
    search: 21,
    synthesis: 1800,
    total: 1908,
  },
};
