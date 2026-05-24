# Ferrum Electron (WIP)

This folder is the new cross-platform desktop frontend target (Windows/macOS/Linux).

## Run (dev)

```bash
cd front-electron
npm start
```

This project expects an `electron` binary available in your `PATH`.

On Arch Linux:

```bash
sudo pacman -S electron
```

## Status

- Implemented: app boots and shows a startup screen
- Implemented: backend start/stop + `/api/health` status
- Next: search flow (`/api/search`) + band details
