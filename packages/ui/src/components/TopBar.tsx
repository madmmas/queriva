import {
  IconCpu,
  IconDatabase,
  IconMenu2,
  IconMoon,
  IconSettings,
  IconSun,
  IconVector,
} from '@tabler/icons-react';
import {
  APP_NAME,
  HEALTH_LABEL_EMBED,
  HEALTH_LABEL_OLLAMA,
  HEALTH_LABEL_QDRANT,
  MENU_LABEL,
  SETTINGS_LABEL,
  STATUS_CHECKING,
  THEME_DARK_LABEL,
  THEME_LIGHT_LABEL,
  THEME_TOGGLE_LABEL,
} from '../constants/ui';
import { useHealth } from '../hooks/useHealth';
import type { Theme } from '../hooks/useTheme';

interface TopBarProps {
  apiBaseUrl?: string;
  theme: Theme;
  onThemeToggle: () => void;
}

/**
 * Header bar with logo, health status pill, and navigation icons (issue #20).
 */
export function TopBar({ apiBaseUrl = '', theme, onThemeToggle }: TopBarProps) {
  const { health, loading } = useHealth(apiBaseUrl);
  const themeLabel = theme === 'light' ? THEME_LIGHT_LABEL : THEME_DARK_LABEL;
  const allConnected =
    health?.qdrant === 'connected' &&
    health?.ollama === 'connected' &&
    health?.embed_sidecar === 'connected';

  return (
    <header className="qv-bar">
      <div className="qv-logo">
        <div className="qv-icon-wrap">
          <img src="/queriva-icon-transparent.svg" alt="" aria-hidden="true" />
        </div>
        <span className="qv-brand">{APP_NAME}</span>
      </div>
      <div className="qv-bar-right">
        <div
          className="qv-status-pill"
          aria-live="polite"
          aria-busy={loading}
        >
          <div
            className={`qv-status-dot${allConnected ? '' : ' qv-status-dot-degraded'}`}
            aria-hidden="true"
          />
          {loading ? (
            <span>{STATUS_CHECKING}</span>
          ) : (
            <>
              <span className="qv-status-service">
                <IconDatabase size={13} aria-hidden="true" />
                {HEALTH_LABEL_QDRANT}
              </span>
              <span className="qv-status-service">
                <IconCpu size={13} aria-hidden="true" />
                {HEALTH_LABEL_OLLAMA}
              </span>
              <span className="qv-status-service">
                <IconVector size={13} aria-hidden="true" />
                {HEALTH_LABEL_EMBED}
              </span>
            </>
          )}
        </div>
        <button
          type="button"
          className="qv-theme-btn"
          onClick={onThemeToggle}
          aria-label={THEME_TOGGLE_LABEL}
          title={themeLabel}
        >
          {theme === 'light' ? (
            <IconMoon size={18} aria-hidden="true" />
          ) : (
            <IconSun size={18} aria-hidden="true" />
          )}
        </button>
        <button type="button" className="qv-nav-icon" aria-label={SETTINGS_LABEL}>
          <IconSettings size={18} aria-hidden="true" />
        </button>
        <button type="button" className="qv-nav-icon" aria-label={MENU_LABEL}>
          <IconMenu2 size={18} aria-hidden="true" />
        </button>
      </div>
    </header>
  );
}
