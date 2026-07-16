# Queriva host example (Module Federation)

Consumes the Queriva `SearchWidget` remote per SPEC §11 / ADR-010.

## Prerequisites

1. Build and preview the UI remote (from repo root):

```bash
cd packages/ui
npm run build
npm run preview   # serves remoteEntry at http://localhost:5173/assets/remoteEntry.js
```

2. Install and run this host:

```bash
cd packages/ui/examples/host
cp .env.example .env   # optional
npm install
npm run dev            # http://localhost:5174
```

## Env

| Variable | Default | Purpose |
|---|---|---|
| `VITE_QUERIVA_REMOTE_ENTRY` | `http://localhost:5173/assets/remoteEntry.js` | Remote entry URL |
| `VITE_API_URL` | `http://localhost:8080` | Queriva API base URL |
| `VITE_DEFAULT_COLLECTION` | `news_radar` | Collection name |
