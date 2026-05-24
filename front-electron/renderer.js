const SEARCH_TYPE_LABELS = {
  BAND_NAME: "Band name",
  MUSIC_GENRE: "Music genre",
  THEMES: "Themes",
  ALBUM_TITLE: "Album title",
  SONG_TITLE: "Song title",
  LABEL: "Label",
  ARTIST: "Artist",
  USER_PROFILE: "User profile",
  GOOGLE: "Google"
};

const THEME_MODES = ["system", "light", "dark", "black"];

const elements = {
  appShell: document.getElementById("appShell"),
  loadingOverlay: document.getElementById("loadingOverlay"),
  loadingBar: document.getElementById("loadingBar"),
  statusLine: document.getElementById("statusLine"),
  appVersion: document.getElementById("appVersion"),
  backendState: document.getElementById("backendState"),
  backendUrl: document.getElementById("backendUrl"),
  searchForm: document.getElementById("searchForm"),
  queryInput: document.getElementById("query"),
  searchTypeSelect: document.getElementById("searchType"),
  menuButton: document.getElementById("menuButton"),
  appMenu: document.getElementById("appMenu"),
  menuSettingsButton: document.getElementById("menuSettingsButton"),
  menuHistoryButton: document.getElementById("menuHistoryButton"),
  menuQuitButton: document.getElementById("menuQuitButton"),
  searchButton: document.getElementById("searchButton"),
  resultsHeading: document.getElementById("resultsHeading"),
  resultsCount: document.getElementById("resultsCount"),
  resultsList: document.getElementById("resultsList"),
  resultsEmpty: document.getElementById("resultsEmpty"),
  resultsEmptyTitle: document.getElementById("resultsEmptyTitle"),
  resultsEmptyDescription: document.getElementById("resultsEmptyDescription"),
  favoritesToggle: document.getElementById("favoritesToggle"),
  detailEmpty: document.getElementById("detailEmpty"),
  detailContent: document.getElementById("detailContent"),
  historyModal: document.getElementById("historyModal"),
  historyList: document.getElementById("historyList"),
  historyCloseButton: document.getElementById("historyCloseButton"),
  albumModal: document.getElementById("albumModal"),
  albumModalTitle: document.getElementById("albumModalTitle"),
  albumModalSubtitle: document.getElementById("albumModalSubtitle"),
  albumModalBody: document.getElementById("albumModalBody"),
  albumCloseButton: document.getElementById("albumCloseButton"),
  settingsModal: document.getElementById("settingsModal"),
  settingsCloseButton: document.getElementById("settingsCloseButton"),
  themeSelect: document.getElementById("themeSelect"),
  providerSelect: document.getElementById("providerSelect"),
  settingsColorList: document.getElementById("settingsColorList"),
  taskLoadingOverlay: document.getElementById("taskLoadingOverlay"),
  taskLoadingTitle: document.getElementById("taskLoadingTitle"),
  taskLoadingMessage: document.getElementById("taskLoadingMessage")
};

const state = {
  backendReady: false,
  historyLoaded: false,
  resultsMode: "search",
  isSearching: false,
  isLoadingBand: false,
  results: [],
  selectedBandSummary: null,
  selectedBandDetail: null,
  selectedBandFilter: null,
  searchHistory: [],
  settings: {
    themeMode: "black",
    musicProvider: "youtube_music",
    albumTypeColors: {},
    favoriteBands: []
  }
};

elements.appVersion.textContent = `electron · ${globalThis.ferrum?.version ?? "unknown"}`;

function normalizeBandSummary(item) {
  return {
    name: String(item?.name ?? "").trim(),
    country: String(item?.country ?? "").trim(),
    genre: String(item?.genre ?? "").trim(),
    status: String(item?.status ?? "").trim(),
    profile_url: String(item?.profile_url ?? item?.profileUrl ?? "").trim()
  };
}

function normalizeAlbumEntry(item) {
  return {
    title: String(item?.title ?? "").trim(),
    type: String(item?.type ?? "").trim(),
    year: String(item?.year ?? "").trim(),
    url: String(item?.url ?? "").trim(),
    image_url: String(item?.image_url ?? item?.imageUrl ?? "").trim()
  };
}

function normalizeBandDetail(item) {
  return {
    name: String(item?.name ?? "").trim(),
    image_url: String(item?.image_url ?? item?.imageUrl ?? "").trim(),
    country: String(item?.country ?? "").trim(),
    location: String(item?.location ?? "").trim(),
    status: String(item?.status ?? "").trim(),
    formed_in: String(item?.formed_in ?? item?.formedIn ?? "").trim(),
    years_active: String(item?.years_active ?? item?.yearsActive ?? "").trim(),
    genre: String(item?.genre ?? "").trim(),
    lyrical_themes: String(item?.lyrical_themes ?? item?.lyricalThemes ?? "").trim(),
    label: String(item?.label ?? "").trim(),
    profile_url: String(item?.profile_url ?? item?.profileUrl ?? "").trim(),
    discography: Array.isArray(item?.discography) ? item.discography.map(normalizeAlbumEntry) : []
  };
}

