# SRS Analyzer – Enterprise Feature Set

## Roles
- USER: private requirement workspace, AI preference, analysis, SRS export.
- ADMIN: operational dashboard, AI prompt audit, token usage and system-wide metrics.

## AI pipeline
Requirement -> preference-aware prompt -> Gemini / Demo Analyzer -> structured JSON -> persisted analysis -> user stories + acceptance criteria -> usage audit.

## Export
Each requirement can be exported as a standalone HTML SRS document. Open it in a browser and use Print -> Save as PDF for a presentation-ready PDF.

## Environment
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `LLM_API_KEY`, `LLM_MODEL`.

## Workspace API (/api/workspace)
versions, generate-ab, stories, criteria, confirm, rewrite, import-docx, swimlane.png, email.
Email needs SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD (+ MAIL_FROM). Word import limit: 100,000 words / 30 MB.
