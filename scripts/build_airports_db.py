#!/usr/bin/env python3
"""
Build app/src/main/assets/airports.db from:
  - app/schemas/.../2.json  → exact Room DDL (tables + indexes)
  - res/airports.dat        → OurAirports CSV (pre-populated airport rows)

The generated DB is used via createFromAsset("airports.db").  Room copies it
verbatim to pilotlog.db on first install, then validates every table against
the compiled entity definitions.  The schema here MUST match Room's schema
exactly — so we read it straight from Room's own schema-export JSON.
"""
import csv
import glob
import json
import os
import sqlite3
import sys

ROOT        = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AIRPORTS_DAT = os.path.join(ROOT, "res", "airports.dat")
AIRCRAFT_SEED = os.path.join(ROOT, "res", "aircraft_seed.csv")
DST          = os.path.join(ROOT, "app", "src", "main", "assets", "airports.db")
SCHEMA_GLOB  = os.path.join(ROOT, "app", "schemas",
                            "dev.pilotlog.data.database.PilotLogDatabase", "*.json")

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

# ── Airport row parser ────────────────────────────────────────────────────────
def parse_row(row):
    try:
        (
            _id, name, city, country, iata, icao,
            lat, lon, elev, _utc_offset, _dst, timezone,
            airport_type, _source,
        ) = row
    except ValueError:
        return None

    icao = icao.strip()
    if not icao or icao == "\\N":
        return None

    if airport_type not in (
        "airport", "large_airport", "medium_airport",
        "small_airport", "seaplane_base",
    ):
        return None

    iata     = iata.strip() if iata.strip() != "\\N" else ""
    timezone = timezone.strip() if timezone.strip() not in ("", "\\N") else "UTC"

    def to_float(v):
        try:    return float(v)
        except: return None

    def to_int(v):
        try:    return int(float(v))
        except: return None

    return {
        "icao":         icao.upper(),
        "iata":         iata.upper(),
        "name":         name.strip(),
        "municipality": city.strip(),
        "country":      country.strip(),
        "latitude":     to_float(lat),
        "longitude":    to_float(lon),
        "elevation_ft": to_int(elev),
        "timezone":     timezone,
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
if not os.path.exists(AIRPORTS_DAT):
    sys.exit(f"Source not found: {AIRPORTS_DAT}")

inserted = skipped = 0
with open(AIRPORTS_DAT, encoding="utf-8", newline="") as f:
    for raw_row in csv.reader(f):
        row = parse_row(raw_row)
        if row is None:
            skipped += 1
            continue
        cur.execute(
            """INSERT OR IGNORE INTO airports
               (icao, iata, name, municipality, country,
                latitude, longitude, elevation_ft, timezone, is_custom)
               VALUES (:icao, :iata, :name, :municipality, :country,
                       :latitude, :longitude, :elevation_ft, :timezone, :is_custom)""",
            row,
        )
        inserted += 1

# ── Pre-populate aircraft (types + registrations) ─────────────────────────────
ac_types = ac_regs = 0
if os.path.exists(AIRCRAFT_SEED):
    seen_types = set()
    with open(AIRCRAFT_SEED, encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            type_code = (row.get("type_code") or "").strip().upper()
            type_name = (row.get("type_name") or "").strip()
            engine    = (row.get("engine_type") or "MULTI").strip().upper()
            reg       = (row.get("registration") or "").strip().upper()
            if not type_code or not reg:
                continue
            if type_code not in seen_types:
                seen_types.add(type_code)
                cur.execute(
                    """INSERT OR IGNORE INTO aircraft_types (type_code, type_name, engine_type)
                       VALUES (?, ?, ?)""",
                    (type_code, type_name or type_code, engine),
                )
                ac_types += 1
            cur.execute(
                "INSERT OR IGNORE INTO aircraft (registration, type_code) VALUES (?, ?)",
                (reg, type_code),
            )
            ac_regs += 1

con.commit()

total      = cur.execute("SELECT COUNT(*) FROM airports").fetchone()[0]
with_coord = cur.execute("SELECT COUNT(*) FROM airports WHERE latitude IS NOT NULL").fetchone()[0]
tables     = [r[0] for r in cur.execute(
    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
).fetchall()]

con.close()

print(f"Tables: {tables}")
print(f"airports.db built: {total} airports ({with_coord} with coords, {skipped} skipped)")
print(f"aircraft seeded: {ac_types} types, {ac_regs} registrations")
print(f"Output: {DST}")
