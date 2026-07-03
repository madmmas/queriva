import { IconFileText } from '@tabler/icons-react';
import { RESULT_COUNT_SUFFIX } from '../constants/ui';

interface ResultCountProps {
  count: number;
}

/**
 * Displays the number of search results (issue #20).
 */
export function ResultCount({ count }: ResultCountProps) {
  const label = `${count} ${RESULT_COUNT_SUFFIX}`;

  return (
    <div className="qv-count" aria-label={label}>
      <IconFileText size={13} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
