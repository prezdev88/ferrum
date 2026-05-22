# Ferrum GNOME

Frontend Linux nativo para Ferrum usando `GTK4` + `libadwaita`.

## Qué hace

- Busca bandas en Metal Archives
- Muestra detalle de banda
- Muestra discografía
- Abre detalle de álbum
- Permite abrir tracks en `YouTube Music` o `YouTube`

## Cómo correrlo

Desde la raíz del repo:

```bash
./gnome/run.sh
```

## Requisitos

- `python3`
- `GTK4` + `libadwaita`
- módulos Python del sistema:
  - `gi`

## Nota sobre Metal Archives

La app GNOME consume el backend Java por HTTP. Eso significa:

- la UI es Python/GTK
- el backend Spring debe estar corriendo antes de abrir la UI
- el scraping y Cloudflare siguen en Java/Playwright

## Backend

Levanta el backend primero:

```bash
cd spring
java -jar target/ferrum-1.0.0.jar
```

Por defecto la UI busca el backend en:

```bash
http://localhost:8080
```
