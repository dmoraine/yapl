#!/usr/bin/env python3
"""
Build app/src/main/assets/airports.db from:
  - app/schemas/.../N.json   → exact Room DDL (tables + indexes)
  - res/airports.csv         → OurAirports airport rows
  - res/countries.csv        → OurAirports ISO country code → country name

The generated DB is used via createFromAsset("airports.db").  Room copies it
verbatim to pilotlog.db on first install, then validates every table against
the compiled entity definitions.  The schema here MUST match Room's schema
exactly — so we read it straight from Room's own schema-export JSON.

Refreshing the data (OurAirports publishes daily):

    curl -o res/countries.csv https://davidmegginson.github.io/ourairports-data/countries.csv
    curl -o /tmp/airports.csv https://davidmegginson.github.io/ourairports-data/airports.csv
    python3 scripts/build_airports_db.py --source /tmp/airports.csv --prune

`--prune` rewrites res/airports.csv with just the rows and columns kept below,
so the repo carries ~0.9 MB instead of the 12+ MB full feed.

Then bump AirportDataRefresher.ASSET_DATA_VERSION, or existing installs keep
the airports they already have.

The asset ships AIRPORTS ONLY.  Aircraft are deliberately never seeded — the
hangar starts empty and the maintainer's own fleet must never reach the repo.
"""
import argparse
import csv
import glob
import json
import os
import sqlite3
import sys

ROOT          = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AIRPORTS_CSV  = os.path.join(ROOT, "res", "airports.csv")
COUNTRIES_CSV = os.path.join(ROOT, "res", "countries.csv")
DST           = os.path.join(ROOT, "app", "src", "main", "assets", "airports.db")
SCHEMA_GLOB   = os.path.join(ROOT, "app", "schemas",
                             "dev.pilotlog.data.database.PilotLogDatabase", "*.json")

# Landplane/seaplane facilities only: heliports, balloonports and closed fields
# would bury real destinations in the search results.
KEEP_TYPES = {"large_airport", "medium_airport", "small_airport", "seaplane_base"}

# Columns retained by --prune, enough to rebuild the asset offline afterwards.
PRUNE_COLUMNS = ["ident", "type", "name", "latitude_deg", "longitude_deg",
                 "elevation_ft", "iso_country", "municipality",
                 "icao_code", "iata_code"]

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--source", default=AIRPORTS_CSV,
                    help="OurAirports airports.csv (default: res/airports.csv)")
parser.add_argument("--prune", action="store_true",
                    help="rewrite res/airports.csv with the kept rows/columns")
args = parser.parse_args()

# ── Load Room schema ──────────────────────────────────────────────────────────
schema_files = sorted(glob.glob(SCHEMA_GLOB))
if not schema_files:
    sys.exit("No Room schema JSON found — run ./gradlew kspDebugKotlin first")

# Use the highest-versioned schema file
schema_path = schema_files[-1]
with open(schema_path, encoding="utf-8") as f:
    room_schema = json.load(f)

db_version = room_schema["database"]["version"]
print(f"Using Room schema v{db_version} from {os.path.basename(schema_path)}")

# ── Build DDL from Room's own schema export ───────────────────────────────────
# Replace Room's ${TABLE_NAME} placeholder with the real table name.
ddl_statements = []
for entity in room_schema["database"]["entities"]:
    table_name = entity["tableName"]
    create_sql = entity["createSql"].replace("${TABLE_NAME}", table_name)
    ddl_statements.append(create_sql)
    for idx in entity.get("indices", []):
        idx_sql = idx["createSql"].replace("${TABLE_NAME}", table_name)
        ddl_statements.append(idx_sql)

# ── Country code → name ───────────────────────────────────────────────────────
if not os.path.exists(COUNTRIES_CSV):
    sys.exit(f"Source not found: {COUNTRIES_CSV}")
