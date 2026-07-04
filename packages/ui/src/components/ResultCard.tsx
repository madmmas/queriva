import type { KeyboardEvent } from 'react';
import { IconCalendar, IconWorld } from '@tabler/icons-react';
import { FILTER_LANGUAGE_BANGLA } from '../constants/filters';
import type { SearchResult } from '../types/api';
import {
  formatLanguageBadge,
  formatPublishedDate,
  formatScore,
  scoreBarWidthPercent,
} from '../utils/formatters';

interface ResultCardProps {
  rank: number;
  result: SearchResult;
  isTopResult: boolean;
  onClick?: (result: SearchResult) => void;
}

/**
 * One ranked search hit with score bar and metadata (issue #21).
 */
export function ResultCard({ rank, result, isTopResult, onClick }: ResultCardProps) {
  const isBangla = result.language === FILTER_LANGUAGE_BANGLA;
  const languageBadgeClass = isBangla ? 'qv-lang-bn' : 'qv-lang-en';

  const handleClick = () => {
    if (onClick) {
      onClick(result);
    }
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (!onClick) {
      return;
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onClick(result);
    }
  };

  return (
    <article
      className={`qv-card${isTopResult ? ' top' : ''}${onClick ? ' qv-card-clickable' : ''}`}
      onClick={onClick ? handleClick : undefined}
      onKeyDown={onClick ? handleKeyDown : undefined}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className="qv-card-head">
        <div className="qv-rank">{rank}</div>
        <h3 className="qv-card-title">{result.title}</h3>
        <div className="qv-score-col">
          <span className="qv-score-num">{formatScore(result.score)}</span>
          <div className="qv-score-bar">
            <div
              className="qv-score-fill"
              style={{ width: scoreBarWidthPercent(result.score) }}
            />
          </div>
        </div>
      </div>
      <p className="qv-snippet">{result.snippet}</p>
      <div className="qv-meta">
        <span className="qv-src">
          <IconWorld size={12} aria-hidden="true" />
          {result.source}
        </span>
        <span className={`qv-lang-badge ${languageBadgeClass}`}>
          {formatLanguageBadge(result.language)}
        </span>
        <span className="qv-date">
          <IconCalendar size={12} aria-hidden="true" />
          {formatPublishedDate(result.published_at)}
        </span>
      </div>
    </article>
  );
}
