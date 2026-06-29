# UI test conventions

See `test-quality.mdc` Section D.

## Tags

Vitest uses file naming and `describe` blocks — no explicit tags yet.
- `*.test.tsx` — unit/component tests (`make test-unit`, `make test-ui`)
- Integration tests (Playwright E2E) — issue #28, run via `make smoke`

## Run commands

```bash
make test-ui
cd packages/ui && npm run test-unit
```

## API mocking

All fetch calls in component and hook tests are intercepted by MSW handlers
in `src/__tests__/handlers.ts`. Never use `vi.mock('fetch')`.

## Accessibility

Every page-level component must pass jest-axe in tests.

## Coverage

Vitest coverage threshold: 80% lines on `src/**` (see `vite.config.ts`).
