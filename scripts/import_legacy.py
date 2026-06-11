#!/usr/bin/env python3
"""
Import legacy DB_export_FLIGHTLOGBOOK.txt (com.mattia.flightlogbook JSON)
into a fresh pilotlog.db SQLite file matching Room's FlightEntity schema.

Usage:
    python3 scripts/import_legacy.py \
        [--json  legacy/DB_export_FLIGHTLOGBOOK.txt] \
        [--out   /tmp/legacy_flights.db]

The resulting SQLite is a Room-compatible database (user_version=1, no
room_master_table so Room regenerates its hash on first open) that can be
pushed to a device or used for development seeding.
"""
import argparse
import datetime
import json
import os
import sqlite3
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

DEFAULT_JSON = os.path.join(ROOT, "legacy", "DB_export_FLIGHTLOGBOOK.txt")
DEFAULT_OUT  = os.path.join(ROOT, "legacy", "legacy_flights.db")

FLIGHTS_SCHEMA = """
CREATE TABLE IF NOT EXISTS flights (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    date              TEXT    NOT NULL,
    dep_airport       TEXT    NOT NULL,
    dep_time          TEXT    NOT NULL,
    arr_airport       TEXT    NOT NULL,
    arr_time          TEXT    NOT NULL,
    ac_type           TEXT    NOT NULL DEFAULT '',
    ac_registration   TEXT    NOT NULL DEFAULT '',
    total_min         INTEGER NOT NULL DEFAULT 0,
    night_min         INTEGER NOT NULL DEFAULT 0,
    ifr_min           INTEGER NOT NULL DEFAULT 0,
    pic_min           INTEGER NOT NULL DEFAULT 0,
    copilot_min       INTEGER NOT NULL DEFAULT 0,
    dual_min          INTEGER NOT NULL DEFAULT 0,
    instructor_min    INTEGER NOT NULL DEFAULT 0,
    takeoffs_day      INTEGER NOT NULL DEFAULT 0,
    takeoffs_night    INTEGER NOT NULL DEFAULT 0,
    landings_day      INTEGER NOT NULL DEFAULT 0,
    landings_night    INTEGER NOT NULL DEFAULT 0,
    is_multi_pilot    INTEGER NOT NULL DEFAULT 0,
    flight_number     TEXT    NOT NULL DEFAULT '',
    remarks           TEXT    NOT NULL DEFAULT ''
);
"""

AIRCRAFT_SCHEMA = """
CREATE TABLE IF NOT EXISTS aircraft (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    type_code    TEXT    NOT NULL,
    type_name    TEXT    NOT NULL DEFAULT '',
    registration TEXT    NOT NULL UNIQUE,
    engine_type  TEXT    NOT NULL DEFAULT 'MULTI'
);
"""


def epoch_ms_to_date(epoch_ms: int) -> str:
    """Convert epoch milliseconds to ISO-8601 date string (UTC)."""
    dt = datetime.datetime.fromtimestamp(epoch_ms / 1000.0, tz=datetime.timezone.utc)
    return dt.strftime("%Y-%m-%d")


