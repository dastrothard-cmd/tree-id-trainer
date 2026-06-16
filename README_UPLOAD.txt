TREE ID TRAINER v15.16 — ANDROID SPEECH ENGINE RESET

Upload the unzipped contents from the MAIN PAGE of the GitHub repository.

Changed only:
- public/index.html
- public/service-worker.js

This version uses three automatic speech methods:
1. Selected installed voice + its exact language
2. Phone default voice + phone language
3. Completely raw Android default utterance

It also force-clears Android's failed speech queue before every attempt.

All visuals and other features are untouched.
