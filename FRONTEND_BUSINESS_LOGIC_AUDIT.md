# Frontend Business Logic Audit

## Features that should move from frontend to backend

| Feature actual en front | Dónde está | Por qué no debería vivir en frontend | Qué debería hacer backend |
|---|---|---|---|
| Historial local en memoria con deduplicación, normalización y límite | `front-electron/renderer.js` (`rememberSearchHistoryEntry`, `submitSearch`) | Reimplementa reglas del historial aunque el backend ya tiene ese caso de uso. Genera dos fuentes de verdad. | Registrar búsquedas y devolver historial ya deduplicado, ordenado y limitado. |
| Ordenamiento del historial alfabético en la UI | `front-electron/renderer.js` (`renderSearchHistoryModal`) | Cambia semántica funcional del historial. El backend lo expone por recencia. | Entregar historial ya ordenado según la regla oficial. |
| Alta/baja de favoritos como colección persistida | `front-electron/renderer.js` (`toggleFavorite`), `front-electron/main.js` (`saveFavorites`) | Favoritos no es presentación; es dato funcional del usuario. Hoy queda fuera del modelo backend. | Exponer `add/remove/list favorites` y persistirlos en backend. |
| Normalización de favoritos antes de guardar | `front-electron/main.js` (`saveFavorites`) | El frontend está definiendo forma canónica de una entidad funcional. Eso debería centralizarse. | Validar y normalizar favoritos al persistirlos. |
| Enriquecimiento automático de favoritos con `image_url` | `front-electron/renderer.js` (`hydrateFavoriteBandArtwork`, `persistFavoriteBandArtwork`) | Es composición de datos derivada desde otra fuente, no mero render. Además dispara lecturas y escrituras funcionales. | Devolver favoritos ya enriquecidos o resolver artwork en un caso de uso dedicado. |
| Cache local de detalles de álbum en el renderer | `front-electron/renderer.js` (`fetchAlbumDetail`) | El frontend está haciendo una política de caching funcional para datos de negocio. Duplica responsabilidades con el backend, que ya maneja cache. | Mantener cache y reuso de detalles en backend; el front solo consume. |
| Prefetch secuencial de portadas de álbum | `front-electron/renderer.js` (`startAlbumCoverPrefetch`) | Decide qué datos faltan, cómo priorizarlos y cuándo pedirlos. Eso es orquestación de aplicación. | Exponer discografía ya enriquecida o un endpoint de hidratación de discografía. |
| Espera fija de 2 segundos entre requests de álbum | `front-electron/renderer.js` (`startAlbumCoverPrefetch`) | Es una política operativa. Si existe throttling, rate limit o scraping hygiene, debe estar centralizado. | Aplicar throttling, retry o rate limiting en backend. |
| Sincronización de artwork del álbum hacia la discografía seleccionada | `front-electron/renderer.js` (`syncAlbumArtworkFromDetail`) | La UI está mutando el modelo funcional a partir de datos derivados. | Entregar la discografía consistente desde backend. |
| Regla por defecto "si existe Full-length, selecciónalo" | `front-electron/renderer.js` (`renderBandDetail`) | Es una decisión funcional del producto sobre cómo interpretar discografía, no solo visual. | Devolver filtro recomendado o criterio por defecto. |
| Construcción de filtros de discografía por tipo | `front-electron/renderer.js` (`resolveDiscographyFilters`) | Si esos tipos son parte del modelo de releases, su taxonomía no debería inferirse en cada cliente. | Exponer tipos normalizados y filtros disponibles. |
| Normalización de modelos API en renderer (`band`, `album`, `history`, `summary`) | `front-electron/renderer.js` (`normalizeBandSummary`, `normalizeAlbumEntry`, `normalizeBandDetail`, `normalizeAlbumDetail`, `normalizeSearchHistoryEntry`) | El frontend compensa shapes alternativos (`snake_case` y `camelCase`) y valores faltantes. Eso indica contrato débil. | Entregar DTOs consistentes y canónicos desde backend. |
| Validación y saneamiento de `favoriteBands` en settings | `front-electron/main.js` (`loadFavorites`, `saveFavorites`) | Mezcla preferencias visuales con datos funcionales del usuario. | Separar preferencias UI de datos funcionales; favoritos al backend, settings visuales local si quieres. |
| Detección de "hay o no hay cache" para decidir loading UX | `front-electron/renderer.js` (`onBandSelected`) | La UI depende de conocimiento operativo del cache. No es grave, pero acopla presentación a estrategia interna. | Devolver metadata de frescura o cache junto con la respuesta, o abstraerlo por completo. |
| Composición de URL de búsqueda en proveedor musical | `front-electron/renderer.js` (`buildProviderUrl`) | Si buscar o reproducir álbumes es feature funcional del producto, la resolución del proveedor debería ser centralizada y testeable. | Resolver enlaces externos desde backend o una capa de aplicación compartida. |

