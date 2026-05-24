const path = require("node:path");
const os = require("node:os");
const fs = require("node:fs");
const { spawn } = require("node:child_process");
const { Menu, app, BrowserWindow, ipcMain, net, shell } = require("electron");

let mainWindow;
let backendProcess;
let currentBackendUrl;

function getConfigDir() {
  return path.join(os.homedir(), ".config", "ferrum");
}

function getPreferencesPath() {
  return path.join(getConfigDir(), "preferences.json");
}

function getFavoritesPath() {
  return path.join(getConfigDir(), "favorites.json");
}

function ensureConfigDir() {
  fs.mkdirSync(getConfigDir(), { recursive: true });
}

function readJsonFile(filePath, fallback) {
  try {
    return JSON.parse(fs.readFileSync(filePath, "utf-8"));
  } catch {
    return fallback;
  }
}

function writeJsonFile(filePath, value) {
  ensureConfigDir();
  fs.writeFileSync(filePath, JSON.stringify(value, null, 2), "utf-8");
}

function normalizeHexColor(value) {
  const normalized = String(value ?? "").trim().toUpperCase();
  return /^#[0-9A-F]{6}$/.test(normalized) ? normalized : null;
}

function loadSettings() {
  const payload = readJsonFile(getPreferencesPath(), {});
  const rawColors = payload && typeof payload === "object" ? payload.album_type_colors : {};
  const albumTypeColors = {};

  if (rawColors && typeof rawColors === "object") {
    for (const [albumType, color] of Object.entries(rawColors)) {
      const normalizedColor = normalizeHexColor(color);
      const normalizedType = String(albumType).trim();
      if (normalizedType && normalizedColor) {
        albumTypeColors[normalizedType] = normalizedColor;
      }
    }
  }

  return {
    themeMode: typeof payload.theme_mode === "string" ? payload.theme_mode : "black",
    musicProvider: typeof payload.music_provider === "string" ? payload.music_provider : "youtube_music",
    albumTypeColors,
    favoriteBands: loadFavorites()
  };
}

function saveSettings(settings) {
  const current = loadSettings();
  const nextSettings = {
    ...current,
    ...settings
  };

  const albumTypeColors = {};
  for (const [albumType, color] of Object.entries(nextSettings.albumTypeColors ?? {})) {
    const normalizedType = String(albumType).trim();
    const normalizedColor = normalizeHexColor(color);
    if (normalizedType && normalizedColor) {
      albumTypeColors[normalizedType] = normalizedColor;
    }
  }

  writeJsonFile(getPreferencesPath(), {
    theme_mode: nextSettings.themeMode || "black",
    music_provider: nextSettings.musicProvider || "youtube_music",
    album_type_colors: Object.fromEntries(
      Object.entries(albumTypeColors).sort((a, b) => a[0].localeCompare(b[0], undefined, { sensitivity: "base" }))
    )
  });

  if (Array.isArray(nextSettings.favoriteBands)) {
    saveFavorites(nextSettings.favoriteBands);
  }

  return loadSettings();
}

function loadFavorites() {
  const payload = readJsonFile(getFavoritesPath(), []);
  if (!Array.isArray(payload)) {
    return [];
  }

  return payload
    .filter((item) => item && typeof item === "object")
    .map((item) => ({
      name: String(item.name ?? "").trim() || "Unknown band",
      country: String(item.country ?? "").trim(),
      genre: String(item.genre ?? "").trim(),
      status: String(item.status ?? "").trim(),
      profile_url: String(item.profile_url ?? "").trim()
    }))
    .filter((item) => item.profile_url);
}

function saveFavorites(favoriteBands) {
  const normalizedFavorites = Array.isArray(favoriteBands)
    ? favoriteBands
        .filter((item) => item && typeof item === "object")
        .map((item) => ({
          name: String(item.name ?? "").trim() || "Unknown band",
          country: String(item.country ?? "").trim(),
          genre: String(item.genre ?? "").trim(),
          status: String(item.status ?? "").trim(),
          profile_url: String(item.profile_url ?? "").trim()
        }))
        .filter((item) => item.profile_url)
    : [];

  writeJsonFile(getFavoritesPath(), normalizedFavorites);
  return normalizedFavorites;
}

function resolveBackendConfig() {
  const backendPort = Number.parseInt(process.env.FERRUM_BACKEND_PORT ?? "18080", 10);
  const backendUrl =
    (process.env.FERRUM_BACKEND_URL ?? `http://localhost:${backendPort}`).replace(/\/$/, "");
  const backendLog =
    process.env.FERRUM_BACKEND_LOG ?? path.join(os.tmpdir(), "ferrum-backend.log");

  return {
    backendPort,
    backendUrl,
    backendLog
  };
}

function findBackendJar() {
  const repoRoot = path.join(__dirname, "..");
  const targetDir = path.join(repoRoot, "back", "target");
  let entries = [];
  try {
    entries = fs.readdirSync(targetDir, { withFileTypes: true });
  } catch {
    return null;
  }

  const candidates = entries
    .filter((entry) => entry.isFile())
    .map((entry) => entry.name)
    .filter((name) => /^ferrum-.*\.jar$/.test(name) && !name.endsWith(".original"))
    .sort()
    .reverse();

  if (candidates.length === 0) return null;
  return path.join(targetDir, candidates[0]);
}

function sendBackendStatus(payload) {
  if (!mainWindow) return;
  mainWindow.webContents.send("backend-status", {
    at: Date.now(),
    ...payload
  });
}

