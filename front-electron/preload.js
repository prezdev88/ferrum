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
    getAlbum: (albumUrl) => ipcRenderer.invoke("api-get-album", { albumUrl }),
    getSearchHistory: () => ipcRenderer.invoke("api-get-search-history")
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
