const $ = (id) => document.getElementById(id);
const api = window.silent;

let versions = [];
let selected = '1.8.9';
let account = null;
let busy = false;
let gameRunning = false;

// Τα σφάλματα από το IPC έρχονται ως "Error invoking remote method '...': Error: μήνυμα"
const cleanError = (e) => String(e && e.message ? e.message : e).replace(/^Error invoking remote method '[^']+': (Error: )?/, '');

// ---------------------------------------------------------------- navigation

function show(view) {
  document.querySelectorAll('.nav').forEach((n) => n.classList.toggle('active', n.dataset.view === view));
  document.querySelectorAll('.view').forEach((v) => v.classList.toggle('active', v.id === `view-${view}`));
}
document.querySelectorAll('.nav').forEach((n) => n.addEventListener('click', () => show(n.dataset.view)));
$('version-pill').addEventListener('click', () => show('versions'));
$('btn-min').addEventListener('click', () => api.minimize());
$('btn-close').addEventListener('click', () => api.close());

// ---------------------------------------------------------------- account

$('acc-head').addEventListener('error', () => { $('acc-head').style.display = 'none'; });
$('acc-head').addEventListener('load', () => { $('acc-head').style.display = 'block'; });

function renderAccount() {
  if (account) {
    $('acc-name').textContent = account.name;
    $('acc-head').src = `https://mc-heads.net/avatar/${account.uuid}/36`;
    $('acc-action').textContent = 'Αποσύνδεση';
  } else {
    $('acc-name').textContent = 'Δεν έχεις συνδεθεί';
    $('acc-head').removeAttribute('src');
    $('acc-head').style.display = 'none';
    $('acc-action').textContent = 'Σύνδεση με Microsoft';
  }
  renderPlay();
}

$('acc-action').addEventListener('click', async () => {
  hideError();
  if (account) {
    await api.logout();
    account = null;
    renderAccount();
    return;
  }
  try {
    $('acc-action').textContent = 'Σύνδεση...';
    account = await api.login();
  } catch (e) {
    showError(cleanError(e));
  }
  renderAccount();
});

// ---------------------------------------------------------------- versions

function current() {
  return versions.find((v) => v.id === selected);
}

function renderVersions() {
  const grid = $('version-grid');
  grid.innerHTML = '';
  for (const v of versions) {
    const el = document.createElement('div');
    el.className = `version${v.id === selected ? ' selected' : ''}${v.available ? '' : ' locked'}`;

    const head = document.createElement('div');
    head.className = 'version-head';
    const name = document.createElement('strong');
    name.textContent = v.name;
    const tag = document.createElement('span');
    tag.className = 'tag';
    tag.textContent = v.tag;
    head.append(name, tag);

    const desc = document.createElement('p');
    desc.textContent = v.description;

    const status = document.createElement('div');
    status.className = `status${v.installed ? ' ok' : ''}`;
    status.textContent = !v.available ? 'Σύντομα' : v.installed ? 'Εγκατεστημένη' : 'Χρειάζεται Install';

    const btn = document.createElement('button');
    btn.className = 'secondary';
    if (!v.available) {
      btn.textContent = 'Σύντομα';
      btn.disabled = true;
    } else {
      btn.textContent = v.id === selected ? 'Επιλεγμένη' : 'Επιλογή';
      btn.addEventListener('click', () => select(v.id));
    }

    el.append(head, desc, status, btn);
    grid.append(el);
  }
}

async function select(id) {
  selected = id;
  await api.setSettings({ selectedVersion: id });
  renderVersions();
  renderPlay();
  show('home');
}

// ---------------------------------------------------------------- play / install

function renderPlay() {
  const v = current();
  const btn = $('play-btn');
  $('pill-name').textContent = v ? v.name : '-';
  if (gameRunning) { btn.textContent = 'ΠΑΙΖΕΙΣ'; btn.disabled = true; return; }
  if (busy) { btn.disabled = true; return; }
  btn.disabled = !v || !v.available;
  btn.textContent = v && v.installed ? 'PLAY' : 'INSTALL';
}

$('play-btn').addEventListener('click', async () => {
  const v = current();
  if (!v || busy) return;
  hideError();
  busy = true;
  renderPlay();
  try {
    if (!v.installed) {
      $('play-btn').textContent = 'INSTALLING...';
      versions = await api.install(v.id);
      renderVersions();
    } else {
      if (!account) throw new Error('Συνδέσου πρώτα με τον Microsoft λογαριασμό σου (κάτω αριστερά).');
      $('play-btn').textContent = 'ΕΚΚΙΝΗΣΗ...';
      await api.launch(v.id);
    }
  } catch (e) {
    showError(cleanError(e));
  } finally {
    busy = false;
    hideProgressSoon();
    renderPlay();
  }
});

api.on('progress', (p) => {
  $('progress').classList.remove('hidden');
  $('progress-stage').textContent = p.stage;
  $('progress-pct').textContent = `${Math.round(p.percent)}%`;
  $('progress-fill').style.width = `${p.percent}%`;
});

api.on('game:started', () => { gameRunning = true; renderPlay(); });
api.on('game:closed', () => { gameRunning = false; renderPlay(); });

const logEl = $('log');
api.on('log', (line) => {
  logEl.textContent += line.endsWith('\n') ? line : `${line}\n`;
  if (logEl.textContent.length > 60000) logEl.textContent = logEl.textContent.slice(-40000);
  logEl.scrollTop = logEl.scrollHeight;
});

function hideProgressSoon() {
  setTimeout(() => { if (!busy) $('progress').classList.add('hidden'); }, 1500);
}
function showError(msg) { $('error').textContent = msg; $('error').classList.remove('hidden'); }
function hideError() { $('error').classList.add('hidden'); }

// ---------------------------------------------------------------- settings

async function loadSettings() {
  const s = await api.getSettings();
  $('ram').value = s.memoryMb;
  $('ram-value').textContent = `${(s.memoryMb / 1024).toFixed(1)} GB`;
  $('close-on-launch').checked = s.closeOnLaunch;
  $('game-dir').textContent = s.resolvedGameDir;
  $('client-id').value = s.azureClientId || '';
  selected = s.selectedVersion || '1.8.9';
}

$('ram').addEventListener('input', () => {
  $('ram-value').textContent = `${($('ram').value / 1024).toFixed(1)} GB`;
});

$('save-settings').addEventListener('click', async () => {
  await api.setSettings({
    memoryMb: Number($('ram').value),
    closeOnLaunch: $('close-on-launch').checked,
    azureClientId: $('client-id').value.trim(),
  });
  $('saved').classList.remove('hidden');
  setTimeout(() => $('saved').classList.add('hidden'), 1500);
});

// ---------------------------------------------------------------- start

(async () => {
  await loadSettings();
  versions = await api.listVersions();
  renderVersions();
  renderAccount();
  try {
    account = await api.restoreAccount();
  } catch { /* όχι συνδεδεμένος */ }
  renderAccount();
})();
