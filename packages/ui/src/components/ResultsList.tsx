import { IconListSearch } from '@tabler/icons-react';
import { TOP_RESULT_HIGHLIGHT_COUNT } from '../constants/display';
import { RESULTS_SECTION_LABEL } from '../constants/ui';
import type { SearchResult } from '../types/api';
import { EmptyState } from './EmptyState';
import { LoadMoreButton } from './LoadMoreButton';
import { ResultCard } from './ResultCard';
import { SearchSkeleton } from './SearchSkeleton';

interface ResultsListProps {
  query: string;
  results: SearchResult[];
  loading: boolean;
  remainingCount: number;
  onLoadMore: () => void;
  onResultClick?: (result: SearchResult) => void;
}

/**
 * Left-column ranked results with section label and load-more control (issue #21).
 */
export function ResultsList({
  query,
  results,
  loading,
  remainingCount,
  onLoadMore,
  onResultClick,
}: ResultsListProps) {
  return (
    <section className="qv-left" aria-label={RESULTS_SECTION_LABEL}>
      <div className="qv-sec-label">
        <IconListSearch size={13} aria-hidden="true" />
        {RESULTS_SECTION_LABEL}
      </div>

      {loading ? (
        <SearchSkeleton />
      ) : results.length === 0 ? (
        <EmptyState query={query} />
      ) : (
        <>
          {results.map((result, index) => (
            <ResultCard
              key={result.id}
              rank={index + 1}
              result={result}
              isTopResult={index < TOP_RESULT_HIGHLIGHT_COUNT}
              onClick={onResultClick}
            />
          ))}
          <LoadMoreButton remainingCount={remainingCount} onLoadMore={onLoadMore} />
        </>
      )}
    </section>
  );
}
