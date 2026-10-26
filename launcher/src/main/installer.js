// Εγκατάσταση εκδόσεων: Java, αρχεία Minecraft (από τους επίσημους servers της Mojang),
// Fabric (Legacy Fabric για την 1.8.9) και το Silent Client mod. Ο φάκελος έχει την ίδια δομή με το .minecraft,
// οπότε το minecraft-launcher-core βρίσκει τα αρχεία έτοιμα στο launch.

const { app } = require('electron');
const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const crypto = require('crypto');
const os = require('os');
const config = require('./config');

const VERSIONS = [
  {
    id: '1.8.9', name: '1.8.9', tag: 'PvP', available: true, java: 8,
    description: 'Η κλασική PvP έκδοση. Ιδανική για Hypixel και PvP servers.',
    // Legacy Fabric: https://legacyfabric.net
    fabricMeta: 'https://meta.legacyfabric.net/v2',
  },
  {
    id: '1.21', name: '1.21.x', tag: 'Latest', available: false, java: 21,
    description: 'Η τελευταία έκδοση με Fabric. Έρχεται στη Φάση 2.',
  },
  {
    id: '1.20.1', name: '1.20.1', tag: 'Modern', available: false, java: 17,
    description: 'Δημοφιλής modern έκδοση. Έρχεται σύντομα.',
  },
];

const MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';

// ---------------------------------------------------------------- βοηθητικά

function osName() {
  return { win32: 'windows', darwin: 'osx', linux: 'linux' }[process.platform];
}

async function exists(p) {
  try { await fsp.access(p); return true; } catch { return false; }
}

async function sha1(file) {
  const data = await fsp.readFile(file);
  return crypto.createHash('sha1').update(data).digest('hex');
}

async function download(url, dest, expectedSha1) {
  if (await exists(dest)) {
    if (!expectedSha1 || (await sha1(dest)) === expectedSha1) return;
  }
  await fsp.mkdir(path.dirname(dest), { recursive: true });
  let lastErr;
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status} για ${url}`);
      const buf = Buffer.from(await res.arrayBuffer());
      if (expectedSha1 && crypto.createHash('sha1').update(buf).digest('hex') !== expectedSha1) {
        throw new Error(`Λάθος checksum για ${path.basename(dest)}`);
      }
      await fsp.writeFile(dest, buf);
      return;
    } catch (e) {
      lastErr = e;
    }
  }
  throw lastErr;
}

async function getJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status} για ${url}`);
  return res.json();
}

/** Τρέχει εργασίες παράλληλα (μέχρι `limit` μαζί) και αναφέρει πρόοδο. */
async function pool(tasks, limit, onDone) {
  let i = 0, done = 0;
  const worker = async () => {
    while (i < tasks.length) {
      const task = tasks[i++];
      await task();
      onDone(++done, tasks.length);
    }
  };
  await Promise.all(Array.from({ length: Math.min(limit, tasks.length) }, worker));
}

function rulesAllow(rules) {
  if (!rules) return true;
  let allowed = false;
  for (const r of rules) {
    const matches = !r.os || r.os.name === osName();
    if (matches) allowed = r.action === 'allow';
  }
  return allowed;
}

function modsSourceDir() {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'mods')
    : path.join(__dirname, '..', '..', 'resources', 'mods');
}

// ---------------------------------------------------------------- Java

function javaDir(major) {
  return path.join(config.gameDir(), 'runtime', `java-${major}`);
}

async function findJavaBinary(dir) {
  const exe = process.platform === 'win32' ? 'javaw.exe' : 'java';
  const stack = [dir];
  while (stack.length) {
    const d = stack.pop();
    let entries;
    try { entries = await fsp.readdir(d, { withFileTypes: true }); } catch { continue; }
    for (const e of entries) {
      const p = path.join(d, e.name);
      if (e.isDirectory()) stack.push(p);
      else if (e.name === exe && path.basename(d) === 'bin') return p;
    }
  }
  return null;
}

