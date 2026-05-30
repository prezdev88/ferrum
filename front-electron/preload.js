const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("ferrum", {
  version: "dev",
  backend: {
    getConfig: () => ipcRenderer.invoke("get-backend-config"),
    onStatus: (handler) => {
      const listener = (_event, payload) => handler(payload);
      ipcRenderer.on("backend-status", listener);
      return () => ipcRenderer.removeListener("backend-status", listener);
    }
  },
  api: {
    search: (query, searchType) => ipcRenderer.invoke("api-search", { query, searchType }),
    getBand: (profileUrl) => ipcRenderer.invoke("api-get-band", { profileUrl }),
    hasBandCache: (profileUrl) => ipcRenderer.invoke("api-has-band-cache", { profileUrl }),
    getAlbum: (albumUrl) => ipcRenderer.invoke("api-get-album", { albumUrl }),
    hasAlbumCache: (albumUrl) => ipcRenderer.invoke("api-has-album-cache", { albumUrl }),
    getSearchHistory: () => ipcRenderer.invoke("api-get-search-history"),
    clearBandCache: (profileUrl) => ipcRenderer.invoke("api-clear-band-cache", { profileUrl })
  },
  settings: {
    load: () => ipcRenderer.invoke("settings-load"),
    save: (settings) => ipcRenderer.invoke("settings-save", settings),
    saveFavorites: (favoriteBands) => ipcRenderer.invoke("favorites-save", favoriteBands)
  },
  openExternal: (url) => ipcRenderer.invoke("open-external", url),
  quit: () => ipcRenderer.invoke("app-quit"),
  ui: {
    onCommand: (handler) => {
      const listener = (_event, payload) => handler(payload);
      ipcRenderer.on("ui-command", listener);
      return () => ipcRenderer.removeListener("ui-command", listener);
    }
  }
});
