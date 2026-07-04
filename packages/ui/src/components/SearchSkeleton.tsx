import { SEARCH_SKELETON_LABEL } from '../constants/ui';

/**
 * Loading placeholder for search results (issue #21).
 */
export function SearchSkeleton() {
  return (
    <div className="qv-search-skeleton" aria-busy="true" aria-label={SEARCH_SKELETON_LABEL}>
      <div className="qv-skeleton-card" />
      <div className="qv-skeleton-card" />
      <div className="qv-skeleton-card" />
    </div>
  );
}
