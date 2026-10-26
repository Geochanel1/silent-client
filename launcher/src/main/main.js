const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');
const config = require('./config');
const auth = require('./auth');
const installer = require('./installer');
const game = require('./game');

let win;

function createWindow() {
  win = new BrowserWindow({
    width: 1100,
    height: 680,
    minWidth: 900,
    minHeight: 580,
    frame: false,
    backgroundColor: '#060a14',
    title: 'Silent Client',
    icon: path.join(__dirname, '..', 'renderer', 'img', 'logo.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });
  win.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));
  // εξωτερικά links ανοίγουν στον browser, όχι μέσα στον launcher
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('https://')) shell.openExternal(url);
    return { action: 'deny' };
  });
}

const send = (channel, data) => win && !win.isDestroyed() && win.webContents.send(channel, data);

// ---------------------------------------------------------------- IPC

ipcMain.handle('window:minimize', () => win.minimize());
ipcMain.handle('window:close', () => win.close());

ipcMain.handle('settings:get', () => ({ ...config.load(), resolvedGameDir: config.gameDir() }));
ipcMain.handle('settings:set', (_e, s) => config.save(s));

ipcMain.handle('auth:restore', () => auth.restore());
ipcMain.handle('auth:login', () => auth.login(win));
ipcMain.handle('auth:logout', () => auth.logout());

ipcMain.handle('versions:list', () => installer.list());

ipcMain.handle('versions:install', async (_e, id) => {
  await installer.install(id, (p) => send('progress', p));
  return installer.list();
});

ipcMain.handle('game:launch', async (_e, id) => {
  await game.launch(id, {
    onLog: (l) => send('log', l),
    onProgress: (p) => send('progress', p),
    onClose: (code) => {
      send('game:closed', code);
      if (win && !win.isVisible()) win.show();
    },
  });
  send('game:started');
  if (config.load().closeOnLaunch) win.hide();
  return true;
});

// ---------------------------------------------------------------- app

app.whenReady().then(createWindow);
app.on('window-all-closed', () => {
  if (!game.isRunning()) app.quit();
});
