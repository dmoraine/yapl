# Changelog

All notable changes to YAPL are documented here. This project adheres to
[Semantic Versioning](https://semver.org/) and
[Keep a Changelog](https://keepachangelog.com/).

## [1.0.1] — 2026-07-22

### Added
- Take-off and landing counts are logged per flight, with day/night landing
  counters for circuits (BCAA requires the number of landings, not just who
  flew them). Airline flying keeps its single switch.
- New flights prefill the aircraft (type and registration) and the seat
  (CPT / F/O) from the last flight.
- The airport field names the airport it resolved, flags an unknown code, and
  uppercases what you type.

### Changed
- Airport data rebuilt from OurAirports (7697 -> 10023 airports, all with
  coordinates). 147 IATA codes and 1520 names corrected, including EDDB, now
  BER instead of SXF.
- Airport corrections now reach existing installs: the bundled data is replayed
  on launch when it moves ahead, leaving user-created airports untouched.

### Fixed
- Flight detail refreshes after an edit instead of showing stale values.
- Home base in Settings no longer reads as an unknown code, and a fully typed
  ICAO is saved without picking from the dropdown.
- The last row of scrolling screens no longer sits behind the Android
  navigation bar.

### Removed
- Import from FlightLogBook backup. It was a personal migration path, unguarded
  against duplicate imports. Use Import flights (CSV) or Import setup (JSON).

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
