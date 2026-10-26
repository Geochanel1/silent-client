// Ξεκινάει το Minecraft με το minecraft-launcher-core.
const { Client } = require('minecraft-launcher-core');
const config = require('./config');
const installer = require('./installer');
const auth = require('./auth');

let running = null;

async function launch(id, { onLog, onProgress, onClose }) {
  if (running) throw new Error('Το παιχνίδι τρέχει ήδη.');
  const v = installer.get(id);
  const state = installer.readState()[id];
  if (!v || !state) throw new Error('Κάνε πρώτα Install αυτή την έκδοση.');
  if (v.fabricMeta && !state.fabricProfile) throw new Error('Ξανακάνε Install (η έκδοση άλλαξε σε Fabric).');

  const authorization = await auth.getLaunchAuth();
  await installer.syncMod(); // πάντα η τελευταία έκδοση του mod
  const settings = config.load();

  const client = new Client();
  const opts = {
    root: config.gameDir(),
    authorization,
    javaPath: state.javaPath,
    version: { number: v.id, type: 'release', custom: state.fabricProfile || undefined },
    memory: { max: `${settings.memoryMb}M`, min: '1024M' },
    overrides: { detached: false },
  };

  client.on('debug', (l) => onLog(String(l)));
  client.on('data', (l) => onLog(String(l)));
  client.on('progress', (p) => {
    if (p && p.total) onProgress({ stage: `Προετοιμασία (${p.type})`, percent: (p.task / p.total) * 100 });
  });
  client.on('close', (code) => {
    running = null;
    onClose(code);
  });

  const proc = await client.launch(opts);
  if (!proc) {
    running = null;
    throw new Error('Το παιχνίδι δεν ξεκίνησε. Δες το log.');
  }
  running = proc;
  return true;
}

function isRunning() {
  return !!running;
}

module.exports = { launch, isRunning };
