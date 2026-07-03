import { useCallback, useEffect, useState } from 'react';

/** Colour theme for the Queriva UI. */
export type Theme = 'light' | 'dark';

const THEME_STORAGE_KEY = 'queriva-theme';

function readStoredTheme(): Theme | null {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
  } catch {
    return null;
  }
  return null;
}

function writeStoredTheme(theme: Theme): void {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // Storage may be unavailable in test or private browsing contexts.
  }
}

/**
 * Resolves the initial theme from localStorage or system preference.
 */
function resolveInitialTheme(): Theme {
  const stored = readStoredTheme();
  if (stored !== null) {
    return stored;
  }

  if (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  ) {
    return 'dark';
  }

  return 'light';
}

/**
 * Manages light/dark theme via the document root data-theme attribute.
 */
export function useTheme(): { theme: Theme; toggleTheme: () => void; setTheme: (theme: Theme) => void } {
  const [theme, setThemeState] = useState<Theme>(() => resolveInitialTheme());

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    writeStoredTheme(theme);
  }, [theme]);

  const setTheme = useCallback((nextTheme: Theme) => {
    setThemeState(nextTheme);
  }, []);

  const toggleTheme = useCallback(() => {
    setThemeState((current) => (current === 'light' ? 'dark' : 'light'));
  }, []);

  return { theme, toggleTheme, setTheme };
}
