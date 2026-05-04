"""
Daily match filler.
Lee ligas de Firestore "tournament" y equipos de "team".
Por cada liga: 2 prompts a Gemini → standings → match Top5 vs Bottom5.
Escribe resultados en Firestore "game".
"""

import json
import os
import re
import unicodedata
from datetime import datetime, timezone, timedelta
from zoneinfo import ZoneInfo

import firebase_admin
from firebase_admin import credentials, firestore
from google import genai
from google.genai import types

# ── Config ──────────────────────────────────────────────────────────────────

MODEL = "gemini-2.5-flash"

# ── Firebase ─────────────────────────────────────────────────────────────────

def init_firebase():
    sa_json = os.environ.get("FIREBASE_SERVICE_ACCOUNT")
    cred = credentials.Certificate(json.loads(sa_json)) if sa_json \
        else credentials.ApplicationDefault()
    firebase_admin.initialize_app(cred)
    return firestore.client()

# ── Gemini ────────────────────────────────────────────────────────────────────

def make_gemini_client():
    return genai.Client(api_key=os.environ["GEMINI_API_KEY"])

def call_gemini(client, prompt: str) -> str:
    config = types.GenerateContentConfig(
        tools=[types.Tool(google_search=types.GoogleSearch())],
        temperature=0.0,
        max_output_tokens=4096,
    )
    return client.models.generate_content(
        model=MODEL,
        contents=prompt,
        config=config,
    ).text or ""

# ── Normalización y match de keys ────────────────────────────────────────────

def normalize(text: str) -> str:
    nfkd = unicodedata.normalize("NFKD", text.lower().strip())
    ascii_str = nfkd.encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9_]", "", ascii_str.replace(" ", "_").replace("-", "_"))

def match_key(ai_name: str, valid_keys: set) -> str | None:
    """Busca el document ID más parecido en la colección team."""
    if ai_name in valid_keys:
        return ai_name

    norm = normalize(ai_name)
    if norm in valid_keys:
        return norm

    for k in valid_keys:
        if normalize(k) == norm:
            return k

    words = [w for w in norm.split("_") if len(w) >= 3]
    if words:
        candidates = [k for k in valid_keys if all(w in normalize(k) for w in words)]
        if len(candidates) == 1:
            return candidates[0]
        if len(candidates) > 1:
            return min(candidates, key=len)

        reverse = [k for k in valid_keys
                   if all(w in norm for w in normalize(k).split("_") if len(w) >= 3)]
        if len(reverse) == 1:
            return reverse[0]

    return None

# ── Extracción de JSON ────────────────────────────────────────────────────────

def extract_json(text: str) -> dict | None:
    text = text.strip()
    if text.startswith("```"):
        nl = text.find("\n")
        text = text[nl + 1:] if nl != -1 else text
    text = text.rstrip("`").strip()
    start = text.find("{")
    if start >= 0:
        text = text[start:]
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return None

# ── Prompt 1: tabla de posiciones ────────────────────────────────────────────

STANDINGS_PROMPT = """
Eres un extractor de datos deportivos. Fecha hoy: {today}.

Busca en sofascore.com o flashscore.com la tabla de posiciones ACTUAL
de {league_name} (temporada en curso).

REGLAS:
- Usa SOLO fuentes fechadas en los últimos 7 días
- Si no encuentras fuente reciente devuelve: {{"error": "no_data"}}
- NO uses conocimiento de entrenamiento para inferir posiciones
- Devuelve TODOS los equipos en orden de posición

Devuelve SOLO este JSON sin texto adicional:
{{
  "source_url": "https://...",
  "standings": [
    {{"pos": 1, "team_name": "Nombre del equipo"}},
    {{"pos": 2, "team_name": "Nombre del equipo"}}
  ]
}}
""".strip()

def get_standings(client, league_name: str, today_str: str) -> list[dict] | None:
    prompt = STANDINGS_PROMPT.format(league_name=league_name, today=today_str)
    for attempt in range(2):
        data = extract_json(call_gemini(client, prompt))
        if data and "standings" in data and len(data["standings"]) >= 8:
            print(f"  📊 Tabla obtenida ({len(data['standings'])} equipos) — {data.get('source_url','')}")
            return data["standings"]
        print(f"  ⚠️  Intento {attempt + 1} fallido para standings de {league_name}")
    return None

# ── Prompt 2: partido del día ────────────────────────────────────────────────

MATCH_PROMPT = """
Fecha exacta buscada: {today} ({today_readable}).
Liga: {league_name}

TOP 5 (primeros de la tabla): {top5}
BOTTOM 5 (últimos de la tabla): {bottom5}

Busca en sofascore.com o flashscore.com si hay un partido programado
el día {today} donde un equipo del TOP 5 juegue contra un equipo del BOTTOM 5.

REGLAS CRÍTICAS:
- Los equipos del resultado DEBEN estar exactamente en las listas anteriores
- La fecha del partido DEBE ser exactamente {today} — no el día anterior ni posterior
- Si el partido es de otra fecha, devuelve null
- Si no encuentras el partido en una fuente oficial devuelve null
- Convierte el horario a UTC
- El campo match_date debe reflejar la fecha REAL del partido según la fuente

Devuelve SOLO este JSON:
{{"match": {{"home_name": "nombre exacto de la lista", "away_name": "nombre exacto de la lista", "time_utc": "HH:MM", "match_date": "{today}"}}}}
o si no hay partido:
{{"match": null}}
""".strip()