function normalizeAlbumDetail(item) {
  return {
    title: String(item?.title ?? "").trim(),
    image_url: String(item?.image_url ?? item?.imageUrl ?? "").trim(),
    type: String(item?.type ?? "").trim(),
    release_date: String(item?.release_date ?? item?.releaseDate ?? "").trim(),
    label: String(item?.label ?? "").trim(),
    url: String(item?.url ?? "").trim(),
    tracks: Array.isArray(item?.tracks)
      ? item.tracks.map((track) => ({
          number: String(track?.number ?? "").trim(),
          title: String(track?.title ?? "").trim(),
          duration: String(track?.duration ?? "").trim()
        }))
      : []
  };
}

function normalizeSearchHistoryEntry(item) {
  return {
    query: String(item?.query ?? "").trim(),
    search_type: String(item?.search_type ?? item?.searchType ?? "").trim()
  };
}

function normalizeHexColor(value) {
  const normalized = String(value ?? "").trim().toUpperCase();
  return /^#[0-9A-F]{6}$/.test(normalized) ? normalized : null;
}

function generateRandomColor() {
  const hue = Math.random();
  const saturation = 0.55 + Math.random() * 0.27;
  const value = 0.72 + Math.random() * 0.2;

  const chroma = value * saturation;
  const huePrime = hue * 6;
  const x = chroma * (1 - Math.abs((huePrime % 2) - 1));
  let red = 0;
  let green = 0;
  let blue = 0;

  if (huePrime < 1) {
    red = chroma;
    green = x;
  } else if (huePrime < 2) {
    red = x;
    green = chroma;
  } else if (huePrime < 3) {
    green = chroma;
    blue = x;
  } else if (huePrime < 4) {
    green = x;
    blue = chroma;
  } else if (huePrime < 5) {
    red = x;
    blue = chroma;
  } else {
    red = chroma;
    blue = x;
  }

  const match = value - chroma;
  return `#${[red, green, blue]
    .map((channel) => Math.round((channel + match) * 255).toString(16).padStart(2, "0"))
    .join("")
    .toUpperCase()}`;
}

function hexToRgb(color) {
  const normalized = String(color).replace("#", "");
  return {
    red: Number.parseInt(normalized.slice(0, 2), 16),
    green: Number.parseInt(normalized.slice(2, 4), 16),
    blue: Number.parseInt(normalized.slice(4, 6), 16)
  };
}

function rgba(color, alpha) {
  const { red, green, blue } = hexToRgb(color);
  return `rgba(${red}, ${green}, ${blue}, ${alpha.toFixed(2)})`;
}

function mixColor(color, targetColor, ratio) {
  const source = hexToRgb(color);
  const target = hexToRgb(targetColor);
  const blend = (sourceChannel, targetChannel) =>
    Math.round(sourceChannel * (1 - ratio) + targetChannel * ratio)
      .toString(16)
      .padStart(2, "0")
      .toUpperCase();

  return `#${blend(source.red, target.red)}${blend(source.green, target.green)}${blend(source.blue, target.blue)}`;
}

function resolveAlbumTypeName(albumType) {
  return String(albumType ?? "").trim() || "Other";
}

