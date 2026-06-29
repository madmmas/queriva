# mock-ui

Design references for Queriva UI implementation.

## Contents

- `queriva_mock_ui_v2.html` — interactive mock of the full search interface.
  Open directly in a browser — no build step needed. Use as the visual reference
  for issues #19–#24.

- `queriva-icon-files/` — production-ready SVG brand assets (icon, wordmarks,
  favicon). These are the source files. During issue #19, the relevant files
  are copied into `packages/ui/public/` for the React app.

## Usage

Issues that reference mock-ui:
- Issue #19 — copy icons to packages/ui/public/, extract CSS variables
- Issue #20 — match TopBar and SearchBar to mock HTML
- Issue #21 — match ResultCard and StatsPanel to mock HTML
- Issue #22 — match AISummary panel to mock HTML