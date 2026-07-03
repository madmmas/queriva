import { IconSearch } from '@tabler/icons-react';
import type { FormEvent } from 'react';
import { SEARCH_INPUT_LABEL, SEARCH_PLACEHOLDER, SEARCH_SUBMIT_LABEL } from '../constants/ui';
import type { SearchMode } from '../types/api';
import { ModeToggle } from './ModeToggle';

interface SearchBarProps {
  query: string;
  mode: SearchMode;
  onQueryChange: (query: string) => void;
  onModeChange: (mode: SearchMode) => void;
  onSubmit: () => void;
}

/**
 * Search input with mode toggle; submits on Enter or button click (issue #20).
 */
export function SearchBar({
  query,
  mode,
  onQueryChange,
  onModeChange,
  onSubmit,
}: SearchBarProps) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSubmit();
  };

  return (
    <form className="qv-search-box" onSubmit={handleSubmit}>
      <IconSearch size={17} aria-hidden="true" className="qv-search-icon" />
      <label htmlFor="qv-search-input" className="sr-only">
        {SEARCH_INPUT_LABEL}
      </label>
      <input
        id="qv-search-input"
        type="search"
        value={query}
        placeholder={SEARCH_PLACEHOLDER}
        onChange={(event) => onQueryChange(event.target.value)}
      />
      <button type="submit" className="sr-only">
        {SEARCH_SUBMIT_LABEL}
      </button>
      <ModeToggle mode={mode} onModeChange={onModeChange} />
    </form>
  );
}
