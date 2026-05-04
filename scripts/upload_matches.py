"""
Batch uploader — lee matches_input.json y sube los partidos a Firestore.

Uso:
  1. Coloca tu serviceAccount.json en scripts/serviceAccount.json
  2. Edita scripts/matches_input.json con los partidos del día
  3. Ejecuta: python scripts/upload_matches.py

Formato de matches_input.json:
[
  {
    "season":          "liga_peruana",
    "homeTeam":        "alianza_lima",
    "visitingTeam":    "deportivo_garcilaso",
    "homePosition":    1,
    "visitingPosition": 14,
    "date":            "2026-05-05",
    "time_utc":        "18:00"
  }
]
"""

import json
import os
from datetime import datetime, timezone
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, firestore

# ── Rutas ─────────────────────────────────────────────────────────────────────

BASE_DIR      = Path(__file__).parent
SA_PATH       = BASE_DIR / "serviceAccount.json"
INPUT_PATH    = BASE_DIR / "matches_input.json"

# ── Firebase ──────────────────────────────────────────────────────────────────

def init_firebase():
    if not SA_PATH.exists():
        raise FileNotFoundError(
            f"No se encontró {SA_PATH}\n"
            "Descárgalo desde Firebase Console → Configuración → Cuentas de servicio"
        )
    cred = credentials.Certificate(str(SA_PATH))
    firebase_admin.initialize_app(cred)
    return firestore.client()

# ── Validación ────────────────────────────────────────────────────────────────

REQUIRED = ["season", "homeTeam", "visitingTeam", "homePosition", "visitingPosition", "date", "time_utc"]

def validate(entry: dict, index: int) -> bool:
    for field in REQUIRED:
        if field not in entry:
            print(f"  ❌ Entrada {index}: falta el campo '{field}' — omitiendo")
            return False
    return True

# ── Upload ────────────────────────────────────────────────────────────────────

def upload(db, entry: dict):
    try:
        h, m = map(int, entry["time_utc"].split(":"))
    except ValueError:
        h, m = 0, 0

    y, mo, d = map(int, entry["date"].split("-"))
    match_dt  = datetime(y, mo, d, h, m, 0, tzinfo=timezone.utc)

    db.collection("game").add({
        "season"          : entry["season"],
        "homeTeam"        : entry["homeTeam"],
        "visitingTeam"    : entry["visitingTeam"],
        "homePosition"    : int(entry["homePosition"]),
        "visitingPosition": int(entry["visitingPosition"]),
        "date"            : match_dt,
    })
    print(f"  ✅ {entry['homeTeam']} vs {entry['visitingTeam']} "
          f"— {entry['date']} {entry['time_utc']} UTC")

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    if not INPUT_PATH.exists():
        raise FileNotFoundError(
            f"No se encontró {INPUT_PATH}\n"
            "Crea el archivo con los partidos del día antes de ejecutar."
        )

    with open(INPUT_PATH, encoding="utf-8") as f:
        entries = json.load(f)

    if not isinstance(entries, list) or len(entries) == 0:
        print("⚠️  matches_input.json está vacío o no es una lista.")
        return

    print(f"\n📋 {len(entries)} partido(s) a subir\n")

    db    = init_firebase()
    saved = 0

    for i, entry in enumerate(entries):
        if not validate(entry, i):
            continue
        try:
            upload(db, entry)
            saved += 1
        except Exception as e:
            print(f"  ❌ Error al subir entrada {i}: {e}")

    print(f"\n✅ {saved}/{len(entries)} partido(s) guardado(s) en Firestore")

if __name__ == "__main__":
    main()
