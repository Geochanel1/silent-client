# Silent Client (Φάση 1)

- `mod/` : το Silent Client mod για **Minecraft 1.8.9 με Legacy Fabric**. Μενού με **Right Shift**, HUD Editor, 13 mods.
- `launcher/` : ο launcher (Electron, σκούρο/μωβ). Εκδόσεις, Install, Microsoft login, Play.
- `.github/workflows/build.yml` : φτιάχνει αυτόματα το **SilentClient-Setup.exe** στο GitHub.

---

## Πώς παίρνεις το .exe (χωρίς να εγκαταστήσεις τίποτα)

1. Φτιάξε λογαριασμό στο github.com και ένα νέο repository (π.χ. `silent-client`).
2. Πάτα **uploading an existing file** και σύρε ΟΛΑ τα περιεχόμενα του φακέλου `silent-client`,
   **μαζί με τον κρυφό φάκελο `.github`** (Εξερεύνηση → Προβολή → Εμφάνιση → Κρυφά στοιχεία).
3. **Commit changes** και μετά καρτέλα **Actions**. Περίμενε ~10 λεπτά να γίνει πράσινο ✓.
4. Στην αρχική σελίδα του repository, δεξιά, πάτα **Releases** και κατέβασε το `SilentClient-Setup-0.1.0.exe`.

Αν τα Windows δείξουν «Τα Windows προστάτευσαν τον υπολογιστή σας»: **Περισσότερες πληροφορίες → Εκτέλεση οπωσδήποτε**
(συμβαίνει επειδή η εφαρμογή δεν έχει πληρωμένη ψηφιακή υπογραφή).

## Microsoft login (μία φορά)

Για το **PLAY** χρειάζεται δική σου «εφαρμογή» στη Microsoft:

1. **portal.azure.com** → *App registrations* → *New registration*, όνομα `Silent Client`,
   account types: **Personal Microsoft accounts only**.
2. *Authentication* → *Add a platform* → **Mobile and desktop applications** → redirect URI:
   `https://login.microsoftonline.com/common/oauth2/nativeclient`
3. Αντέγραψε το **Application (client) ID**.
4. Ζήτα από τη Mojang έγκριση για Minecraft API με αυτό το ID (φόρμα «Minecraft Java Edition API» / AppID review).
5. Στον launcher: **Settings → Azure Client ID → Αποθήκευση**.

Ο launcher ανοίγει την επίσημη σελίδα της Microsoft και δεν βλέπει ποτέ τον κωδικό.

## Τι κάνει το INSTALL

Κατεβάζει Java 8, το Minecraft 1.8.9 από τη Mojang, τον **Legacy Fabric loader** και το Silent Client mod
στον φάκελο `.silentclient`.

## Για developers

- Mod: άνοιξε το `mod/` στο IntelliJ (χρειάζεται **JDK 21**), μετά `./gradlew runClient` για δοκιμή ή `./gradlew build`.
- Launcher: `cd launcher && npm install && npm start`. Βάλε το jar του mod στο `launcher/resources/mods/`.
- Το mod μπαίνει και σε κανονικό Legacy Fabric 1.8.9 (στον φάκελο `mods`).

## Φάση 1: mods

| Κατηγορία | Mods |
|---|---|
| HUD | FPS Counter, Ping, Coordinates (+biome), Direction, Clock |
| PvP | CPS Counter, Keystrokes, Armor Status, Potion Effects, Toggle Sprint, Toggle Sneak |
| Visual | Fullbright |
| Performance | Dynamic FPS |

## Επόμενες φάσεις

- **Φάση 2:** δωρεάν cosmetics (capes, wings, backpacks, shields, hats, emotes), 1.21.x με Fabric
- **Φάση 3:** minimap, waypoints, crosshair editor, hit color, combo/reach display, custom main menu
- **Φάση 4:** replay, screenshots, friends, Discord Rich Presence, auto-update, presets