## Features that should stay in frontend

- Tema visual.
- Modo claro, oscuro o black.
- Tamaño de grilla.
- Opacidad visual.
- Apertura y cierre de modales.
- Overlay de loading.
- Render HTML y CSS.
- Colores por tipo de álbum, pero solo si siguen siendo personalización visual local.

## Recommended split

- Backend: historial, favoritos, enriquecimiento de favoritos, hidratación de discografía y álbumes, reglas por defecto de releases, contratos canónicos.
- Frontend: render, interacción, estado efímero de pantalla y preferencias visuales.

## Prioritized refactor plan

| Prioridad | Feature a mover | Impacto | Riesgo actual | Use case o endpoint sugerido |
|---|---|---|---|---|
| Alta | Historial local en memoria con deduplicación, normalización, límite y orden | Alto | Dos fuentes de verdad y comportamiento inconsistente entre sesiones y clientes | `RecordSearchUseCase`, `GetSearchHistoryUseCase`, `POST /api/search-history`, `GET /api/search-history` |
| Alta | Alta y baja de favoritos como colección persistida | Alto | Favoritos fuera del modelo backend, difícil reutilización y validación inconsistente | `AddFavoriteBandUseCase`, `RemoveFavoriteBandUseCase`, `ListFavoriteBandsUseCase`, `POST/DELETE/GET /api/favorites` |
| Alta | Normalización y validación de favoritos | Alto | La forma canónica de favoritos depende de Electron | Integrarlo en `AddFavoriteBandUseCase` y `ListFavoriteBandsUseCase` |
| Alta | Enriquecimiento automático de favoritos con `image_url` | Alto | La UI hace composición funcional y persiste datos derivados | `HydrateFavoriteBandsUseCase` o `GET /api/favorites?includeArtwork=true` |
| Alta | Normalización de DTOs API en renderer | Alto | Contrato débil, clientes obligados a compensar inconsistencias | DTOs únicos desde backend, solo `snake_case` o solo `camelCase` |
| Media | Cache local de detalles de álbum en renderer | Medio/alto | Duplica estrategia de cache y complica consistencia del cliente | Reforzar `GetAlbumDetailsUseCase`; opcionalmente incluir metadata de cache en respuesta |
| Media | Prefetch secuencial de portadas de álbum | Medio/alto | Orquestación funcional en UI, muchas requests desde cliente | `HydrateBandDiscographyUseCase` o `GET /api/band?url=...&includeAlbumArtwork=true` |
| Media | Espera fija de 2 segundos entre requests | Medio | Política operativa dispersa y difícil de mantener | Throttling interno en gateway o servicio backend |
| Media | Sincronización de artwork del álbum hacia discografía | Medio | La UI muta el modelo de releases con datos derivados | Devolver discografía ya consistente desde `GetBandDetailsUseCase` |
| Media | Regla por defecto "si existe Full-length, selecciónalo" | Medio | Regla funcional embebida en render, difícil de reutilizar | Incluir `default_discography_filter` en respuesta de banda |
| Media | Construcción de filtros de discografía por tipo | Medio | Taxonomía inferida en cliente | Incluir `available_discography_filters` normalizados en respuesta |
| Baja/Media | Detección de cache para decidir loading UX | Bajo/medio | Acopla UX a detalles internos de cache | Responder con metadata `cached`, `fresh` o `source` en `GET /api/band` |
| Baja/Media | Composición de URL de proveedor musical | Bajo/medio | Regla funcional pequeña pero dispersa | `BuildPlaybackLinkUseCase` o `GET /api/playback-link?...` |
| Baja | Validación y saneamiento de `favoriteBands` dentro de settings | Bajo | Mezcla configuración visual con datos funcionales | Separar favoritos de `preferences.json`; dejar solo settings visuales en frontend |

## Suggested execution order

1. Unificar contratos API y eliminar normalización defensiva en el renderer.
2. Mover favoritos al backend como agregado propio con endpoints CRUD.
3. Mover el historial completamente al backend y eliminar la copia local del renderer.
4. Hacer que `getBand` devuelva más contexto listo para UI: filtros disponibles, filtro default y opcionalmente artwork de discografía.
5. Eliminar cache funcional y prefetch de álbumes en frontend, dejando solo estado visual de carga.
6. Separar definitivamente preferencias visuales de datos funcionales del usuario.

## Minimal target architecture

- `frontend`: render, eventos, navegación local, estado efímero de pantalla.
- `electron main`: bridge IPC, lifecycle de app, persistencia solo de preferencias visuales locales si aplica.
- `backend application`: historial, favoritos, enriquecimiento, reglas de releases, contratos y políticas de acceso.
- `backend infrastructure`: cache, scraping, throttling, composición con fuentes externas.
