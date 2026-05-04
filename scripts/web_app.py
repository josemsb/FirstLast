"""
Web local para gestionar partidos antes de subirlos a Firestore.
Uso:
  pip install flask firebase-admin
  python scripts/web_app.py
  Abre http://localhost:5000
"""

import json
from datetime import datetime, timezone
from pathlib import Path

from flask import Flask, jsonify, render_template_string, request

import firebase_admin
from firebase_admin import credentials, firestore

# ── Rutas ─────────────────────────────────────────────────────────────────────

BASE_DIR   = Path(__file__).parent
SA_PATH    = BASE_DIR / "serviceAccount.json"
INPUT_PATH = BASE_DIR / "matches_input.json"

# ── Firebase ──────────────────────────────────────────────────────────────────

db          = None
teams_dict  = {}
leagues_dict = {}

def init_firebase():
    global db, teams_dict, leagues_dict
    if not SA_PATH.exists():
        print("⚠️  serviceAccount.json no encontrado — sin conexión a Firestore")
        return
    cred = credentials.Certificate(str(SA_PATH))
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    teams_dict   = {d.id: d.to_dict().get("name", d.id) for d in db.collection("team").stream()}
    leagues_dict = {d.id: d.to_dict().get("name", d.id) for d in db.collection("tournament").stream()}
    print(f"✅ Firebase conectado — {len(teams_dict)} equipos, {len(leagues_dict)} ligas")

# ── Helpers ───────────────────────────────────────────────────────────────────

def read_matches():
    if INPUT_PATH.exists():
        with open(INPUT_PATH, encoding="utf-8") as f:
            return json.load(f)
    return []