function resolveEffectiveThemeMode(themeMode) {
  if (themeMode !== "system") {
    return themeMode;
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function applyTheme() {
  const effectiveTheme = resolveEffectiveThemeMode(state.settings.themeMode);
  document.body.classList.remove("ferrum-light", "ferrum-dark", "ferrum-black");
  document.body.classList.add(
    effectiveTheme === "black" ? "ferrum-black" : effectiveTheme === "dark" ? "ferrum-dark" : "ferrum-light"
  );
}

function getAlbumTypeBadgeStyle(albumType) {
  const resolvedAlbumType = resolveAlbumTypeName(albumType);
  const color = ensureAlbumTypeColor(resolvedAlbumType, { persist: false });
  const effectiveTheme = resolveEffectiveThemeMode(state.settings.themeMode);

  if (effectiveTheme === "light") {
    return {
      background: rgba(color, 0.16),
      color: mixColor(color, "#000000", 0.38),
      borderColor: rgba(color, 0.28)
    };
  }

  if (effectiveTheme === "dark") {
    return {
      background: rgba(color, 0.22),
      color: mixColor(color, "#FFFFFF", 0.58),
      borderColor: rgba(color, 0.34)
    };
  }

  return {
    background: rgba(color, 0.2),
    color: mixColor(color, "#FFFFFF", 0.64),
    borderColor: rgba(color, 0.32)
  };
}

function applyAlbumTypeBadgeStyle(node, albumType) {
  const style = getAlbumTypeBadgeStyle(albumType);
  node.style.background = style.background;
  node.style.color = style.color;
  node.style.borderColor = style.borderColor;
}

function persistSettings() {
  globalThis.ferrum.settings.save({
    themeMode: state.settings.themeMode,
    musicProvider: state.settings.musicProvider,
    albumTypeColors: state.settings.albumTypeColors,
    favoriteBands: state.settings.favoriteBands
  });
}

function ensureAlbumTypeColor(albumType, options = { persist: true }) {
  const resolvedAlbumType = resolveAlbumTypeName(albumType);
  for (const [currentAlbumType, color] of Object.entries(state.settings.albumTypeColors)) {
    if (currentAlbumType.toLowerCase() === resolvedAlbumType.toLowerCase()) {
      return color;
    }
  }

  const generatedColor = generateRandomColor();
  state.settings.albumTypeColors[resolvedAlbumType] = generatedColor;
  if (options.persist) {
    persistSettings();
    renderSettingsColorRows();
  }
  return generatedColor;
}

function refreshAlbumTypeBadges() {
  for (const badge of document.querySelectorAll("[data-album-type-badge]")) {
    applyAlbumTypeBadgeStyle(badge, badge.dataset.albumTypeBadge || "");
  }
}

function setText(node, value) {
  node.textContent = value;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function isFavoriteBand(profileUrl) {
  return state.settings.favoriteBands.some((item) => item.profile_url === profileUrl);
}

function rememberSearchHistoryEntry(query, searchType) {
  const normalizedQuery = String(query ?? "").trim().toLowerCase();
  if (!normalizedQuery) {
    return;
  }

  const nextHistory = state.searchHistory.filter(
    (entry) => !(entry.query.toLowerCase() === normalizedQuery && entry.search_type === searchType)
  );
  nextHistory.unshift({ query: normalizedQuery, search_type: searchType });
  state.searchHistory = nextHistory.slice(0, 100);
}

function setBackendReady(isReady) {
  const wasReady = state.backendReady;
  state.backendReady = isReady;
  elements.searchButton.disabled = !isReady || !elements.queryInput.value.trim() || state.isSearching;

  if (isReady) {
    elements.loadingOverlay.classList.add("hidden");
    elements.appShell.classList.remove("hidden");
    if (!wasReady && !state.historyLoaded) {
      loadSearchHistory();
    }
    elements.queryInput.focus();
    return;
  }

  elements.appShell.classList.add("hidden");
  elements.loadingOverlay.classList.remove("hidden");
}

async function loadSearchHistory() {
  try {
    const history = await globalThis.ferrum.api.getSearchHistory();
    state.searchHistory = Array.isArray(history) ? history.map(normalizeSearchHistoryEntry).filter((entry) => entry.query) : [];
    state.historyLoaded = true;
  } catch {
    state.searchHistory = [];
  }
}

function setSearching(isSearching) {
  state.isSearching = isSearching;
  elements.queryInput.disabled = isSearching;
  elements.searchTypeSelect.disabled = isSearching;
  elements.resultsList.style.pointerEvents = isSearching ? "none" : "";
  elements.favoritesToggle.disabled = isSearching;
  elements.searchButton.disabled = !state.backendReady || isSearching || !elements.queryInput.value.trim();
  elements.searchButton.textContent = isSearching ? "Searching..." : "Search";
}

function setLoadingBand(isLoadingBand, bandName = "") {
  state.isLoadingBand = isLoadingBand;
  if (isLoadingBand) {
    presentTaskLoading("Loading band", `Fetching details for ${bandName || "selected band"}...`);
    elements.resultsList.style.pointerEvents = "none";
    showDetailPlaceholder("Loading band", `Fetching details for ${bandName || "selected band"}...`);
    return;
  }
  elements.resultsList.style.pointerEvents = "";
}

function presentTaskLoading(title, message) {
  elements.taskLoadingTitle.textContent = title;
  elements.taskLoadingMessage.textContent = message;
  elements.taskLoadingOverlay.classList.remove("hidden");
}

function closeTaskLoading() {
  elements.taskLoadingOverlay.classList.add("hidden");
}

function showResultsEmpty(title, description) {
  setText(elements.resultsEmptyTitle, title);
  setText(elements.resultsEmptyDescription, description);
  elements.resultsEmpty.classList.remove("hidden");
  elements.resultsList.classList.add("hidden");
}

function showDetailPlaceholder(title, description) {
  elements.detailEmpty.querySelector(".statusTitle").textContent = title;
  elements.detailEmpty.querySelector(".statusDescription").textContent = description;
  elements.detailEmpty.classList.remove("hidden");
  elements.detailContent.classList.add("hidden");
}

function showDetailContent() {
  elements.detailEmpty.classList.add("hidden");
  elements.detailContent.classList.remove("hidden");
}

function syncResultsHeader() {
  if (state.resultsMode === "favorites") {
    setText(elements.resultsHeading, "Favorites");
    elements.favoritesToggle.textContent = "Search results";
    elements.favoritesToggle.classList.add("active");
    return;
  }

  setText(elements.resultsHeading, "Results");
  elements.favoritesToggle.textContent = "Favorites";
  elements.favoritesToggle.classList.remove("active");
}

function syncResultsCount() {
  if (state.resultsMode === "favorites") {
    setText(elements.resultsCount, `${state.settings.favoriteBands.length} favorites`);
    return;
  }

  if (state.isSearching) {
    setText(elements.resultsCount, "Searching...");
    return;
  }

  if (state.results.length === 0) {
    setText(elements.resultsCount, "No results yet");
    return;
  }

  setText(elements.resultsCount, `${state.results.length} matches`);
}

function buildBandSummaryMeta(item) {
  return [item.country, item.genre, item.status].filter(Boolean).join("  •  ") || "—";
}

function getVisibleResults() {
  if (state.resultsMode === "favorites") {
    return [...state.settings.favoriteBands]
      .map(normalizeBandSummary)
      .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: "base" }));
  }
  return state.results;
}