def minutes_to_hhmm(minutes: int) -> str:
    """Convert minutes-from-midnight to 'HH:MM' string."""
    return "%02d:%02d" % (minutes // 60 % 24, minutes % 60)


def build_lookup(models: list, aircrafts: list) -> dict:
    """
    Returns a dict: aircraftId -> {type_code, type_name, registration, engine_type}
    """
    model_map = {m["id"]: m for m in models}
    result = {}
    for ac in aircrafts:
        model_id = int(ac.get("modelId", 0))
        model = model_map.get(model_id, {})
        result[ac["id"]] = {
            "type_code":    model.get("code", ""),
            "type_name":    model.get("fullName", ""),
            "registration": ac.get("registration", ""),
            "engine_type":  "MULTI" if model.get("multiEngine", True) else "SINGLE",
        }
    return result


def main():
    parser = argparse.ArgumentParser(description="Import legacy flightlogbook JSON")
    parser.add_argument("--json", default=DEFAULT_JSON)
    parser.add_argument("--out",  default=DEFAULT_OUT)
    args = parser.parse_args()

    if not os.path.exists(args.json):
        sys.exit(f"JSON not found: {args.json}")

    with open(args.json, encoding="utf-8") as f:
        data = json.load(f)

    events   = data.get("events",   [])
    models   = data.get("acModels", [])
    aircraft = data.get("aircrafts", [])

    ac_lookup = build_lookup(models, aircraft)

    if os.path.exists(args.out):
        os.remove(args.out)

    con = sqlite3.connect(args.out)
    cur = con.cursor()
    cur.executescript(FLIGHTS_SCHEMA + AIRCRAFT_SCHEMA)

    # Insert all unique aircraft
    seen_regs: set[str] = set()
    for ac_info in ac_lookup.values():
        reg = ac_info["registration"].strip().upper()
        if reg and reg not in seen_regs:
            seen_regs.add(reg)
            cur.execute(
                "INSERT OR IGNORE INTO aircraft (type_code, type_name, registration, engine_type) "
                "VALUES (?, ?, ?, ?)",
                (ac_info["type_code"], ac_info["type_name"], reg, ac_info["engine_type"]),
            )

    # Insert flights (sorted oldest first to preserve natural order)
    events_sorted = sorted(events, key=lambda e: e.get("logDate", 0))
    imported = skipped = 0

    for evt in events_sorted:
        if evt.get("type", 0) != 0:   # type=0 is actual flight; skip sim sessions etc.
            skipped += 1
            continue

        try:
            date_str = epoch_ms_to_date(evt["logDate"])
            dep_time = minutes_to_hhmm(int(evt["depTime"]))
            arr_time = minutes_to_hhmm(int(evt["arrTime"]))

            ac_id = evt.get("aircraftId")
            ac = ac_lookup.get(ac_id, {})

            cur.execute(
                """INSERT INTO flights
                   (date, dep_airport, dep_time, arr_airport, arr_time,
                    ac_type, ac_registration, total_min, night_min, ifr_min,
                    pic_min, copilot_min, dual_min, instructor_min,
                    takeoffs_day, takeoffs_night, landings_day, landings_night,
                    is_multi_pilot, flight_number, remarks)
                   VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                (
                    date_str,
                    evt.get("depPlace", "").strip().upper(),
                    dep_time,
                    evt.get("arrPlace", "").strip().upper(),
                    arr_time,
                    ac.get("type_code", ""),
                    ac.get("registration", ""),
                    int(evt.get("length", 0)),
                    int(evt.get("nightTime", 0)),
                    int(evt.get("ifrTime", 0)),
                    int(evt.get("picTime", 0)),
                    int(evt.get("coPilotTime", 0)),
                    int(evt.get("dualTime", 0)),
                    int(evt.get("instructorTime", 0)),
                    int(evt.get("takeoffDay", 0)),
                    int(evt.get("takeoffNight", 0)),
                    int(evt.get("landingDay", 0)),
                    int(evt.get("landingNight", 0)),
                    1 if int(evt.get("multiPilotTime", 0)) > 0 else 0,
                    evt.get("flightNumb", ""),
                    evt.get("remarks", ""),
                ),
            )
            imported += 1
        except Exception as e:
            print(f"Warning: skipped event id={evt.get('id')}: {e}", file=sys.stderr)
            skipped += 1

    con.execute("PRAGMA user_version = 1")
    con.commit()

    total_flights = cur.execute("SELECT COUNT(*) FROM flights").fetchone()[0]
    total_aircraft = cur.execute("SELECT COUNT(*) FROM aircraft").fetchone()[0]
    con.close()

    print(f"Imported: {imported} flights, {total_aircraft} aircraft  ({skipped} skipped)")
    print(f"Output:   {args.out}")


if __name__ == "__main__":
    main()
