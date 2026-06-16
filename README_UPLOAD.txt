TREE ID TRAINER v15.15 — ANDROID SPEECH COMPATIBILITY PATCH

Upload the unzipped contents from the MAIN PAGE of the GitHub repository.

Changed only:
- public/index.html
- public/service-worker.js

Speech changes:
- Restores Phone default voice as the first option.
- Resets stale saved voice selections to Phone default.
- Does not set a voice or language when Phone default is selected.
- Avoids cancel()+speak() on an idle Android speech engine.
- Retries a failed named voice through the phone default voice.
- Preserves all v15.14 visuals, footer and import-text fix.