def find_match_today(client, league_name: str, top5: list, bottom5: list,
                     today_str: str, today_readable: str) -> dict | None:
    prompt = MATCH_PROMPT.format(
        league_name    = league_name,
        today          = today_str,
        today_readable = today_readable,
        top5           = ", ".join(t["team_name"] for t in top5),
        bottom5        = ", ".join(t["team_name"] for t in bottom5),
    )
    for attempt in range(2):
        data = extract_json(call_gemini(client, prompt))
        if data is not None:
            return data.get("match")
        print(f"  ⚠️  Intento {attempt + 1} fallido para match de {league_name}")
    return None

# ── Validación y resolución de keys ──────────────────────────────────────────

def validate_and_resolve(match: dict, top5: list, bottom5: list,
                         team_keys: set, today_str: str) -> dict | None:
    match_date = match.get("match_date", "")
    if match_date and match_date != today_str:
        print(f"  ❌ Fecha incorrecta: la IA devolvió partido del {match_date}, se busca {today_str}")
        return None

    all_teams = {t["team_name"].lower(): t for t in top5 + bottom5}

    home_entry = all_teams.get(match["home_name"].lower())
    away_entry = all_teams.get(match["away_name"].lower())

    if not home_entry or not away_entry:
        print(f"  ❌ Equipos no encontrados en lista: '{match['home_name']}', '{match['away_name']}'")
        return None

    home_key = match_key(match["home_name"], team_keys) or normalize(match["home_name"])
    away_key = match_key(match["away_name"], team_keys) or normalize(match["away_name"])

    return {
        "home_key": home_key,
        "home_pos": home_entry["pos"],
        "away_key": away_key,
        "away_pos": away_entry["pos"],
        "time_utc": match.get("time_utc", "00:00"),
    }

# ── Escritura en Firestore ────────────────────────────────────────────────────

def write_game(db, league_key: str, game: dict, today_str: str):
    try:
        h, m = map(int, game["time_utc"].split(":"))
    except ValueError:
        h, m = 0, 0

    y, mo, d = map(int, today_str.split("-"))
    match_dt = datetime(y, mo, d, h, m, 0, tzinfo=timezone.utc)

    db.collection("game").add({
        "season"          : league_key,
        "homeTeam"        : game["home_key"],
        "visitingTeam"    : game["away_key"],
        "homePosition"    : game["home_pos"],
        "visitingPosition": game["away_pos"],
        "date"            : match_dt,
    })
    print(f"  ✅ Guardado: {game['home_key']} ({game['home_pos']}) "
          f"vs {game['away_key']} ({game['away_pos']}) — {game['time_utc']} UTC")

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    db     = init_firebase()
    client = make_gemini_client()

    tz             = ZoneInfo(os.environ.get("APP_TIMEZONE", "America/Lima"))
    days_ahead     = int(os.environ.get("DAYS_AHEAD", "1"))
    target         = datetime.now(tz) + timedelta(days=days_ahead)
    today_str      = target.strftime("%Y-%m-%d")
    today_readable = target.strftime("%-d de %B de %Y")

    print(f"\n📅 Procesando partidos para: {today_str}\n")

    # Leer ligas desde Firestore
    tournaments = {doc.id: doc.to_dict() for doc in db.collection("tournament").stream()}
    print(f"🏆 Ligas en Firestore: {list(tournaments.keys())}")

    # Leer equipos desde Firestore (solo los IDs para el match)
    team_keys = {doc.id for doc in db.collection("team").stream()}
    print(f"👥 Equipos en Firestore: {len(team_keys)}\n")

    saved = 0
    for league_key, league_data in tournaments.items():
        league_name = league_data.get("name", league_key)
        print(f"\n⚽ [{league_key}] {league_name}")

        # Prompt 1 — tabla de posiciones
        standings = get_standings(client, league_name, today_str)
        if not standings:
            print(f"  — Sin tabla disponible, omitiendo")
            continue

        n       = len(standings)
        top5    = standings[:5]
        bottom5 = standings[max(0, n - 5):]

        print(f"  TOP 5   : {[t['team_name'] for t in top5]}")
        print(f"  BOTTOM 5: {[t['team_name'] for t in bottom5]}")

        # Prompt 2 — partido del día
        match = find_match_today(client, league_name, top5, bottom5,
                                 today_str, today_readable)
        if not match:
            print(f"  — Sin partido Top5 vs Bottom5 hoy")
            continue

        print(f"  🔍 Partido encontrado: {match['home_name']} vs {match['away_name']}")

        # Validar equipos contra listas enviadas + resolver keys de Firestore
        resolved = validate_and_resolve(match, top5, bottom5, team_keys, today_str)
        if not resolved:
            continue

        write_game(db, league_key, resolved, today_str)
        saved += 1

    print(f"\n✅ Proceso completado — {saved} partido(s) guardado(s) para {today_str}")

if __name__ == "__main__":
    main()
