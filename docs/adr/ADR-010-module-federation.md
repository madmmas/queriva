# ADR-010 — Micro-frontend: Module Federation over iframe / Web Components

## Status
Accepted

## Context
Queriva's UI must work in two modes:

1. **Standalone:** a full React SPA at `localhost:3000`
2. **Embeddable:** a `SearchWidget` component importable by host apps (AIPlane, News Radar)

The embeddable mode requires a decision on the micro-frontend (MFE) strategy.
Three main options exist: Module Federation, iframe embedding, and Web Components.

## Decision
Use **Vite Module Federation** (`@originjs/vite-plugin-federation`) to export
`SearchWidget` as a remote module consumable by host applications.

## Alternatives Considered

| Option | Reason rejected |
|---|---|
| **iframe** | Simplest isolation model but cannot share CSS variables (theme tokens), React context, or state with the host. Host cannot call methods on the widget or receive `onResultClick` callbacks. iframe resizing is awkward. |
| **Web Components** | Framework-agnostic — works with any host. But TypeScript props API is painful (attributes vs properties), React event handling inside a shadow DOM requires workarounds, and CSS custom properties need explicit forwarding through the shadow boundary. |
| **npm package (build-time)** | Publishing `@queriva/search-widget` to npm would require consumers to rebuild their app on every Queriva update. Module Federation allows runtime loading — the host always gets the latest remote without rebuilding. |

## Module Federation Design

```typescript
// packages/ui/vite.config.ts (remote — Queriva)
federation({
  name: 'queriva',
  filename: 'remoteEntry.js',
  exposes: {
    './SearchWidget': './src/SearchWidget.tsx'
  },
  shared: ['react', 'react-dom']
})

// Host app vite.config.ts (AIPlane / News Radar)
federation({
  remotes: {
    queriva: 'http://localhost:5173/assets/remoteEntry.js'
  },
  shared: ['react', 'react-dom']
})
```

Sharing `react` and `react-dom` ensures both host and remote use the same
React instance — required for hooks and context to work correctly across the boundary.

## Props API
The `SearchWidgetProps` TypeScript interface (SPEC §11) is exported alongside
the component, giving host apps full type safety without a separate type package.

## Consequences

**Makes easier:**
- Full TypeScript type safety for `SearchWidgetProps` in host apps
- Shared CSS variables from host theme flow into the widget naturally (no shadow DOM barrier)
- `onResultClick` callback works as a standard React prop
- Host app displays widget at current Queriva version without rebuilding

**Makes harder:**
- Both host and remote must use compatible Vite + Module Federation plugin versions
- `remoteEntry.js` URL must be known at host build time (configurable via env var)
- Shared `react` version must be compatible — pin versions in both repos
- Cold load: browser fetches `remoteEntry.js` on first render (~one extra network request)

## References
- SPEC.md §11 (MFE design), §5 (repo structure — `vite.config.ts`)
- Issue #25 (SearchWidget Module Federation), #26 (MFE integration tests)
- ADR-004 (Spring Boot — explains host app context: AIPlane is also React-fronted)
