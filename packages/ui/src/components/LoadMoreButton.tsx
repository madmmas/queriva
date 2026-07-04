import { IconChevronDown } from '@tabler/icons-react';
import { LOAD_MORE_LABEL } from '../constants/ui';
import { formatLoadMoreLabel } from '../utils/formatters';

interface LoadMoreButtonProps {
  remainingCount: number;
  onLoadMore: () => void;
}

/**
 * Loads additional results by incrementing top_k (issue #21).
 */
export function LoadMoreButton({ remainingCount, onLoadMore }: LoadMoreButtonProps) {
  if (remainingCount <= 0) {
    return null;
  }

  const label = formatLoadMoreLabel(remainingCount);

  return (
    <button type="button" className="qv-more-btn" onClick={onLoadMore} aria-label={LOAD_MORE_LABEL}>
      <IconChevronDown size={14} aria-hidden="true" />
      {label}
    </button>
  );
}
