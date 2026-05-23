# Ferrum Back Endpoints

Base URL by default:

```text
http://localhost:18080
```

If you run the backend separately, adjust host/port as needed.

## Health

Checks if the backend is alive.

```bash
curl "http://localhost:18080/api/health"
```

## Search Bands

Searches Metal Archives using the given query and search type.

Supported `searchType` values:

- `BAND_NAME`
- `MUSIC_GENRE`
- `THEMES`
- `ALBUM_TITLE`
- `SONG_TITLE`
- `LABEL`
- `ARTIST`
- `USER_PROFILE`
- `GOOGLE`

Example:

```bash
curl "http://localhost:18080/api/search?query=darkthrone&searchType=BAND_NAME"
```

## Get Band Details

Fetches full band metadata and discography from a Metal Archives band URL.

Example:

```bash
curl "http://localhost:18080/api/band?url=https://www.metal-archives.com/bands/Darkthrone/146"
```

## Get Album Details

Fetches album metadata and tracklist from a Metal Archives album URL.

Example:

```bash
curl "http://localhost:18080/api/album?url=https://www.metal-archives.com/albums/Darkthrone/A_Blade_in_the_Dark/1484"
```

## Get Search History

Returns recent search entries recovered from the backend cache.

Optional query params:

- `limit`: maximum number of entries to return

Example:

```bash
curl "http://localhost:18080/api/search-history?limit=50"
```
