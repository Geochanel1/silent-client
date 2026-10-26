// Ρυθμίσεις του launcher. Αποθηκεύονται στο userData/settings.json
const { app } = require('electron');
const fs = require('fs');
const path = require('path');

/**
 * ΣΗΜΑΝΤΙΚΟ: Βάλε εδώ το Client ID της δικής σου εφαρμογής από το Azure
 * (δες README.md -> "Microsoft login"). Μπορεί επίσης να μπει από τα Settings του launcher.
 */
const AZURE_CLIENT_ID = '';

const defaults = {
  azureClientId: AZURE_CLIENT_ID,
  gameDir: '',
  memoryMb: 3072,
  selectedVersion: '1.8.9',
  closeOnLaunch: false,
};

function file() {
  return path.join(app.getPath('userData'), 'settings.json');
}

function load() {
  try {
    return { ...defaults, ...JSON.parse(fs.readFileSync(file(), 'utf8')) };
  } catch {
    return { ...defaults };
  }
}

function save(settings) {
  const merged = { ...load(), ...settings };
  fs.mkdirSync(path.dirname(file()), { recursive: true });
  fs.writeFileSync(file(), JSON.stringify(merged, null, 2));
  return merged;
}

function gameDir() {
  const s = load();
  return s.gameDir || path.join(app.getPath('appData'), '.silentclient');
}

module.exports = { load, save, gameDir };
