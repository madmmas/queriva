import {
  IconChartBar,
  IconClock,
  IconFileText,
  IconLanguage,
} from '@tabler/icons-react';
import {
  STAT_BEST_SCORE_LABEL,
  STAT_HITS_SUFFIX,
  STAT_LANG_SUFFIX,
  STAT_LANGUAGES_LABEL,
  STAT_RESULTS_LABEL,
  STAT_SECONDS_SUFFIX,
  STAT_TOP_SUFFIX,
  STAT_TOTAL_TIME_LABEL,
} from '../constants/ui';
import { formatLatencySeconds, formatScore } from '../utils/formatters';
import type { SearchStats } from '../utils/searchStats';

interface StatsPanelProps {
  stats: SearchStats;
}

/**
 * Right-column metric cards for hits, score, languages, and latency (issue #21).
 */
export function StatsPanel({ stats }: StatsPanelProps) {
  const totalTimeLabel =
    stats.totalTimeMs === null ? '—' : formatLatencySeconds(stats.totalTimeMs);
  const bestScoreLabel = stats.bestScore === null ? '—' : formatScore(stats.bestScore);

  return (
    <aside className="qv-right" aria-label="Search statistics">
      <div className="qv-stats">
        <div className="qv-stat">
          <div className="qv-stat-val">
            {stats.hitCount} <span>{STAT_HITS_SUFFIX}</span>
          </div>
          <div className="qv-stat-lbl">
            <IconFileText size={11} aria-hidden="true" />
            {STAT_RESULTS_LABEL}
          </div>
        </div>
        <div className="qv-stat">
          <div className="qv-stat-val">
            {bestScoreLabel} <span>{STAT_TOP_SUFFIX}</span>
          </div>
          <div className="qv-stat-lbl">
            <IconChartBar size={11} aria-hidden="true" />
            {STAT_BEST_SCORE_LABEL}
          </div>
        </div>
        <div className="qv-stat">
          <div className="qv-stat-val">
            {stats.languageCount} <span>{STAT_LANG_SUFFIX}</span>
          </div>
          <div className="qv-stat-lbl">
            <IconLanguage size={11} aria-hidden="true" />
            {STAT_LANGUAGES_LABEL}
          </div>
        </div>
        <div className="qv-stat">
          <div className="qv-stat-val">
            {totalTimeLabel} <span>{STAT_SECONDS_SUFFIX}</span>
          </div>
          <div className="qv-stat-lbl">
            <IconClock size={11} aria-hidden="true" />
            {STAT_TOTAL_TIME_LABEL}
          </div>
        </div>
      </div>
    </aside>
  );
}
