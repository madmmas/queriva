import { IconMoon, IconSun } from '@tabler/icons-react';
import {
  APP_NAME,
  SCAFFOLD_STATUS_MESSAGE,
  THEME_DARK_LABEL,
  THEME_LIGHT_LABEL,
  THEME_TOGGLE_LABEL,
} from './constants/ui';
import { useTheme } from './hooks/useTheme';
import './App.css';

/**
 * Root application component — design scaffold for issues #20–#22 (issue #19).
 */
function App() {
  const { theme, toggleTheme } = useTheme();
  const themeLabel = theme === 'light' ? THEME_LIGHT_LABEL : THEME_DARK_LABEL;

  return (
    <div className="qv-app">
      <header className="qv-bar">
        <div className="qv-logo">
          <div className="qv-icon-wrap">
            <img src="/queriva-icon-transparent.svg" alt="" aria-hidden="true" />
          </div>
          <span className="qv-brand">{APP_NAME}</span>
        </div>
        <div className="qv-bar-right">
          <button
            type="button"
            className="qv-theme-btn"
            onClick={toggleTheme}
            aria-label={THEME_TOGGLE_LABEL}
            title={themeLabel}
          >
            {theme === 'light' ? (
              <IconMoon size={18} aria-hidden="true" />
            ) : (
              <IconSun size={18} aria-hidden="true" />
            )}
          </button>
        </div>
      </header>

      <main className="qv-main">
        <div className="qv-scaffold-card">
          <h1>{APP_NAME}</h1>
          <p>{SCAFFOLD_STATUS_MESSAGE}</p>
          <div className="qv-token-swatch" aria-label="Brand colour tokens">
            <div className="qv-swatch qv-swatch-navy" title="--qv-navy" />
            <div className="qv-swatch qv-swatch-teal" title="--qv-teal" />
            <div className="qv-swatch qv-swatch-amber" title="--qv-amber" />
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
