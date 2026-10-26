const { contextBridge, ipcRenderer } = require('electron');

// Το μόνο API που βλέπει η σελίδα του launcher.
contextBridge.exposeInMainWorld('silent', {
  minimize: () => ipcRenderer.invoke('window:minimize'),
  close: () => ipcRenderer.invoke('window:close'),

  getSettings: () => ipcRenderer.invoke('settings:get'),
  setSettings: (s) => ipcRenderer.invoke('settings:set', s),

  restoreAccount: () => ipcRenderer.invoke('auth:restore'),
  login: () => ipcRenderer.invoke('auth:login'),
  logout: () => ipcRenderer.invoke('auth:logout'),

  listVersions: () => ipcRenderer.invoke('versions:list'),
  install: (id) => ipcRenderer.invoke('versions:install', id),
  launch: (id) => ipcRenderer.invoke('game:launch', id),

  on: (channel, fn) => {
    const allowed = ['progress', 'log', 'game:started', 'game:closed'];
    if (allowed.includes(channel)) ipcRenderer.on(channel, (_e, data) => fn(data));
  },
});