def save_matches(matches):
    with open(INPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(matches, f, indent=2, ensure_ascii=False)

# ── App ───────────────────────────────────────────────────────────────────────

app = Flask(__name__)

# ── API ───────────────────────────────────────────────────────────────────────

@app.route("/api/meta")
def meta():
    return jsonify({"teams": teams_dict, "leagues": leagues_dict, "firebase": db is not None})

@app.route("/api/matches", methods=["GET"])
def get_matches():
    return jsonify(read_matches())

@app.route("/api/matches", methods=["POST"])
def add_match():
    matches = read_matches()
    matches.append(request.json)
    save_matches(matches)
    return jsonify({"ok": True})

@app.route("/api/matches/<int:idx>", methods=["PUT"])
def update_match(idx):
    matches = read_matches()
    if 0 <= idx < len(matches):
        matches[idx] = request.json
        save_matches(matches)
    return jsonify({"ok": True})

@app.route("/api/matches/<int:idx>", methods=["DELETE"])
def delete_match(idx):
    matches = read_matches()
    if 0 <= idx < len(matches):
        matches.pop(idx)
        save_matches(matches)
    return jsonify({"ok": True})

@app.route("/api/upload", methods=["POST"])
def upload():
    if db is None:
        return jsonify({"error": "Falta serviceAccount.json"}), 400
    matches = read_matches()
    saved, errors = 0, []
    for i, e in enumerate(matches):
        try:
            h, m   = map(int, e["time_utc"].split(":"))
            y,mo,d = map(int, e["date"].split("-"))
            db.collection("game").add({
                "season"          : e["season"],
                "homeTeam"        : e["homeTeam"],
                "visitingTeam"    : e["visitingTeam"],
                "homePosition"    : int(e["homePosition"]),
                "visitingPosition": int(e["visitingPosition"]),
                "date"            : datetime(y, mo, d, h, m, 0, tzinfo=timezone.utc),
            })
            saved += 1
        except Exception as ex:
            errors.append(f"Entrada {i}: {ex}")
    return jsonify({"saved": saved, "total": len(matches), "errors": errors})

# ── HTML ──────────────────────────────────────────────────────────────────────

HTML = r"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>FirstLast · Panel de Partidos</title>
<style>
  :root{--green:#2E7D32;--green-light:#4CAF50;--red:#c62828;--gray:#f5f5f5;--border:#e0e0e0}
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Segoe UI',sans-serif;background:#fafafa;color:#212121}
  header{background:var(--green);color:#fff;padding:16px 24px;display:flex;align-items:center;gap:12px}
  header h1{font-size:1.3rem;font-weight:700}
  header span{font-size:1.5rem}
  #firebase-badge{margin-left:auto;font-size:.75rem;padding:4px 10px;border-radius:20px;font-weight:600}
  .connected{background:#a5d6a7;color:#1b5e20}
  .disconnected{background:#ef9a9a;color:#b71c1c}
  main{max-width:960px;margin:24px auto;padding:0 16px;display:grid;gap:24px}
  .card{background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,.08);padding:24px}
  h2{font-size:1rem;font-weight:700;color:var(--green);margin-bottom:16px;text-transform:uppercase;letter-spacing:.05em}
  .form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}
  @media(max-width:600px){.form-grid{grid-template-columns:1fr}}
  label{font-size:.8rem;font-weight:600;color:#555;display:block;margin-bottom:4px}
  input,select{width:100%;padding:8px 10px;border:1px solid var(--border);border-radius:8px;font-size:.9rem;outline:none;transition:border .2s}
  input:focus,select:focus{border-color:var(--green-light)}
  .btn{padding:10px 20px;border:none;border-radius:8px;font-size:.9rem;font-weight:600;cursor:pointer;transition:opacity .2s}
  .btn:hover{opacity:.85}
  .btn-green{background:var(--green);color:#fff}
  .btn-red{background:var(--red);color:#fff}
  .btn-outline{background:#fff;color:var(--green);border:2px solid var(--green)}
  .btn-sm{padding:5px 12px;font-size:.8rem}
  .form-actions{display:flex;gap:8px;margin-top:8px}
  table{width:100%;border-collapse:collapse;font-size:.88rem}
  th{text-align:left;padding:8px 10px;border-bottom:2px solid var(--border);color:#757575;font-weight:600;font-size:.78rem;text-transform:uppercase}
  td{padding:10px;border-bottom:1px solid var(--border);vertical-align:middle}
  tr:last-child td{border-bottom:none}
  .badge{display:inline-block;padding:2px 8px;border-radius:12px;font-size:.75rem;font-weight:600}
  .badge-home{background:#e8f5e9;color:#2e7d32}
  .badge-away{background:#fff3e0;color:#e65100}
  .upload-bar{display:flex;align-items:center;gap:12px;flex-wrap:wrap}
  #upload-status{font-size:.88rem;font-weight:600}
  .ok{color:var(--green)}.err{color:var(--red)}
  .empty{text-align:center;padding:32px;color:#9e9e9e;font-size:.9rem}
  .day-btns{display:flex;gap:8px;flex-wrap:wrap;margin-top:4px}
  .day-btn{padding:8px 16px;border:2px solid var(--border);border-radius:20px;background:#fff;cursor:pointer;font-size:.85rem;font-weight:600;color:#555;transition:all .2s}
  .day-btn.active{border-color:var(--green);background:var(--green);color:#fff}
  .prompt-box{width:100%;min-height:220px;padding:12px;border:1px solid var(--border);border-radius:8px;font-size:.82rem;font-family:monospace;line-height:1.5;resize:vertical;background:#f9f9f9;color:#212121}
  .copy-bar{display:flex;align-items:center;gap:10px;margin-top:8px}
  #copy-status{font-size:.8rem;color:var(--green);font-weight:600}
  .section-row{display:grid;grid-template-columns:1fr 1fr;gap:16px;align-items:start}
  @media(max-width:600px){.section-row{grid-template-columns:1fr}}
</style>
</head>
<body>
<header>
  <span>⚽</span>
  <h1>FirstLast · Panel de Partidos</h1>
  <div id="firebase-badge" class="disconnected">Sin Firebase</div>
</header>
<main>

  <!-- Generador de consulta Gemini -->
  <div class="card">
    <h2>🤖 Generar consulta para Gemini Studio</h2>
    <div class="section-row">
      <div>
        <label>Torneo</label>
        <select id="g-league" onchange="generatePrompt()"></select>
      </div>
      <div>
        <label>Día</label>
        <div class="day-btns" id="day-btns"></div>
      </div>
    </div>
    <div style="margin-top:16px">
      <label>Prompt generado — copia y pega en Gemini Studio</label>
      <textarea class="prompt-box" id="prompt-output" readonly></textarea>
      <div class="copy-bar">
        <button class="btn btn-green" onclick="copyPrompt()">📋 Copiar prompt</button>
        <span id="copy-status"></span>
      </div>
    </div>
  </div>

  <!-- Formulario -->
  <div class="card">
    <h2 id="form-title">Agregar Partido</h2>
    <input type="hidden" id="edit-idx" value="">
    <div class="form-grid">
      <div>
        <label>Liga (season)</label>
        <select id="f-season"></select>
      </div>
      <div>
        <label>Fecha</label>
        <input type="date" id="f-date">
      </div>
      <div>
        <label>Equipo Local</label>
        <select id="f-home"></select>
      </div>
      <div>
        <label>Posición Local</label>
        <input type="number" id="f-home-pos" min="1" max="30" placeholder="ej. 1">
      </div>
      <div>
        <label>Equipo Visitante</label>
        <select id="f-away"></select>
      </div>
      <div>
        <label>Posición Visitante</label>
        <input type="number" id="f-away-pos" min="1" max="30" placeholder="ej. 14">
      </div>
      <div>
        <label>Hora (UTC)</label>
        <input type="time" id="f-time" value="18:00">
      </div>
    </div>
    <div class="form-actions">
      <button class="btn btn-green" onclick="submitForm()">Guardar partido</button>
      <button class="btn btn-outline" id="cancel-btn" style="display:none" onclick="cancelEdit()">Cancelar</button>
    </div>
  </div>

  <!-- Lista de partidos -->
  <div class="card">
    <h2>Partidos a subir</h2>
    <div id="match-list"></div>
    <div style="margin-top:20px" class="upload-bar">
      <button class="btn btn-green" onclick="uploadAll()">⬆ Subir todo a Firestore</button>
      <span id="upload-status"></span>
    </div>
  </div>

</main>
<script>
let teams={}, leagues={}, hasFirebase=false;

async function loadMeta(){
  const r=await fetch('/api/meta');
  const d=await r.json();
  teams=d.teams; leagues=d.leagues; hasFirebase=d.firebase;
  document.getElementById('firebase-badge').textContent=hasFirebase?'Firebase ✓':'Sin Firebase';
  document.getElementById('firebase-badge').className=hasFirebase?'connected':'disconnected';
  populateSelects();
}

function populateSelects(){
  const today=new Date().toISOString().split('T')[0];
  document.getElementById('f-date').value=today;
  fillSelect('f-season', leagues, 'Selecciona liga');
  fillSelect('f-home',   teams,   'Selecciona local');
  fillSelect('f-away',   teams,   'Selecciona visitante');
}


function fillSelect(id, dict, placeholder){
  const sel=document.getElementById(id);
  sel.innerHTML=`<option value="">— ${placeholder} —</option>`;
  if(Object.keys(dict).length===0){
    // sin Firebase: input libre
    const p=sel.parentElement;
    const inp=document.createElement('input');
    inp.type='text'; inp.id=id; inp.placeholder=placeholder;
    inp.className=sel.className;
    p.replaceChild(inp,sel);
    return;
  }
  Object.entries(dict).sort((a,b)=>a[1].localeCompare(b[1])).forEach(([k,v])=>{
    const o=document.createElement('option');
    o.value=k; o.textContent=v;
    sel.appendChild(o);
  });
}

function getVal(id){return document.getElementById(id).value.trim();}

async function submitForm(){
  const idx=document.getElementById('edit-idx').value;
  const entry={
    season:          getVal('f-season'),
    homeTeam:        getVal('f-home'),
    visitingTeam:    getVal('f-away'),
    homePosition:    parseInt(getVal('f-home-pos'))||0,
    visitingPosition:parseInt(getVal('f-away-pos'))||0,
    date:            getVal('f-date'),
    time_utc:        getVal('f-time'),
  };
  if(!entry.season||!entry.homeTeam||!entry.visitingTeam||!entry.date){
    alert('Completa liga, equipos y fecha.'); return;
  }
  if(idx!==''){
    await fetch(`/api/matches/${idx}`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(entry)});
    cancelEdit();
  } else {
    await fetch('/api/matches',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(entry)});
    resetForm();
  }
  loadMatches();
}

function resetForm(){
  ['f-season','f-home','f-away'].forEach(id=>{const el=document.getElementById(id);if(el.tagName==='SELECT')el.value='';else el.value='';});
  document.getElementById('f-home-pos').value='';
  document.getElementById('f-away-pos').value='';
  document.getElementById('f-time').value='18:00';
}

function cancelEdit(){
  document.getElementById('edit-idx').value='';
  document.getElementById('form-title').textContent='Agregar Partido';
  document.getElementById('cancel-btn').style.display='none';
  resetForm();
}

function editMatch(idx, m){
  document.getElementById('edit-idx').value=idx;
  document.getElementById('form-title').textContent='Editar Partido';
  document.getElementById('cancel-btn').style.display='';
  setVal('f-season',  m.season);
  setVal('f-home',    m.homeTeam);
  setVal('f-away',    m.visitingTeam);
  document.getElementById('f-home-pos').value=m.homePosition;
  document.getElementById('f-away-pos').value=m.visitingPosition;
  document.getElementById('f-date').value=m.date;
  document.getElementById('f-time').value=m.time_utc;
  window.scrollTo({top:0,behavior:'smooth'});
}

function setVal(id,val){
  const el=document.getElementById(id);
  if(!el)return;
  el.value=val;
}

async function deleteMatch(idx){
  if(!confirm('¿Eliminar este partido?'))return;
  await fetch(`/api/matches/${idx}`,{method:'DELETE'});
  loadMatches();
}

function teamName(key){return teams[key]||key;}
function leagueName(key){return leagues[key]||key;}

async function loadMatches(){
  const r=await fetch('/api/matches');
  const matches=await r.json();
  const el=document.getElementById('match-list');
  if(matches.length===0){
    el.innerHTML='<p class="empty">📭 No hay partidos cargados. Agrega uno arriba.</p>';
    return;
  }
  let html=`<table>
    <thead><tr>
      <th>Liga</th><th>Local</th><th>Visitante</th>
      <th>Fecha</th><th>Hora UTC</th><th></th>
    </tr></thead><tbody>`;
  matches.forEach((m,i)=>{
    html+=`<tr>
      <td>${leagueName(m.season)}</td>
      <td><span class="badge badge-home">#${m.homePosition}</span> ${teamName(m.homeTeam)}</td>
      <td><span class="badge badge-away">#${m.visitingPosition}</span> ${teamName(m.visitingTeam)}</td>
      <td>${m.date}</td>
      <td>${m.time_utc}</td>
      <td style="white-space:nowrap">
        <button class="btn btn-outline btn-sm" onclick='editMatch(${i},${JSON.stringify(m)})'>✏️</button>
        <button class="btn btn-red btn-sm" onclick="deleteMatch(${i})">🗑</button>
      </td>
    </tr>`;
  });
  html+='</tbody></table>';
  el.innerHTML=html;
}

async function uploadAll(){
  const status=document.getElementById('upload-status');
  status.textContent='Subiendo...'; status.className='';
  const r=await fetch('/api/upload',{method:'POST'});
  const d=await r.json();
  if(d.error){
    status.textContent='❌ '+d.error; status.className='err'; return;
  }
  if(d.errors&&d.errors.length){
    status.textContent=`⚠️ ${d.saved}/${d.total} subidos. Errores: ${d.errors.join(' | ')}`;
    status.className='err';
  } else {
    status.textContent=`✅ ${d.saved} partido(s) subido(s) a Firestore`;
    status.className='ok';
  }
}

// ── Generador de prompt ──────────────────────────────────────────────────────

const DAY_LABELS = ['Hoy', 'Mañana', '+2 días', '+3 días'];
let selectedDay = 0;

function buildDayButtons(){
  const container = document.getElementById('day-btns');
  container.innerHTML = '';
  DAY_LABELS.forEach((label, i) => {
    const btn = document.createElement('button');
    btn.className = 'day-btn' + (i === selectedDay ? ' active' : '');
    btn.textContent = label;
    btn.onclick = () => { selectedDay = i; buildDayButtons(); generatePrompt(); };
    container.appendChild(btn);
  });
}

function getTargetDate(offset){
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return d;
}

function formatDateISO(d){
  return d.toISOString().split('T')[0];
}

function formatDateReadable(d){
  return d.toLocaleDateString('es-PE', {weekday:'long', day:'numeric', month:'long', year:'numeric'});
}

function fillLeagueSelect(){
  const sel = document.getElementById('g-league');
  sel.innerHTML = '<option value="">— Selecciona torneo —</option>';
  Object.entries(leagues).sort((a,b)=>a[1].localeCompare(b[1])).forEach(([k,v])=>{
    const o = document.createElement('option');
    o.value = k; o.textContent = v;
    sel.appendChild(o);
  });
}

function generatePrompt(){
  const leagueKey  = document.getElementById('g-league').value;
  const leagueName = leagues[leagueKey] || '';
  const target     = getTargetDate(selectedDay);
  const dateISO    = formatDateISO(target);
  const dateHuman  = formatDateReadable(target);

  if(!leagueKey){
    document.getElementById('prompt-output').value = 'Selecciona un torneo para generar el prompt.';
    return;
  }

  const jsonFormat = JSON.stringify([
    {
      season: leagueKey,
      homeTeam: "id_equipo_local",
      visitingTeam: "id_equipo_visitante",
      homePosition: 1,
      visitingPosition: 14,
      date: dateISO,
      time_utc: "HH:MM"
    }
  ], null, 2);

  const prompt =
`Eres un extractor de datos deportivos.

Liga: ${leagueName}
Fecha buscada: ${dateISO} (${dateHuman})

TAREA:
1. Busca en sofascore.com o flashscore.com la tabla de posiciones ACTUAL de ${leagueName}.
2. Identifica el TOP 5 (posiciones 1-5) y el BOTTOM 5 (últimas 5 posiciones).
3. Busca en el fixture/calendario de ${leagueName} si hay un partido programado el ${dateISO} donde un equipo del TOP 5 juegue contra uno del BOTTOM 5.

REGLAS:
- La fecha del partido en la fuente DEBE ser exactamente ${dateISO}
- Si no hay partido ese día entre TOP 5 y BOTTOM 5, devuelve lista vacía []
- El campo homeTeam y visitingTeam deben ser el nombre del equipo en minúsculas con guiones bajos (ej: "alianza_lima")
- time_utc debe estar en formato HH:MM en UTC

Devuelve SOLO este JSON, sin texto adicional:
${jsonFormat}`;

  document.getElementById('prompt-output').value = prompt;
}

function copyPrompt(){
  const ta = document.getElementById('prompt-output');
  if(!ta.value || ta.value.startsWith('Selecciona')) return;
  navigator.clipboard.writeText(ta.value).then(()=>{
    const s = document.getElementById('copy-status');
    s.textContent = '✅ Copiado';
    setTimeout(()=>{ s.textContent=''; }, 2000);
  });
}

loadMeta().then(() => {
  fillLeagueSelect();
  buildDayButtons();
  generatePrompt();
  loadMatches();
});
</script>
</body>
</html>"""

@app.route("/")
def index():
    return render_template_string(HTML)

# ── Inicio ────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    init_firebase()
    print("\n🌐 Abre http://localhost:5000 en tu navegador\n")
    app.run(debug=False, port=5000)