async function installJava(major, progress) {
  const dir = javaDir(major);
  const existing = await findJavaBinary(dir);
  if (existing) return existing;

  const osMap = { win32: 'windows', darwin: 'mac', linux: 'linux' };
  // Η Java 8 δεν έχει έκδοση για Apple Silicon, οπότε σε Mac παίρνουμε x64 (τρέχει με Rosetta).
  const arch = process.platform === 'darwin' || os.arch() === 'x64' ? 'x64' : os.arch() === 'arm64' ? 'aarch64' : 'x64';
  const url = `https://api.adoptium.net/v3/binary/latest/${major}/ga/${osMap[process.platform]}/${arch}/jre/hotspot/normal/eclipse`;
  const ext = process.platform === 'win32' ? 'zip' : 'tar.gz';
  const archive = path.join(dir, `jre.${ext}`);

  progress({ stage: `Κατέβασμα Java ${major}...`, percent: 0 });
  await download(url, archive);
  progress({ stage: `Αποσυμπίεση Java ${major}...`, percent: 50 });
  if (ext === 'zip') {
    const extract = require('extract-zip');
    await extract(archive, { dir: dir });
  } else {
    const tar = require('tar');
    await tar.x({ file: archive, cwd: dir });
  }
  await fsp.unlink(archive);

  const bin = await findJavaBinary(dir);
  if (!bin) throw new Error('Δεν βρέθηκε η Java μετά την εγκατάσταση.');
  if (process.platform !== 'win32') await fsp.chmod(bin, 0o755);
  return bin;
}

// ---------------------------------------------------------------- Minecraft

async function installVanilla(id, progress) {
  const root = config.gameDir();
  progress({ stage: 'Λήψη πληροφοριών έκδοσης...', percent: 0 });
  const manifest = await getJson(MANIFEST);
  const entry = manifest.versions.find((v) => v.id === id);
  if (!entry) throw new Error(`Η έκδοση ${id} δεν βρέθηκε.`);

  const versionDir = path.join(root, 'versions', id);
  const versionJsonPath = path.join(versionDir, `${id}.json`);
  await download(entry.url, versionJsonPath, entry.sha1);
  const version = JSON.parse(await fsp.readFile(versionJsonPath, 'utf8'));

  // client jar
  progress({ stage: 'Κατέβασμα Minecraft...', percent: 2 });
  await download(version.downloads.client.url, path.join(versionDir, `${id}.jar`), version.downloads.client.sha1);

  // libraries (+ natives)
  const libTasks = [];
  for (const lib of version.libraries) {
    if (!rulesAllow(lib.rules) || !lib.downloads) continue;
    const art = lib.downloads.artifact;
    if (art) libTasks.push(() => download(art.url, path.join(root, 'libraries', art.path), art.sha1));
    if (lib.natives && lib.natives[osName()] && lib.downloads.classifiers) {
      const key = lib.natives[osName()].replace('${arch}', os.arch() === 'x64' ? '64' : '32');
      const nat = lib.downloads.classifiers[key];
      if (nat) libTasks.push(() => download(nat.url, path.join(root, 'libraries', nat.path), nat.sha1));
    }
  }
  await pool(libTasks, 8, (d, t) => progress({ stage: `Βιβλιοθήκες (${d}/${t})`, percent: 5 + (d / t) * 20 }));

  // assets
  const idx = version.assetIndex;
  const indexPath = path.join(root, 'assets', 'indexes', `${idx.id}.json`);
  await download(idx.url, indexPath, idx.sha1);
  const objects = JSON.parse(await fsp.readFile(indexPath, 'utf8')).objects;
  const assetTasks = Object.values(objects).map(({ hash }) => () => {
    const sub = hash.substring(0, 2);
    return download(
      `https://resources.download.minecraft.net/${sub}/${hash}`,
      path.join(root, 'assets', 'objects', sub, hash),
      hash,
    );
  });
  await pool(assetTasks, 16, (d, t) => progress({ stage: `Ήχοι & textures (${d}/${t})`, percent: 25 + (d / t) * 60 }));
}