function renderResultsList() {
  const items = getVisibleResults();
  elements.resultsList.replaceChildren();
  syncResultsHeader();
  syncResultsCount();

  if (items.length === 0) {
    if (state.resultsMode === "favorites") {
      showResultsEmpty("No favorite bands yet", "Star a band from the detail panel to keep it here.");
    } else {
      showResultsEmpty("Start with a band, genre or theme", "Search results will appear here and load band details on selection.");
    }
    return;
  }

  for (const band of items) {
    const item = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "resultRow";
    if (state.selectedBandSummary?.profile_url === band.profile_url) {
      button.classList.add("active");
    }

    button.innerHTML = `
      <div class="resultTitle">${escapeHtml(band.name || "Unknown band")}</div>
      <div class="resultMeta">${escapeHtml(buildBandSummaryMeta(band))}</div>
    `;
    button.addEventListener("click", () => onBandSelected(band));
    item.appendChild(button);
    elements.resultsList.appendChild(item);
  }

  elements.resultsEmpty.classList.add("hidden");
  elements.resultsList.classList.remove("hidden");
}

function createArtworkNode(src, className) {
  if (src) {
    const image = document.createElement("img");
    image.className = className;
    image.src = src;
    image.alt = "";
    image.loading = "lazy";
    image.referrerPolicy = "no-referrer";
    image.addEventListener("error", () => {
      image.removeAttribute("src");
    });
    return image;
  }

  const fallback = document.createElement("div");
  fallback.className = className;
  return fallback;
}

function renderBandDetail(detail) {
  for (const album of detail.discography) {
    ensureAlbumTypeColor(album.type);
  }
  state.selectedBandDetail = detail;
  showDetailContent();

  const chips = [detail.country, detail.status, detail.genre]
    .filter(Boolean)
    .map((value) => `<span class="chip">${escapeHtml(value)}</span>`)
    .join("");

  const favoriteLabel = isFavoriteBand(detail.profile_url) ? "★" : "☆";
  const favoriteTitle = isFavoriteBand(detail.profile_url) ? "Remove favorite" : "Add favorite";

  elements.detailContent.innerHTML = `
    <section class="heroCard">
      <div id="bandHeroArtwork"></div>
      <div>
        <div class="heroTitle">${escapeHtml(detail.name || "Unknown band")}</div>
        <div class="chips">${chips}</div>
        <div class="heroActions">
          <button class="button starButton" id="favoriteButton" type="button" title="${escapeHtml(favoriteTitle)}">${favoriteLabel}</button>
          ${
            detail.profile_url
              ? '<button class="button" id="openProfileButton" type="button">Open in browser</button>'
              : ""
          }
        </div>
      </div>
    </section>
    <section class="metaGrid">
      <div class="metaKey">Country</div><div class="metaValue">${escapeHtml(detail.country || "—")}</div>
      <div class="metaKey">Location</div><div class="metaValue">${escapeHtml(detail.location || "—")}</div>
      <div class="metaKey">Formed in</div><div class="metaValue">${escapeHtml(detail.formed_in || "—")}</div>
      <div class="metaKey">Years active</div><div class="metaValue">${escapeHtml(detail.years_active || "—")}</div>
      <div class="metaKey">Themes</div><div class="metaValue">${escapeHtml(detail.lyrical_themes || "—")}</div>
      <div class="metaKey">Label</div><div class="metaValue">${escapeHtml(detail.label || "—")}</div>
    </section>
    <section class="sectionCard">
      <div class="sectionHeader">
        <div class="sectionTitle">Discography</div>
        <select class="searchSelect discographyFilter" id="discographyFilter"></select>
      </div>
      <div class="albumList" id="discographyList"></div>
    </section>
  `;

  const artworkMount = document.getElementById("bandHeroArtwork");
  artworkMount.replaceWith(createArtworkNode(detail.image_url, "artwork"));

  const favoriteButton = document.getElementById("favoriteButton");
  favoriteButton?.addEventListener("click", () => toggleFavorite(detail));

  const openProfileButton = document.getElementById("openProfileButton");
  openProfileButton?.addEventListener("click", () => globalThis.ferrum.openExternal(detail.profile_url));

  const discographyFilter = document.getElementById("discographyFilter");
  const filters = resolveDiscographyFilters(detail.discography);
  for (const value of filters) {
    const option = document.createElement("option");
    option.value = value ?? "";
    option.textContent = value ?? "All types";
    discographyFilter.appendChild(option);
  }

  const selectedFilter = state.selectedBandFilter && filters.includes(state.selectedBandFilter) ? state.selectedBandFilter : null;
  discographyFilter.value = selectedFilter ?? "";
  state.selectedBandFilter = selectedFilter;
  discographyFilter.addEventListener("change", () => {
    state.selectedBandFilter = discographyFilter.value || null;
    renderDiscography(detail);
  });

  renderDiscography(detail);
}