function httpGetJson(url) {
  return new Promise((resolve, reject) => {
    const request = net.request({
      method: "GET",
      url
    });

    request.on("response", (response) => {
      let body = "";
      response.on("data", (chunk) => {
        body += chunk.toString("utf-8");
      });
      response.on("end", () => {
        if (response.statusCode && response.statusCode >= 400) {
          reject(new Error(`HTTP ${response.statusCode} for ${url}`));
          return;
        }
        try {
          resolve(JSON.parse(body));
        } catch (e) {
          reject(e);
        }
      });
    });

    request.on("error", reject);
    request.end();
  });
}

async function waitForBackend(backendUrl, timeoutMs = 60_000) {
  const deadline = Date.now() + timeoutMs;
  let lastError;
  while (Date.now() < deadline) {
    try {
      // eslint-disable-next-line no-await-in-loop
      const payload = await httpGetJson(`${backendUrl}/api/health`);
      if (payload?.status === "ok") return;
    } catch (e) {
      lastError = e;
    }
    // eslint-disable-next-line no-await-in-loop
    await new Promise((r) => setTimeout(r, 350));
  }
  throw lastError ?? new Error("Backend did not become ready");
}

async function ensureBackendRunning() {
  const { backendPort, backendUrl, backendLog } = resolveBackendConfig();
  currentBackendUrl = backendUrl;

  if (process.env.FERRUM_BACKEND_URL) {
    sendBackendStatus({
      state: "external",
      message: `Using external backend at ${backendUrl}`
    });
    return { backendUrl };
  }

  const jarPath = findBackendJar();
  if (!jarPath) {
    sendBackendStatus({
      state: "error",
      message: "Backend JAR not found in back/target. Build it first (mvn -DskipTests package)."
    });
    return { backendUrl };
  }

  sendBackendStatus({
    state: "starting",
    message: `Starting backend on ${backendUrl}…`,
    jarPath,
    backendLog
  });

  const logStream = fs.createWriteStream(backendLog, { flags: "a" });
  backendProcess = spawn("java", ["-jar", jarPath, `--server.port=${backendPort}`], {
    stdio: ["ignore", "pipe", "pipe"]
  });
  backendProcess.stdout.pipe(logStream);
  backendProcess.stderr.pipe(logStream);

  backendProcess.once("exit", (code, signal) => {
    sendBackendStatus({
      state: "stopped",
      message: `Backend stopped (${signal ?? code ?? "unknown"}). See log: ${backendLog}`,
      code,
      signal
    });
    backendProcess = undefined;
  });

  try {
    await waitForBackend(backendUrl, 60_000);
    sendBackendStatus({
      state: "ready",
      message: `Backend ready at ${backendUrl}`
    });
  } catch (e) {
    sendBackendStatus({
      state: "error",
      message: `Backend did not become ready. See log: ${backendLog}`
    });
  }

  return { backendUrl };
}

function requireBackendUrl() {
  const backendUrl = currentBackendUrl;
  if (!backendUrl) {
    throw new Error("Backend URL not initialized");
  }
  return backendUrl;
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1100,
    height: 760,
    show: false,
    backgroundColor: "#0b0d10",
    webPreferences: {
      contextIsolation: true,
      preload: path.join(__dirname, "preload.js")
    }
  });

  mainWindow = win;
  win.once("ready-to-show", () => win.show());
  win.loadFile(path.join(__dirname, "index.html"));
}

function sendUiCommand(command) {
  if (!mainWindow || mainWindow.isDestroyed()) {
    return;
  }
  mainWindow.webContents.send("ui-command", { command });
}

function buildApplicationMenu() {
  Menu.setApplicationMenu(null);
}

app.whenReady().then(() => {
  buildApplicationMenu();
  createWindow();
  ensureBackendRunning().catch((e) => {
    sendBackendStatus({
      state: "error",
      message: e?.message ? `Backend error: ${e.message}` : "Backend error"
    });
  });
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

app.on("before-quit", () => {
  if (backendProcess && !backendProcess.killed) {
    backendProcess.kill();
  }
});

ipcMain.handle("get-backend-config", () => resolveBackendConfig());

ipcMain.handle("api-search", async (_event, { query, searchType }) => {
  const backendUrl = requireBackendUrl();
  const q = String(query ?? "").trim();
  const type = String(searchType ?? "BAND_NAME").trim();
  if (!q) return [];

  const url = `${backendUrl}/api/search?query=${encodeURIComponent(q)}&searchType=${encodeURIComponent(type)}`;
  return httpGetJson(url);
});

ipcMain.handle("api-get-band", async (_event, { profileUrl }) => {
  const backendUrl = requireBackendUrl();
  const url = `${backendUrl}/api/band?url=${encodeURIComponent(String(profileUrl ?? "").trim())}`;
  return httpGetJson(url);
});

ipcMain.handle("api-get-album", async (_event, { albumUrl }) => {
  const backendUrl = requireBackendUrl();
  const url = `${backendUrl}/api/album?url=${encodeURIComponent(String(albumUrl ?? "").trim())}`;
  return httpGetJson(url);
});

ipcMain.handle("api-get-search-history", async () => {
  const backendUrl = requireBackendUrl();
  try {
    return await httpGetJson(`${backendUrl}/api/search-history?limit=100`);
  } catch {
    return [];
  }
});

ipcMain.handle("settings-load", () => loadSettings());
ipcMain.handle("settings-save", (_event, settings) => saveSettings(settings ?? {}));
ipcMain.handle("favorites-save", (_event, favoriteBands) => saveFavorites(favoriteBands));
ipcMain.handle("open-external", (_event, url) => shell.openExternal(String(url ?? "")));
ipcMain.handle("app-quit", () => app.quit());
