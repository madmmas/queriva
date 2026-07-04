import { ERROR_BANNER_PREFIX } from '../constants/ui';

interface ErrorBannerProps {
  message: string;
}

/**
 * Shown when POST /api/search fails (issue #23).
 */
export function ErrorBanner({ message }: ErrorBannerProps) {
  return (
    <div className="qv-error-banner" role="alert">
      {ERROR_BANNER_PREFIX} {message}
    </div>
  );
}