function resolveDiscographyFilters(discography) {
  const filters = [null];
  for (const album of discography ?? []) {
    const label = String(album.type ?? "").trim() || "Other";
    if (!filters.includes(label)) {
      filters.push(label);
    }
  }
  return filters;
}

function renderDiscography(detail) {
  const list = document.getElementById("discographyList");
  if (!list) {
    return;
  }

  list.replaceChildren();
  let albums = detail.discography ?? [];
  if (state.selectedBandFilter) {
    albums = albums.filter((album) => (String(album.type ?? "").trim() || "Other") === state.selectedBandFilter);
  }

  if (albums.length === 0) {
    const empty = document.createElement("div");
    empty.className = "resultMeta";
    empty.textContent = detail.discography?.length
      ? "No releases for this filter. Try another discography type."
      : "No discography loaded. This band page did not expose a release table.";
    list.appendChild(empty);
    return;
  }

  for (const album of albums) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "albumRow";
    if (!album.url) {
      button.disabled = true;
    }
    button.innerHTML = `
      <div id="album-art-${Math.random().toString(36).slice(2)}"></div>
      <div>
        <div class="albumTitle">${escapeHtml(album.title || "Untitled release")}</div>
        <div class="albumMeta">
          <span>${escapeHtml(album.year || "Unknown year")}</span>
          <span>•</span>
          <span class="albumType albumTypeBadge" data-album-type-badge="${escapeHtml(resolveAlbumTypeName(album.type))}">${escapeHtml(album.type || "Other")}</span>
        </div>
      </div>
      <div class="arrow">${album.url ? "›" : ""}</div>
    `;
    const artworkSlot = button.firstElementChild;
    artworkSlot.replaceWith(createArtworkNode(album.image_url, "albumArtwork"));
    if (album.url) {
      button.addEventListener("click", () => openAlbum(album));
    }
    list.appendChild(button);
    const badge = button.querySelector("[data-album-type-badge]");
    if (badge) {
      applyAlbumTypeBadgeStyle(badge, badge.dataset.albumTypeBadge || "");
    }
  }
}

async function onBandSelected(band) {
  if (!band?.profile_url) {
    return;
  }

  state.selectedBandSummary = band;
  renderResultsList();
  setLoadingBand(true, band.name);

  try {
    const detail = await globalThis.ferrum.api.getBand(band.profile_url);
    renderBandDetail(normalizeBandDetail(detail));
  } catch (error) {
    showDetailPlaceholder("Band details live here", error?.message || "Failed to load band detail.");
  } finally {
    closeTaskLoading();
    state.isLoadingBand = false;
    elements.resultsList.style.pointerEvents = "";
  }
}

function toggleFavorite(detail) {
  if (!detail?.profile_url) {
    return;
  }

  const index = state.settings.favoriteBands.findIndex((item) => item.profile_url === detail.profile_url);
  if (index >= 0) {
    state.settings.favoriteBands.splice(index, 1);
  } else {
    state.settings.favoriteBands.unshift({
      name: detail.name || "Unknown band",
      country: detail.country || "",
      genre: detail.genre || "",
      status: detail.status || "",
      profile_url: detail.profile_url
    });
  }

  globalThis.ferrum.settings.save({
    ...state.settings,
    favoriteBands: state.settings.favoriteBands
  });
  renderBandDetail(detail);
  renderResultsList();
}

