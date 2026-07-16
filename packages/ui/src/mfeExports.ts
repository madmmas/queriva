/**
 * Public Module Federation type surface for host apps (SPEC §11, issue #26).
 *
 * Hosts can import these types alongside the federated `SearchWidget` remote.
 */
export type { SearchMode, SearchResult } from './types/api';
export type { SearchWidgetProps } from './types/widget';
export { default as SearchWidget } from './SearchWidget';