/** Μετατρέπει "group:artifact:version" σε διαδρομή maven. */
function mavenPath(name) {
  const [group, artifact, version] = name.split(':');
  return `${group.replace(/\./g, '/')}/${artifact}/${version}/${artifact}-${version}.jar`;
}

/**
 * Κατεβάζει το Fabric loader: το profile json (μπαίνει στο versions/<id>/<id>.json)
 * και τις βιβλιοθήκες του. Επιστρέφει το id του profile για το launch.
 */
async function installFabric(v, progress) {
  progress({ stage: 'Κατέβασμα Fabric...', percent: 87 });
  const loaders = await getJson(`${v.fabricMeta}/versions/loader/${v.id}`);
  const stable = loaders.find((l) => l.loader.stable) || loaders[0];
  if (!stable) throw new Error('Δεν βρέθηκε Fabric loader για αυτή την έκδοση.');
  const loaderVersion = stable.loader.version;

  const profile = await getJson(`${v.fabricMeta}/versions/loader/${v.id}/${loaderVersion}/profile/json`);
  const root = config.gameDir();
  const dir = path.join(root, 'versions', profile.id);
  await fsp.mkdir(dir, { recursive: true });
  await fsp.writeFile(path.join(dir, `${profile.id}.json`), JSON.stringify(profile, null, 2));

  const tasks = (profile.libraries || [])
    .filter((lib) => lib.name && lib.url)
    .map((lib) => () => {
      const rel = mavenPath(lib.name);
      const base = lib.url.endsWith('/') ? lib.url : `${lib.url}/`;
      return download(base + rel, path.join(root, 'libraries', rel), lib.sha1);
    });
  await pool(tasks, 6, (d, t) => progress({ stage: `Fabric (${d}/${t})`, percent: 87 + (d / t) * 8 }));
  return profile.id;
}

/** Αντιγράφει το Silent Client mod (το .jar από το resources/mods) στο mods του παιχνιδιού. */
async function syncMod() {
  const src = modsSourceDir();
  const dest = path.join(config.gameDir(), 'mods');
  await fsp.mkdir(dest, { recursive: true });
  let files = [];
  try { files = (await fsp.readdir(src)).filter((f) => f.endsWith('.jar')); } catch { /* κανένα */ }
  if (!files.length) {
    throw new Error('Δεν βρέθηκε το silentclient .jar στο launcher/resources/mods. Κάνε build το mod πρώτα (δες README).');
  }
  // σβήνουμε παλιές εκδόσεις του Silent mod
  for (const f of await fsp.readdir(dest)) {
    if (f.startsWith('silentclient') && f.endsWith('.jar')) await fsp.unlink(path.join(dest, f));
  }
  for (const f of files) await fsp.copyFile(path.join(src, f), path.join(dest, f));
}

// ---------------------------------------------------------------- state

const stateFile = () => path.join(config.gameDir(), 'silent-installed.json');

function readState() {
  try { return JSON.parse(fs.readFileSync(stateFile(), 'utf8')); } catch { return {}; }
}

function list() {
  const state = readState();
  return VERSIONS.map((v) => ({ ...v, installed: !!state[v.id] }));
}

function get(id) {
  return VERSIONS.find((v) => v.id === id);
}

async function install(id, progress) {
  const v = get(id);
  if (!v || !v.available) throw new Error('Αυτή η έκδοση δεν είναι ακόμα διαθέσιμη.');
  await fsp.mkdir(config.gameDir(), { recursive: true });

  const javaPath = await installJava(v.java, progress);
  await installVanilla(v.id, progress);
  const fabricProfile = v.fabricMeta ? await installFabric(v, progress) : null;
  progress({ stage: 'Εγκατάσταση Silent Client...', percent: 96 });
  await syncMod();

  const state = readState();
  state[v.id] = { installedAt: new Date().toISOString(), javaPath, fabricProfile };
  await fsp.writeFile(stateFile(), JSON.stringify(state, null, 2));
  progress({ stage: 'Έτοιμο!', percent: 100 });
}

module.exports = { list, get, install, readState, syncMod };