async function openAlbum(album) {
  if (!album?.url) {
    return;
  }

  elements.albumModalTitle.textContent = "Loading album...";
  elements.albumModalSubtitle.textContent = album.title || "";
  elements.albumModalBody.innerHTML = "";
  presentTaskLoading("Loading album", `Fetching details for ${album.title || "selected release"}...`);

  try {
    const detail = await globalThis.ferrum.api.getAlbum(album.url);
    const normalizedDetail = normalizeAlbumDetail(detail);
    if (state.selectedBandDetail) {
      const currentAlbum = state.selectedBandDetail.discography.find((item) => item.url === normalizedDetail.url);
      if (currentAlbum && normalizedDetail.image_url) {
        currentAlbum.image_url = normalizedDetail.image_url;
        renderDiscography(state.selectedBandDetail);
      }
    }
    renderAlbumModal(normalizedDetail);
    elements.albumModal.classList.remove("hidden");
  } catch (error) {
    elements.albumModalTitle.textContent = "Album";
    elements.albumModalSubtitle.textContent = error?.message || "Failed to load album";
  } finally {
    closeTaskLoading();
  }
}

function resolveProviderLabel() {
  return state.settings.musicProvider === "youtube" ? "YouTube" : "YouTube Music";
}

function buildProviderUrl(bandName, albumTitle, trackTitle = "") {
  const query = [bandName, albumTitle, trackTitle].filter(Boolean).join(" ");
  const encoded = encodeURIComponent(query);
  if (state.settings.musicProvider === "youtube") {
    return `https://www.youtube.com/results?search_query=${encoded}`;
  }
  return `https://music.youtube.com/search?q=${encoded}`;
}

function renderAlbumModal(album) {
  elements.albumModalTitle.textContent = album.title || "Album";
  elements.albumModalSubtitle.textContent = [album.type, album.release_date, album.label].filter(Boolean).join("  •  ");

  const tracks = (album.tracks ?? [])
    .map(
      (track) => `
        <div class="trackRow">
          <div class="trackMeta">${escapeHtml(track.number || "—")}</div>
          <div>${escapeHtml(track.title || "Untitled track")}</div>
          <div class="trackMeta">${escapeHtml(track.duration || "Unknown length")}</div>
          <button class="button" type="button" data-track-title="${escapeHtml(track.title || "")}">Play</button>
        </div>
      `
    )
    .join("");

  elements.albumModalBody.innerHTML = `
    <section class="heroCard" style="grid-template-columns:220px 1fr;">
      <div id="albumModalArtwork"></div>
      <div>
        <div class="heroTitle" style="font-size:28px;">${escapeHtml(album.title || "Album")}</div>
        <div class="chips">
          ${album.type ? `<span class="chip albumTypeBadge" data-album-type-badge="${escapeHtml(resolveAlbumTypeName(album.type))}">${escapeHtml(album.type)}</span>` : ""}
          ${album.release_date ? `<span class="chip">${escapeHtml(album.release_date)}</span>` : ""}
          ${album.label ? `<span class="chip">${escapeHtml(album.label)}</span>` : ""}
        </div>
        <div class="heroActions">
          ${album.url ? '<button class="button" id="albumOpenBrowserButton" type="button">Open in Metal Archives</button>' : ""}
          <button class="button" id="albumProviderButton" type="button">Search on ${escapeHtml(resolveProviderLabel())}</button>
        </div>
      </div>
    </section>
    <section class="sectionCard">
      <div class="sectionTitle" style="font-size:22px; margin-bottom:12px;">Tracklist</div>
      <div class="trackList">${tracks || '<div class="resultMeta">No tracklist returned.</div>'}</div>
    </section>
  `;

  const artworkNode = createArtworkNode(album.image_url, "artwork");
  artworkNode.classList.add("artworkSquare");
  document.getElementById("albumModalArtwork").replaceWith(artworkNode);

  document.getElementById("albumOpenBrowserButton")?.addEventListener("click", () => globalThis.ferrum.openExternal(album.url));
  document.getElementById("albumProviderButton")?.addEventListener("click", () => {
    globalThis.ferrum.openExternal(buildProviderUrl(state.selectedBandDetail?.name || "", album.title || ""));
  });

  for (const button of elements.albumModalBody.querySelectorAll("[data-track-title]")) {
    button.addEventListener("click", () => {
      globalThis.ferrum.openExternal(
        buildProviderUrl(state.selectedBandDetail?.name || "", album.title || "", button.dataset.trackTitle || "")
      );
    });
  }

  refreshAlbumTypeBadges();
}

