// Σύνδεση με Microsoft λογαριασμό -> Xbox Live -> Minecraft.
// Χρησιμοποιεί την επίσημη σελίδα σύνδεσης της Microsoft (OAuth2 + PKCE).
// Ο launcher ΔΕΝ βλέπει ποτέ τον κωδικό σου. Κρατάμε μόνο το refresh token,
// κρυπτογραφημένο με το safeStorage του λειτουργικού.

const { BrowserWindow, safeStorage, app } = require('electron');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const config = require('./config');

const AUTHORITY = 'https://login.microsoftonline.com/consumers/oauth2/v2.0';
const REDIRECT_URI = 'https://login.microsoftonline.com/common/oauth2/nativeclient';
const SCOPE = 'XboxLive.signin offline_access';

const accountFile = () => path.join(app.getPath('userData'), 'account.dat');

function clientId() {
  const id = config.load().azureClientId;
  if (!id) {
    throw new Error('Δεν έχει οριστεί Azure Client ID. Βάλ\' το στα Settings (δες README).');
  }
  return id;
}

function base64url(buf) {
  return buf.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// ---------------------------------------------------------------- Microsoft

function openLoginWindow(parent) {
  const verifier = base64url(crypto.randomBytes(32));
  const challenge = base64url(crypto.createHash('sha256').update(verifier).digest());
  const state = base64url(crypto.randomBytes(16));

  const url = `${AUTHORITY}/authorize?` + new URLSearchParams({
    client_id: clientId(),
    response_type: 'code',
    redirect_uri: REDIRECT_URI,
    scope: SCOPE,
    code_challenge: challenge,
    code_challenge_method: 'S256',
    state,
    prompt: 'select_account',
  });

  return new Promise((resolve, reject) => {
    const win = new BrowserWindow({
      parent, modal: true, width: 520, height: 680, autoHideMenuBar: true,
      title: 'Sign in with Microsoft', backgroundColor: '#ffffff',
      webPreferences: { partition: 'auth', nodeIntegration: false, contextIsolation: true },
    });
    let done = false;

    const check = (target) => {
      if (!target.startsWith(REDIRECT_URI)) return false;
      const params = new URL(target).searchParams;
      done = true;
      win.close();
      if (params.get('error')) return reject(new Error(params.get('error_description') || params.get('error')));
      if (params.get('state') !== state) return reject(new Error('Λάθος state στη σύνδεση. Δοκίμασε ξανά.'));
      resolve({ code: params.get('code'), verifier });
      return true;
    };

    win.webContents.on('will-redirect', (e, target) => { if (check(target)) e.preventDefault(); });
    win.webContents.on('will-navigate', (e, target) => { if (check(target)) e.preventDefault(); });
    win.on('closed', () => { if (!done) reject(new Error('Η σύνδεση ακυρώθηκε.')); });
    win.loadURL(url);
  });
}

async function msToken(body) {
  const res = await fetch(`${AUTHORITY}/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ client_id: clientId(), scope: SCOPE, ...body }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(`Microsoft: ${json.error_description || json.error || res.status}`);
  return json; // { access_token, refresh_token, ... }
}

// ---------------------------------------------------------------- Xbox / Minecraft

async function postJson(url, body, headers = {}) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...headers },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  let json = {};
  try { json = text ? JSON.parse(text) : {}; } catch { /* κενό */ }
  return { ok: res.ok, status: res.status, json };
}

const XSTS_ERRORS = {
  2148916233: 'Αυτός ο λογαριασμός Microsoft δεν έχει Xbox προφίλ. Μπες μία φορά στο minecraft.net για να δημιουργηθεί.',
  2148916235: 'Το Xbox Live δεν είναι διαθέσιμο στη χώρα σου.',
  2148916236: 'Ο λογαριασμός χρειάζεται επιβεβαίωση ενηλικίωσης (Νότια Κορέα).',
  2148916237: 'Ο λογαριασμός χρειάζεται επιβεβαίωση ενηλικίωσης (Νότια Κορέα).',
  2148916238: 'Ο λογαριασμός είναι παιδικός. Πρέπει να προστεθεί σε Microsoft Family από γονέα.',
};

async function minecraftLogin(msAccessToken) {
  const xbl = await postJson('https://user.auth.xboxlive.com/user/authenticate', {
    Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: `d=${msAccessToken}` },
    RelyingParty: 'http://auth.xboxlive.com',
    TokenType: 'JWT',
  });
  if (!xbl.ok) throw new Error(`Xbox Live σφάλμα (${xbl.status})`);
  const uhs = xbl.json.DisplayClaims.xui[0].uhs;

  const xsts = await postJson('https://xsts.auth.xboxlive.com/xsts/authorize', {
    Properties: { SandboxId: 'RETAIL', UserTokens: [xbl.json.Token] },
    RelyingParty: 'rp://api.minecraftservices.com/',
    TokenType: 'JWT',
  });
  if (!xsts.ok) {
    throw new Error(XSTS_ERRORS[xsts.json.XErr] || `XSTS σφάλμα (${xsts.status})`);
  }

  const mc = await postJson('https://api.minecraftservices.com/authentication/login_with_xbox', {
    identityToken: `XBL3.0 x=${uhs};${xsts.json.Token}`,
  });
  if (!mc.ok) {
    if (mc.status === 403) {
      throw new Error('Η Mojang δεν έχει εγκρίνει ακόμα το Azure app σου για Minecraft (δες README).');
    }
    throw new Error(`Minecraft login σφάλμα (${mc.status})`);
  }

  const profileRes = await fetch('https://api.minecraftservices.com/minecraft/profile', {
    headers: { Authorization: `Bearer ${mc.json.access_token}` },
  });
  if (profileRes.status === 404) throw new Error('Αυτός ο λογαριασμός δεν έχει αγοράσει Minecraft Java Edition.');
  if (!profileRes.ok) throw new Error(`Δεν βρέθηκε το προφίλ (${profileRes.status})`);
  const profile = await profileRes.json();

  return {
    name: profile.name,
    uuid: profile.id,
    accessToken: mc.json.access_token,
    expiresAt: Date.now() + (mc.json.expires_in - 60) * 1000,
  };
}

// ---------------------------------------------------------------- αποθήκευση

let session = null; // στη μνήμη: { name, uuid, accessToken, expiresAt }

function storeRefreshToken(token) {
  const data = safeStorage.isEncryptionAvailable()
    ? safeStorage.encryptString(token)
    : Buffer.from(token, 'utf8');
  fs.writeFileSync(accountFile(), data);
}

function readRefreshToken() {
  try {
    const data = fs.readFileSync(accountFile());
    return safeStorage.isEncryptionAvailable() ? safeStorage.decryptString(data) : data.toString('utf8');
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------- API

async function login(parentWindow) {
  const { code, verifier } = await openLoginWindow(parentWindow);
  const tokens = await msToken({
    grant_type: 'authorization_code', code, redirect_uri: REDIRECT_URI, code_verifier: verifier,
  });
  session = await minecraftLogin(tokens.access_token);
  storeRefreshToken(tokens.refresh_token);
  return publicInfo();
}

/** Επαναφέρει τη σύνδεση από το αποθηκευμένο refresh token (χωρίς παράθυρο). */
async function restore() {
  if (session && session.expiresAt > Date.now()) return publicInfo();
  const refresh = readRefreshToken();
  if (!refresh) return null;
  try {
    const tokens = await msToken({ grant_type: 'refresh_token', refresh_token: refresh });
    session = await minecraftLogin(tokens.access_token);
    if (tokens.refresh_token) storeRefreshToken(tokens.refresh_token);
    return publicInfo();
  } catch (e) {
    console.error('Restore failed:', e.message);
    return null;
  }
}

function logout() {
  session = null;
  try { fs.unlinkSync(accountFile()); } catch { /* δεν υπήρχε */ }
}

function publicInfo() {
  return session ? { name: session.name, uuid: session.uuid } : null;
}

/** Για το launch: δίνει το token στο minecraft-launcher-core. */
async function getLaunchAuth() {
  if (!session || session.expiresAt <= Date.now()) await restore();
  if (!session) throw new Error('Πρέπει πρώτα να συνδεθείς με τον Microsoft λογαριασμό σου.');
  return {
    access_token: session.accessToken,
    client_token: crypto.randomUUID(),
    uuid: session.uuid,
    name: session.name,
    user_properties: '{}',
    meta: { type: 'msa', demo: false },
  };
}

module.exports = { login, restore, logout, getLaunchAuth };
