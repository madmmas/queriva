import { IconListSearch } from '@tabler/icons-react';
import { EMPTY_STATE_MESSAGE, NO_RESULTS_MESSAGE } from '../constants/ui';

interface EmptyStateProps {
  query: string;
}

/**
 * Shown when there are no search results to display (issue #21).
 */
export function EmptyState({ query }: EmptyStateProps) {
  const message = query.trim().length > 0 ? NO_RESULTS_MESSAGE : EMPTY_STATE_MESSAGE;

  return (
    <div className="qv-empty-state" role="status">
      <IconListSearch size={20} aria-hidden="true" />
      <p>{message}</p>
    </div>
  );
}
