import {
  IconCalendar,
  IconCloudRain,
  IconFilter,
  IconLanguage,
  IconTag,
} from '@tabler/icons-react';
import {
  CATEGORY_FILTER_NATIONAL,
  CATEGORY_FILTER_WEATHER,
  DATE_FILTER_LAST_7_DAYS,
  FILTER_STRIP_LABEL,
  LANGUAGE_FILTER_BANGLA,
  LANGUAGE_FILTER_ENGLISH,
} from '../constants/ui';
import {
  DATE_FILTER_RANGE_DAYS,
  FILTER_CATEGORY_NATIONAL,
  FILTER_CATEGORY_WEATHER,
  FILTER_LANGUAGE_BANGLA,
  FILTER_LANGUAGE_ENGLISH,
} from '../constants/filters';
import type { ActiveFilters } from '../utils/filterState';
import { formatIsoDateDaysAgo } from '../utils/filterState';
import { ResultCount } from './ResultCount';

interface FilterStripProps {
  collection: string;
  filters: ActiveFilters;
  resultCount: number;
  onFiltersChange: (filters: ActiveFilters) => void;
}

/**
 * Filter chips for language, date range, category, and collection badge (issue #20).
 */
export function FilterStrip({
  collection,
  filters,
  resultCount,
  onFiltersChange,
}: FilterStripProps) {
  const toggleLanguage = (languageCode: string) => {
    onFiltersChange({
      ...filters,
      language: filters.language === languageCode ? null : languageCode,
    });
  };

  const toggleLastSevenDays = () => {
    if (filters.dateFrom && filters.dateTo) {
      onFiltersChange({ ...filters, dateFrom: null, dateTo: null });
      return;
    }

    const today = formatIsoDateDaysAgo(0);
    const weekAgo = formatIsoDateDaysAgo(DATE_FILTER_RANGE_DAYS);
    onFiltersChange({ ...filters, dateFrom: weekAgo, dateTo: today });
  };

  const toggleCategory = (category: string) => {
    onFiltersChange({
      ...filters,
      category: filters.category === category ? null : category,
    });
  };

  return (
    <div className="qv-filter-strip" role="group" aria-label={FILTER_STRIP_LABEL}>
      <button
        type="button"
        className={`qv-chip${filters.language === FILTER_LANGUAGE_BANGLA ? ' on' : ''}`}
        aria-pressed={filters.language === FILTER_LANGUAGE_BANGLA}
        onClick={() => toggleLanguage(FILTER_LANGUAGE_BANGLA)}
      >
        <IconLanguage size={12} aria-hidden="true" />
        {LANGUAGE_FILTER_BANGLA}
      </button>
      <button
        type="button"
        className={`qv-chip${filters.language === FILTER_LANGUAGE_ENGLISH ? ' on' : ''}`}
        aria-pressed={filters.language === FILTER_LANGUAGE_ENGLISH}
        onClick={() => toggleLanguage(FILTER_LANGUAGE_ENGLISH)}
      >
        <IconLanguage size={12} aria-hidden="true" />
        {LANGUAGE_FILTER_ENGLISH}
      </button>
      <button
        type="button"
        className={`qv-chip${filters.dateFrom && filters.dateTo ? ' on' : ''}`}
        aria-pressed={Boolean(filters.dateFrom && filters.dateTo)}
        onClick={toggleLastSevenDays}
      >
        <IconCalendar size={12} aria-hidden="true" />
        {DATE_FILTER_LAST_7_DAYS}
      </button>
      <button
        type="button"
        className={`qv-chip${filters.category === FILTER_CATEGORY_NATIONAL ? ' on' : ''}`}
        aria-pressed={filters.category === FILTER_CATEGORY_NATIONAL}
        onClick={() => toggleCategory(FILTER_CATEGORY_NATIONAL)}
      >
        <IconTag size={12} aria-hidden="true" />
        {CATEGORY_FILTER_NATIONAL}
      </button>
      <button
        type="button"
        className={`qv-chip${filters.category === FILTER_CATEGORY_WEATHER ? ' on' : ''}`}
        aria-pressed={filters.category === FILTER_CATEGORY_WEATHER}
        onClick={() => toggleCategory(FILTER_CATEGORY_WEATHER)}
      >
        <IconCloudRain size={12} aria-hidden="true" />
        {CATEGORY_FILTER_WEATHER}
      </button>
      <div className="qv-chip amber" aria-label={`Collection ${collection}`}>
        <IconFilter size={12} aria-hidden="true" />
        {collection}
      </div>
      <ResultCount count={resultCount} />
    </div>
  );
}
