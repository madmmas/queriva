/**
 * SPEC §11 consumption sample — must typecheck in the host example (issue #26).
 *
 * Mirrors the Module Federation guide in docs/SPEC.md §11.
 */
import SearchWidget from 'queriva/SearchWidget';
import type { SearchWidgetProps } from 'queriva/SearchWidget';

/**
 * Compiles the SPEC §11 host integration snippet against exported widget types.
 */
export function SpecConsumptionSample() {
  const props: SearchWidgetProps = {
    apiUrl: 'http://localhost:8080',
    collection: 'news_radar',
    defaultMode: 'rag',
    filters: { language: 'bn' },
  };

  return <SearchWidget {...props} />;
}