function renderSearchHistoryModal() {
  elements.historyList.replaceChildren();

  if (state.searchHistory.length === 0) {
    const item = document.createElement("li");
    item.className = "resultMeta";
    item.textContent = "No search history yet.";
    elements.historyList.appendChild(item);
  } else {
    const entries = [...state.searchHistory].sort((a, b) => a.query.localeCompare(b.query, undefined, { sensitivity: "base" }));
    for (const entry of entries) {
      const item = document.createElement("li");
      const button = document.createElement("button");
      button.type = "button";
      button.className = "resultRow";
      button.innerHTML = `
        <div class="resultTitle">${escapeHtml(entry.query)}</div>
        <div class="resultMeta">${escapeHtml(SEARCH_TYPE_LABELS[entry.search_type] || entry.search_type)}</div>
      `;
      button.addEventListener("click", () => {
        elements.queryInput.value = entry.query;
        elements.searchTypeSelect.value = entry.search_type;
        closeHistoryModal();
        submitSearch();
      });
      item.appendChild(button);
      elements.historyList.appendChild(item);
    }
  }

  elements.historyModal.classList.remove("hidden");
}

function closeHistoryModal() {
  elements.historyModal.classList.add("hidden");
}

function closeAlbumModal() {
  elements.albumModal.classList.add("hidden");
}

function toggleAppMenu(forceOpen) {
  const shouldOpen = forceOpen ?? elements.appMenu.classList.contains("hidden");
  elements.appMenu.classList.toggle("hidden", !shouldOpen);
  elements.menuButton.setAttribute("aria-expanded", shouldOpen ? "true" : "false");
}

function closeAppMenu() {
  toggleAppMenu(false);
}

function renderSettingsColorRows() {
  elements.settingsColorList.replaceChildren();
  const albumTypes = Object.keys(state.settings.albumTypeColors).sort((a, b) => a.localeCompare(b, undefined, { sensitivity: "base" }));

  if (albumTypes.length === 0) {
    const empty = document.createElement("div");
    empty.className = "settingsEmpty";
    empty.textContent = "Album types will appear here after you load bands with discography data.";
    elements.settingsColorList.appendChild(empty);
    return;
  }

  for (const albumType of albumTypes) {
    const row = document.createElement("div");
    row.className = "settingsColorRow";

    const name = document.createElement("div");
    name.className = "settingsLabel";
    name.textContent = albumType;

    const preview = document.createElement("span");
    preview.className = "albumTypeBadge";
    preview.dataset.albumTypeBadge = albumType;
    preview.textContent = albumType;
    applyAlbumTypeBadgeStyle(preview, albumType);

    const input = document.createElement("input");
    input.className = "searchInput settingsColorInput";
    input.value = state.settings.albumTypeColors[albumType] || "";
    input.placeholder = "#RRGGBB";

    const saveButton = document.createElement("button");
    saveButton.type = "button";
    saveButton.className = "button";
    saveButton.textContent = "Save";
    saveButton.addEventListener("click", () => {
      const normalized = normalizeHexColor(input.value);
      if (!normalized) {
        window.alert("Use a valid color like #7C3AED.");
        input.value = state.settings.albumTypeColors[albumType] || "";
        return;
      }
      state.settings.albumTypeColors[albumType] = normalized;
      input.value = normalized;
      persistSettings();
      renderSettingsColorRows();
      refreshAlbumTypeBadges();
      if (state.selectedBandDetail) {
        renderDiscography(state.selectedBandDetail);
      }
    });

    const randomButton = document.createElement("button");
    randomButton.type = "button";
    randomButton.className = "button";
    randomButton.textContent = "Random";
    randomButton.addEventListener("click", () => {
      const randomColor = generateRandomColor();
      state.settings.albumTypeColors[albumType] = randomColor;
      input.value = randomColor;
      persistSettings();
      renderSettingsColorRows();
      refreshAlbumTypeBadges();
      if (state.selectedBandDetail) {
        renderDiscography(state.selectedBandDetail);
      }
    });

    row.append(name, preview, input, saveButton, randomButton);
    elements.settingsColorList.appendChild(row);
  }
}

function openSettingsModal() {
  elements.themeSelect.value = THEME_MODES.includes(state.settings.themeMode) ? state.settings.themeMode : "black";
  elements.providerSelect.value = state.settings.musicProvider || "youtube_music";
  renderSettingsColorRows();
  elements.settingsModal.classList.remove("hidden");
}

function closeSettingsModal() {
  elements.settingsModal.classList.add("hidden");
}