with open(COUNTRIES_CSV, encoding="utf-8", newline="") as f:
    COUNTRIES = {r["code"].strip().upper(): r["name"].strip()
                 for r in csv.DictReader(f) if r.get("code")}

# ── Airport row parser ────────────────────────────────────────────────────────
def parse_row(row):
    # icao_code is populated only where ICAO actually assigned one; ident falls
    # back to local codes (e.g. "AK15"), which would pollute a pilot's search.
    icao = (row.get("icao_code") or "").strip().upper()
    if len(icao) != 4 or not icao.isalpha():
        return None
    if row.get("type") not in KEEP_TYPES:
        return None

    def to_float(v):
        try:    return float(v)
        except: return None

    def to_int(v):
        try:    return int(float(v))
        except: return None

    iso = (row.get("iso_country") or "").strip().upper()

    return {
        "icao":         icao,
        "iata":         (row.get("iata_code") or "").strip().upper(),
        "name":         (row.get("name") or "").strip(),
        "municipality": (row.get("municipality") or "").strip(),
        "country":      COUNTRIES.get(iso, iso),
        "latitude":     to_float(row.get("latitude_deg")),
        "longitude":    to_float(row.get("longitude_deg")),
        "elevation_ft": to_int(row.get("elevation_ft")),
        # Never read by the app (night time comes from lat/lon, and
        # AddAirportDialog already hardcodes "UTC"); OurAirports ships no tz.
        "timezone":     "UTC",
        "is_custom":    0,
    }

# ── Build the DB ──────────────────────────────────────────────────────────────
os.makedirs(os.path.dirname(DST), exist_ok=True)
if os.path.exists(DST):
    os.remove(DST)

con = sqlite3.connect(DST)
cur = con.cursor()

for stmt in ddl_statements:
    cur.execute(stmt)

# user_version must match @Database(version=N) so Room skips migration on
# first open.  room_master_table is intentionally absent — Room creates it
# with the correct identity_hash on first open (skips validation if absent).
cur.execute(f"PRAGMA user_version = {db_version}")

# ── Pre-populate airports ─────────────────────────────────────────────────────
if not os.path.exists(args.source):
    sys.exit(f"Source not found: {args.source}")

kept_raw = []
inserted = skipped = 0
with open(args.source, encoding="utf-8", newline="") as f:
    for raw_row in csv.DictReader(f):
        row = parse_row(raw_row)
        if row is None:
            skipped += 1
            continue
        kept_raw.append(raw_row)
        cur.execute(
            """INSERT OR IGNORE INTO airports
               (icao, iata, name, municipality, country,
                latitude, longitude, elevation_ft, timezone, is_custom)
               VALUES (:icao, :iata, :name, :municipality, :country,
                       :latitude, :longitude, :elevation_ft, :timezone, :is_custom)""",
            row,
        )
        inserted += 1

con.commit()

total      = cur.execute("SELECT COUNT(*) FROM airports").fetchone()[0]
with_coord = cur.execute("SELECT COUNT(*) FROM airports WHERE latitude IS NOT NULL").fetchone()[0]
tables     = [r[0] for r in cur.execute(
    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
).fetchall()]

# The hangar must ship empty — a stray seed file would publish personal aircraft.
for table in ("aircraft", "aircraft_types"):
    count = cur.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
    if count:
        con.close()
        os.remove(DST)
        sys.exit(f"ABORT: {count} rows in {table} — the asset ships airports only")

con.close()

# ── Optionally shrink the checked-in source ───────────────────────────────────
if args.prune:
    with open(AIRPORTS_CSV, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=PRUNE_COLUMNS)
        writer.writeheader()
        for row in kept_raw:
            writer.writerow({c: row.get(c, "") for c in PRUNE_COLUMNS})
    print(f"Pruned source: {AIRPORTS_CSV} ({len(kept_raw)} rows)")

print(f"Tables: {tables}")
print(f"airports.db built: {total} airports ({with_coord} with coords, {skipped} skipped)")
print(f"Output: {DST}")
