# Changelog

All notable changes to YAPL are documented here. This project adheres to
[Semantic Versioning](https://semver.org/) and
[Keep a Changelog](https://keepachangelog.com/).

## [1.0.0] — 2026-06-11

First public release.

### Added
- Flight logging with automatic night-time computation (NOAA sunrise/sunset).
- Logbook grouped by month with running totals and a fast scrollbar.
- Statistics: all-time and 90-day totals, seeded by editable previous totals.
- Hangar of aircraft types and registrations.
- BCAA / EASA PDF export with per-page and carried-forward totals, paper-logbook
  page-break markers, and export-from-date that snaps to the start of a page.
- Previous totals with a day/night landing split for accurate PDF reconciliation.
- Backup & restore: flights CSV and a JSON reference backup; legacy JSON import.
- Bundled offline airport database (OurAirports).
- Material 3 interface with a branded splash screen.