async function submitSearch() {
  if (!state.backendReady) {
    return;
  }

  const query = elements.queryInput.value.trim();
  if (!query) {
    return;
  }

  state.resultsMode = "search";
  state.selectedBandSummary = null;
  state.selectedBandDetail = null;
  state.selectedBandFilter = null;
  showDetailPlaceholder("Band details live here", "Pick a result to inspect line-up context, metadata and discography.");
  setSearching(true);
  state.results = [];
  presentTaskLoading("Searching", `Searching for ${query}...`);
  renderResultsList();

  try {
    const searchType = elements.searchTypeSelect.value;
    const results = await globalThis.ferrum.api.search(query, searchType);
    state.results = Array.isArray(results) ? results.map(normalizeBandSummary) : [];
    rememberSearchHistoryEntry(query, searchType);
    renderResultsList();
  } catch (error) {
    state.results = [];
    showResultsEmpty("No results", error?.message || "Search failed.");
    syncResultsCount();
  } finally {
    closeTaskLoading();
    setSearching(false);
  }
}

async function bootstrap() {
  const [config, settings] = await Promise.all([
    globalThis.ferrum.backend.getConfig(),
    globalThis.ferrum.settings.load()
  ]);

  state.settings = {
    themeMode: settings?.themeMode ?? settings?.theme_mode ?? "black",
    musicProvider: settings?.musicProvider ?? settings?.music_provider ?? "youtube_music",
    albumTypeColors: settings?.albumTypeColors ?? settings?.album_type_colors ?? {},
    favoriteBands: Array.isArray(settings?.favoriteBands ?? settings?.favorite_bands)
      ? (settings?.favoriteBands ?? settings?.favorite_bands).map(normalizeBandSummary)
      : []
  };
  applyTheme();
  setText(elements.backendUrl, `url: ${config.backendUrl}`);
  renderResultsList();
}

globalThis.ferrum.backend.onStatus((payload) => {
  if (!payload) {
    return;
  }

  setText(elements.backendState, `backend: ${payload.state ?? "unknown"}`);
  setText(elements.statusLine, payload.message ?? "...");

  if (payload.state === "ready" || payload.state === "external") {
    setBackendReady(true);
  } else if (payload.state === "error" || payload.state === "stopped") {
    setBackendReady(false);
  }
});

elements.queryInput.addEventListener("input", () => {
  elements.searchButton.disabled = !state.backendReady || state.isSearching || !elements.queryInput.value.trim();
});

elements.searchForm.addEventListener("submit", (event) => {
  event.preventDefault();
  submitSearch();
});

elements.favoritesToggle.addEventListener("click", () => {
  state.resultsMode = state.resultsMode === "favorites" ? "search" : "favorites";
  renderResultsList();
});

elements.menuButton.addEventListener("click", (event) => {
  event.stopPropagation();
  toggleAppMenu();
});

elements.menuSettingsButton.addEventListener("click", () => {
  closeAppMenu();
  openSettingsModal();
});

elements.menuHistoryButton.addEventListener("click", () => {
  closeAppMenu();
  renderSearchHistoryModal();
});

elements.menuQuitButton.addEventListener("click", () => {
  closeAppMenu();
  globalThis.ferrum.quit();
});

elements.historyCloseButton.addEventListener("click", closeHistoryModal);
elements.albumCloseButton.addEventListener("click", closeAlbumModal);

elements.historyModal.addEventListener("click", (event) => {
  if (event.target === elements.historyModal) {
    closeHistoryModal();
  }
});

elements.albumModal.addEventListener("click", (event) => {
  if (event.target === elements.albumModal) {
    closeAlbumModal();
  }
});

elements.settingsCloseButton.addEventListener("click", closeSettingsModal);

elements.settingsModal.addEventListener("click", (event) => {
  if (event.target === elements.settingsModal) {
    closeSettingsModal();
  }
});

document.addEventListener("click", (event) => {
  if (!elements.appMenu.contains(event.target) && !elements.menuButton.contains(event.target)) {
    closeAppMenu();
  }
});

elements.themeSelect.addEventListener("change", () => {
  state.settings.themeMode = elements.themeSelect.value;
  persistSettings();
  applyTheme();
  refreshAlbumTypeBadges();
  renderSettingsColorRows();
});

elements.providerSelect.addEventListener("change", () => {
  state.settings.musicProvider = elements.providerSelect.value;
  persistSettings();
});

window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
  if (state.settings.themeMode === "system") {
    applyTheme();
    refreshAlbumTypeBadges();
    renderSettingsColorRows();
  }
});

globalThis.ferrum.ui.onCommand((payload) => {
  if (!state.backendReady || !payload?.command) {
    if (payload?.command === "open-settings") {
      openSettingsModal();
    }
    return;
  }

  if (payload.command === "open-settings") {
    openSettingsModal();
  }

  if (payload.command === "open-search-history") {
    renderSearchHistoryModal();
  }
});

showResultsEmpty("Start with a band, genre or theme", "Search results will appear here and load band details on selection.");
showDetailPlaceholder("Band details live here", "Pick a result to inspect line-up context, metadata and discography.");
bootstrap();
