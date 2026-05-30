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
const COUNTRY_TO_ISO = {
  argentina: "AR",
  australia: "AU",
  austria: "AT",
  belgium: "BE",
  brazil: "BR",
  bulgaria: "BG",
  canada: "CA",
  chile: "CL",
  china: "CN",
  colombia: "CO",
  croatia: "HR",
  cuba: "CU",
  czechia: "CZ",
  "czech republic": "CZ",
  denmark: "DK",
  ecuador: "EC",
  egypt: "EG",
  england: "GB",
  estonia: "EE",
  finland: "FI",
  france: "FR",
  germany: "DE",
  greece: "GR",
  hungary: "HU",
  iceland: "IS",
  india: "IN",
  indonesia: "ID",
  ireland: "IE",
  israel: "IL",
  italy: "IT",
  japan: "JP",
  malta: "MT",
  mexico: "MX",
  netherlands: "NL",
  new_zealand: "NZ",
  "new zealand": "NZ",
  norway: "NO",
  peru: "PE",
  poland: "PL",
  portugal: "PT",
  romania: "RO",
  russia: "RU",
  "russian federation": "RU",
  serbia: "RS",
  slovakia: "SK",
  slovenia: "SI",
  spain: "ES",
  sweden: "SE",
  switzerland: "CH",
  turkey: "TR",
  uk: "GB",
  ukraine: "UA",
  "united kingdom": "GB",
  "united states": "US",
  usa: "US",
  uruguay: "UY",
  venezuela: "VE",
  wales: "GB"
};

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
  menuRefreshBandButton: document.getElementById("menuRefreshBandButton"),
  menuSettingsButton: document.getElementById("menuSettingsButton"),
  menuHistoryButton: document.getElementById("menuHistoryButton"),
  menuQuitButton: document.getElementById("menuQuitButton"),
  searchButton: document.getElementById("searchButton"),
  resultsHeading: document.getElementById("resultsHeading"),
  resultsCount: document.getElementById("resultsCount"),
  resultsBody: document.getElementById("resultsBody"),
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
  favoriteLogoOpacityRange: document.getElementById("favoriteLogoOpacityRange"),
  favoriteLogoOpacityValue: document.getElementById("favoriteLogoOpacityValue"),
  favoriteImageOnlyCheckbox: document.getElementById("favoriteImageOnlyCheckbox"),
  settingsColorList: document.getElementById("settingsColorList"),
  taskLoadingOverlay: document.getElementById("taskLoadingOverlay"),
  taskLoadingTitle: document.getElementById("taskLoadingTitle"),
  taskLoadingMessage: document.getElementById("taskLoadingMessage")
};

const state = {
  backendReady: false,
  historyLoaded: false,
  resultsMode: "search",
  resultsScrollTop: {
    search: 0,
    favorites: 0
  },
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
    favoriteLogoOpacity: 0,
    favoriteImageOnly: false,
    albumTypeColors: {},
    favoriteBands: []
  },
  favoriteArtworkLoading: new Set(),
  expandedAlbumUrl: null,
  expandedAlbumLoadingUrl: null,
  expandedAlbumError: null,
  albumDetailsByUrl: {},
  taskLoadingTimer: null,
  detailLoadingTimer: null
};

elements.appVersion.textContent = `electron · ${globalThis.ferrum?.version ?? "unknown"}`;

function normalizeBandSummary(item) {
  return {
    name: String(item?.name ?? "").trim(),
    country: String(item?.country ?? "").trim(),
    genre: String(item?.genre ?? "").trim(),
    status: String(item?.status ?? "").trim(),
    profile_url: String(item?.profile_url ?? item?.profileUrl ?? "").trim(),
    image_url: String(item?.image_url ?? item?.imageUrl ?? "").trim()
  };
}

function countryToFlag(country) {
  const normalizedCountry = String(country ?? "").trim().toLowerCase();
  if (!normalizedCountry) {
    return "";
  }

  const isoCode = COUNTRY_TO_ISO[normalizedCountry];
  if (!isoCode) {
    return "";
  }

  return [...isoCode.toUpperCase()]
    .map((character) => String.fromCodePoint(127397 + character.charCodeAt(0)))
    .join("");
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
    favoriteLogoOpacity: state.settings.favoriteLogoOpacity,
    favoriteImageOnly: state.settings.favoriteImageOnly,
    albumTypeColors: state.settings.albumTypeColors,
    favoriteBands: state.settings.favoriteBands
  });
}

function clampFavoriteLogoOpacity(value) {
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round(numericValue)));
}

function applyFavoriteLogoOpacity() {
  const opacity = clampFavoriteLogoOpacity(state.settings.favoriteLogoOpacity);
  state.settings.favoriteLogoOpacity = opacity;
  document.documentElement.style.setProperty("--favorite-logo-opacity", String(opacity / 100));
  if (elements.favoriteLogoOpacityRange) {
    elements.favoriteLogoOpacityRange.value = String(opacity);
  }
  if (elements.favoriteLogoOpacityValue) {
    elements.favoriteLogoOpacityValue.textContent = `${opacity}%`;
  }
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
    return;
  }
  if (state.detailLoadingTimer) {
    window.clearTimeout(state.detailLoadingTimer);
    state.detailLoadingTimer = null;
  }
  elements.resultsList.style.pointerEvents = "";
}

function presentTaskLoading(title, message) {
  if (state.taskLoadingTimer) {
    window.clearTimeout(state.taskLoadingTimer);
  }

  state.taskLoadingTimer = window.setTimeout(() => {
    elements.taskLoadingTitle.textContent = title;
    elements.taskLoadingMessage.textContent = message;
    elements.taskLoadingOverlay.classList.remove("hidden");
    state.taskLoadingTimer = null;
  }, 120);
}

function closeTaskLoading() {
  if (state.taskLoadingTimer) {
    window.clearTimeout(state.taskLoadingTimer);
    state.taskLoadingTimer = null;
  }
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

async function hydrateFavoriteBandArtwork() {
  if (state.resultsMode !== "favorites") {
    return;
  }

  const missingArtworkFavorites = state.settings.favoriteBands.filter(
    (band) => band.profile_url && !band.image_url && !state.favoriteArtworkLoading.has(band.profile_url)
  );

  for (const band of missingArtworkFavorites) {
    state.favoriteArtworkLoading.add(band.profile_url);
    try {
      const detail = normalizeBandDetail(await globalThis.ferrum.api.getBand(band.profile_url));
      if (!detail.image_url) {
        continue;
      }

      const favoriteBand = state.settings.favoriteBands.find((item) => item.profile_url === band.profile_url);
      if (!favoriteBand) {
        continue;
      }

      favoriteBand.image_url = detail.image_url;
      globalThis.ferrum.settings.save({
        ...state.settings,
        favoriteBands: state.settings.favoriteBands
      });

      if (state.resultsMode === "favorites") {
        renderResultsList();
      }
    } catch {
    } finally {
      state.favoriteArtworkLoading.delete(band.profile_url);
    }
  }
}

function renderResultsList() {
  const items = getVisibleResults();
  const previousScrollTop = state.resultsScrollTop[state.resultsMode] ?? 0;
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
    if (state.resultsMode === "favorites" && band.image_url) {
      button.classList.add("favoriteResultRow");
      if (state.settings.favoriteImageOnly) {
        button.classList.add("imageOnlyFavoriteRow");
      }
    }

    const flag = countryToFlag(band.country);
    button.innerHTML = `
      ${state.resultsMode === "favorites" && band.image_url ? `<img class="favoriteResultLogo" src="${escapeHtml(band.image_url)}" alt="" loading="lazy" referrerpolicy="no-referrer" />` : ""}
      <div class="resultRowContent">
        <div class="resultTitle">${flag ? `${flag} ` : ""}${escapeHtml(band.name || "Unknown band")}</div>
        <div class="resultMeta">${escapeHtml(buildBandSummaryMeta(band))}</div>
      </div>
    `;
    button.addEventListener("click", () => onBandSelected(band));
    item.appendChild(button);
    elements.resultsList.appendChild(item);
  }

  elements.resultsEmpty.classList.add("hidden");
  elements.resultsList.classList.remove("hidden");
  elements.resultsBody.scrollTop = previousScrollTop;
  requestAnimationFrame(() => {
    elements.resultsBody.scrollTop = previousScrollTop;
  });

  if (state.resultsMode === "favorites") {
    void hydrateFavoriteBandArtwork();
  }
}

function persistFavoriteBandArtwork(detail) {
  if (!detail?.profile_url || !detail?.image_url) {
    return;
  }

  const favoriteBand = state.settings.favoriteBands.find((item) => item.profile_url === detail.profile_url);
  if (!favoriteBand || favoriteBand.image_url === detail.image_url) {
    return;
  }

  favoriteBand.image_url = detail.image_url;
  globalThis.ferrum.settings.save({
    ...state.settings,
    favoriteBands: state.settings.favoriteBands
  });
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

function createCompactDiscNode(size) {
  const discStage = document.createElement("div");
  discStage.style.cssText = `
    width:100%;
    height:100%;
    display:flex;
    align-items:center;
    justify-content:center;
    background:radial-gradient(circle at 50% 45%, #1e2126 0%, #191c20 58%, #111317 100%);
    overflow:hidden;
  `;

  const disc = document.createElement("div");
  const discSize = Math.max(28, Math.round(size * 0.76));
  disc.style.cssText = `
    width:${discSize}px;
    height:${discSize}px;
    border-radius:50%;
    position:relative;
    background:
      radial-gradient(circle at 28% 24%, rgba(255,255,255,0.95) 0 4%, rgba(255,255,255,0.24) 5%, rgba(255,255,255,0) 18%),
      radial-gradient(circle at 50% 50%, #fbfbfd 0%, #d5d9df 22%, #8f969e 48%, #d7dbe0 63%, #7d848d 78%, #e4e7eb 88%, #b7bcc3 100%);
    box-shadow:
      0 10px 20px rgba(0, 0, 0, 0.4),
      inset 0 2px 6px rgba(255, 255, 255, 0.55),
      inset 0 -10px 18px rgba(0, 0, 0, 0.28);
  `;

  const iridescence = document.createElement("div");
  iridescence.style.cssText = `
    position:absolute;
    inset:0;
    border-radius:50%;
    background:conic-gradient(
      from 210deg,
      rgba(116, 199, 255, 0.18),
      rgba(255, 120, 190, 0.12),
      rgba(255, 228, 122, 0.16),
      rgba(100, 255, 184, 0.12),
      rgba(116, 199, 255, 0.18)
    );
    mix-blend-mode:screen;
    opacity:0.7;
  `;
  disc.appendChild(iridescence);

  const outerRing = document.createElement("div");
  outerRing.style.cssText = `
    position:absolute;
    inset:${Math.max(2, Math.round(discSize * 0.016))}px;
    border-radius:50%;
    border:${Math.max(1, Math.round(discSize * 0.012))}px solid rgba(255,255,255,0.22);
    box-shadow:inset 0 0 ${Math.max(4, Math.round(discSize * 0.035))}px rgba(0,0,0,0.18);
  `;
  disc.appendChild(outerRing);

  const labelRing = document.createElement("div");
  const labelRingSize = Math.round(discSize * 0.38);
  labelRing.style.cssText = `
    position:absolute;
    top:50%;
    left:50%;
    width:${labelRingSize}px;
    height:${labelRingSize}px;
    transform:translate(-50%, -50%);
    border-radius:50%;
    background:radial-gradient(circle, rgba(235,238,242,0.95) 0%, rgba(174,180,188,0.88) 58%, rgba(120,126,135,0.82) 100%);
    box-shadow:
      inset 0 1px 3px rgba(255,255,255,0.35),
      inset 0 -2px 4px rgba(0,0,0,0.2);
  `;
  disc.appendChild(labelRing);

  const hub = document.createElement("div");
  const hubSize = Math.max(8, Math.round(discSize * 0.12));
  hub.style.cssText = `
    position:absolute;
    top:50%;
    left:50%;
    width:${hubSize}px;
    height:${hubSize}px;
    transform:translate(-50%, -50%);
    border-radius:50%;
    background:radial-gradient(circle, #0e1013 0%, #1a1d22 55%, #060708 100%);
    box-shadow:
      0 0 0 ${Math.max(2, Math.round(discSize * 0.03))}px rgba(205,210,217,0.62),
      inset 0 1px 2px rgba(255,255,255,0.14);
  `;
  disc.appendChild(hub);

  const sheen = document.createElement("div");
  sheen.style.cssText = `
    position:absolute;
    inset:0;
    border-radius:50%;
    background:linear-gradient(135deg, rgba(255,255,255,0.26) 0%, rgba(255,255,255,0.08) 20%, rgba(255,255,255,0) 48%, rgba(255,255,255,0.12) 76%, rgba(255,255,255,0) 100%);
    opacity:0.9;
  `;
  disc.appendChild(sheen);

  discStage.appendChild(disc);
  return discStage;
}

function createJewelcaseNode(album, options = {}) {
  const coverSize = options.coverSize ?? 340;
  const spineWidth = options.spineWidth ?? 32;
  const frameInset = options.frameInset ?? 0.97;
  const caseNode = document.createElement("div");
  caseNode.style.cssText = `
    width:${coverSize + spineWidth}px;
    height:${coverSize}px;
    background:linear-gradient(135deg,#23262a 80%,#444 100%);
    border-radius:0;
    box-shadow:0 12px 36px #000b, 0 0 0 8px #222a inset;
    display:flex;
    flex-direction:row;
    overflow:hidden;
    border:4px solid #191c20;
    position:relative;
    flex:0 0 auto;
  `;

  const spine = document.createElement("div");
  spine.style.cssText = `
    width:${spineWidth}px;
    background:linear-gradient(180deg,#121417 0%,#181b1f 100%);
    display:flex;
    align-items:center;
    justify-content:center;
    writing-mode:vertical-rl;
    text-orientation:mixed;
    box-shadow:inset -6px 0 14px #000f, inset 1px 0 0 #2a2d31;
    border-right:2px solid #0b0d10;
    position:relative;
    flex:0 0 auto;
  `;

  const spineGloss = document.createElement("div");
  spineGloss.style.cssText = "position:absolute;top:0;left:0;width:100%;height:100%;box-shadow:inset 0 0 12px #fff3,0 0 8px #0008;pointer-events:none;";
  spine.appendChild(spineGloss);

  const front = document.createElement("div");
  front.style.cssText = `
    flex:1;
    background:linear-gradient(120deg,#23262a 80%,#444 100%);
    display:flex;
    align-items:center;
    justify-content:center;
    border-left:2px solid #191c20;
    position:relative;
    overflow:hidden;
  `;

  const frame = document.createElement("div");
  frame.style.cssText = `
    width:${frameInset * 100}%;
    height:${frameInset * 100}%;
    background:#222;
    display:flex;
    align-items:center;
    justify-content:center;
    box-shadow:0 2px 8px #0006;
    border:1px solid #2a2d31;
    position:relative;
    z-index:1;
  `;

  if (album.image_url) {
    const coverImage = createArtworkNode(album.image_url, "");
    coverImage.alt = album.title || "Album cover";
    coverImage.loading = "eager";
    coverImage.style.cssText = "width:calc(100% - 1px);height:calc(100% - 1px);object-fit:cover;box-shadow:0 0 8px #0004;background:#222;";
    frame.appendChild(coverImage);
  } else {
    frame.appendChild(createCompactDiscNode(coverSize));
  }

  front.appendChild(frame);

  const frontGloss = document.createElement("div");
  frontGloss.style.cssText = "position:absolute;top:0;left:0;width:100%;height:100%;box-shadow:inset 0 0 32px #fff3,0 0 24px #0008;pointer-events:none;";
  front.appendChild(frontGloss);

  const hinge = document.createElement("div");
  hinge.style.cssText = `
    position:absolute;
    bottom:${Math.max(8, Math.round(coverSize * 0.035))}px;
    right:${Math.max(12, Math.round(coverSize * 0.053))}px;
    width:${Math.max(24, Math.round(coverSize * 0.11))}px;
    height:${Math.max(24, Math.round(coverSize * 0.11))}px;
    border-radius:50%;
    background:radial-gradient(circle,#fff8 60%,#aaa2 100%);
    box-shadow:0 2px 8px #0006;
    opacity:0.18;
    pointer-events:none;
  `;
  front.appendChild(hinge);

  const plasticTexture = document.createElement("div");
  plasticTexture.style.cssText = "position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;mix-blend-mode:soft-light;background:repeating-linear-gradient(120deg,rgba(255,255,255,0.04) 0 2px,transparent 2px 12px);";

  caseNode.append(spine, front, plasticTexture);
  return caseNode;
}

function renderBandStatusChip(status) {
  if (!status) {
    return "";
  }

  const normalized = String(status).trim().toLowerCase();
  const isActive = normalized === "active";
  const title = String(status).trim();
  return `<span class="chip statusChip" title="${escapeHtml(title)}"><span class="statusDot${isActive ? " isActive" : ""}"></span></span>`;
}

function renderBandDetail(detail) {
  for (const album of detail.discography) {
    ensureAlbumTypeColor(album.type);
  }
  state.selectedBandDetail = detail;
  state.expandedAlbumUrl = null;
  state.expandedAlbumLoadingUrl = null;
  state.expandedAlbumError = null;
  state.albumDetailsByUrl = {};
  persistFavoriteBandArtwork(detail);
  showDetailContent();

  const countryFlag = countryToFlag(detail.country);
  const countryLocation = [detail.country, detail.location].filter(Boolean).join(", ");
  const formedLabel = detail.formed_in ? `Since ${detail.formed_in}` : null;
  const chips = [
    renderBandStatusChip(detail.status),
    countryLocation ? `<span class="chip">${escapeHtml(`${countryFlag ? `${countryFlag} ` : ""}${countryLocation}`)}</span>` : "",
    detail.genre ? `<span class="chip">${escapeHtml(detail.genre)}</span>` : "",
    formedLabel ? `<span class="chip">${escapeHtml(formedLabel)}</span>` : "",
    detail.years_active ? `<span class="chip">${escapeHtml(detail.years_active)}</span>` : ""
  ]
    .filter(Boolean)
    .join("");

  const favoriteLabel = isFavoriteBand(detail.profile_url) ? "★" : "☆";
  const favoriteTitle = isFavoriteBand(detail.profile_url) ? "Remove favorite" : "Add favorite";

  elements.detailContent.innerHTML = `
    <section class="heroCard bandHeroCard">
      <div class="bandHeroText">
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
      <div id="bandHeroArtwork"></div>
    </section>
    <section class="sectionCard discographySection">
      <div class="sectionHeader discographyHeader">
        <div class="sectionTitle">Discography</div>
        <select class="searchSelect discographyFilter" id="discographyFilter"></select>
      </div>
      <div class="albumList" id="discographyList"></div>
    </section>
  `;

  const artworkMount = document.getElementById("bandHeroArtwork");
  const artworkNode = createArtworkNode(detail.image_url, "artwork");
  artworkNode.classList.add("bandHeroArtwork");
  artworkMount.replaceWith(artworkNode);

  const favoriteButton = document.getElementById("favoriteButton");
  if (favoriteButton && isFavoriteBand(detail.profile_url)) {
    favoriteButton.classList.add("favoriteStarActive");
  }
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
  updateAppMenuState();
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

  const previousScrollTop = list.scrollTop;
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
    const entry = document.createElement("div");
    entry.className = "albumEntry";
    const button = document.createElement("button");
    button.type = "button";
    button.className = "albumRow";
    if (state.expandedAlbumUrl === album.url) {
      button.classList.add("isExpanded");
    }
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
    const jewelcaseNode = createJewelcaseNode(album, { coverSize: 72, spineWidth: 10, frameInset: 0.972 });
    jewelcaseNode.classList.add("albumArtworkJewelcase");
    artworkSlot.replaceWith(jewelcaseNode);
    if (album.url) {
      button.addEventListener("click", () => openAlbum(album));
    }
    entry.appendChild(button);
    const badge = button.querySelector("[data-album-type-badge]");
    if (badge) {
      applyAlbumTypeBadgeStyle(badge, badge.dataset.albumTypeBadge || "");
    }

    if (album.url && state.expandedAlbumUrl === album.url) {
      const panel = buildExpandedAlbumPanel(album);
      entry.appendChild(panel);
    }

    list.appendChild(entry);
  }

  list.scrollTop = previousScrollTop;
}

async function onBandSelected(band) {
  if (!band?.profile_url) {
    return;
  }

  state.resultsScrollTop[state.resultsMode] = elements.resultsBody.scrollTop;
  state.selectedBandSummary = band;
  renderResultsList();

  let shouldShowLoading = true;
  try {
    shouldShowLoading = !(await globalThis.ferrum.api.hasBandCache(band.profile_url));
  } catch {
    shouldShowLoading = true;
  }

  if (shouldShowLoading) {
    setLoadingBand(true, band.name);
  }

  try {
    const detail = await globalThis.ferrum.api.getBand(band.profile_url);
    renderBandDetail(normalizeBandDetail(detail));
  } catch (error) {
    showDetailPlaceholder("Band details live here", error?.message || "Failed to load band detail.");
  } finally {
    closeTaskLoading();
    state.isLoadingBand = false;
    elements.resultsList.style.pointerEvents = "";
    elements.resultsBody.scrollTop = state.resultsScrollTop[state.resultsMode] ?? 0;
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
      profile_url: detail.profile_url,
      image_url: detail.image_url || ""
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

  if (state.expandedAlbumUrl === album.url && state.expandedAlbumLoadingUrl !== album.url) {
    state.expandedAlbumUrl = null;
    state.expandedAlbumError = null;
    renderDiscography(state.selectedBandDetail);
    return;
  }

  state.expandedAlbumUrl = album.url;
  state.expandedAlbumError = null;

  if (state.albumDetailsByUrl[album.url]) {
    renderDiscography(state.selectedBandDetail);
    return;
  }

  let shouldShowLoading = true;
  try {
    shouldShowLoading = !(await globalThis.ferrum.api.hasAlbumCache(album.url));
  } catch {
    shouldShowLoading = true;
  }

  if (shouldShowLoading) {
    state.expandedAlbumLoadingUrl = album.url;
    presentTaskLoading("Loading album", `Fetching details for ${album.title || "selected release"}...`);
    renderDiscography(state.selectedBandDetail);
  }

  try {
    const detail = await globalThis.ferrum.api.getAlbum(album.url);
    const normalizedDetail = normalizeAlbumDetail(detail);
    state.albumDetailsByUrl[album.url] = normalizedDetail;
    if (state.selectedBandDetail) {
      const currentAlbum = state.selectedBandDetail.discography.find((item) => item.url === normalizedDetail.url);
      if (currentAlbum && normalizedDetail.image_url) {
        currentAlbum.image_url = normalizedDetail.image_url;
      }
    }
    state.expandedAlbumError = null;
  } catch (error) {
    state.expandedAlbumError = {
      url: album.url,
      message: error?.message || "Failed to load album"
    };
  } finally {
    state.expandedAlbumLoadingUrl = null;
    renderDiscography(state.selectedBandDetail);
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

function buildExpandedAlbumPanel(albumSummary) {
  const panel = document.createElement("section");
  panel.className = "sectionCard albumExpandedPanel";

  if (state.expandedAlbumLoadingUrl === albumSummary.url) {
    panel.innerHTML = `
      <div class="sectionTitle" style="font-size:20px;">Loading album...</div>
      <div class="resultMeta">Fetching details for ${escapeHtml(albumSummary.title || "selected release")}.</div>
    `;
    return panel;
  }

  if (state.expandedAlbumError?.url === albumSummary.url) {
    panel.innerHTML = `
      <div class="sectionTitle" style="font-size:20px;">Album</div>
      <div class="resultMeta">${escapeHtml(state.expandedAlbumError.message)}</div>
    `;
    return panel;
  }

  const album = state.albumDetailsByUrl[albumSummary.url];
  if (!album) {
    panel.innerHTML = `
      <div class="sectionTitle" style="font-size:20px;">Album</div>
      <div class="resultMeta">No album detail loaded yet.</div>
    `;
    return panel;
  }

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

  panel.innerHTML = `
    <section class="heroCard albumExpandedHero">
      <div id="album-inline-art-${Math.random().toString(36).slice(2)}"></div>
      <div>
        <div class="heroTitle" style="font-size:28px;">${escapeHtml(album.title || "Album")}</div>
        <div class="chips">
          ${album.type ? `<span class="chip albumTypeBadge" data-album-type-badge="${escapeHtml(resolveAlbumTypeName(album.type))}">${escapeHtml(album.type)}</span>` : ""}
          ${album.release_date ? `<span class="chip">${escapeHtml(album.release_date)}</span>` : ""}
          ${album.label ? `<span class="chip">${escapeHtml(album.label)}</span>` : ""}
        </div>
        <div class="heroActions">
          ${album.url ? '<button class="button" data-album-open-browser type="button">Open in Metal Archives</button>' : ""}
          <button class="button" data-album-provider type="button">Search on ${escapeHtml(resolveProviderLabel())}</button>
        </div>
      </div>
    </section>
    <section class="sectionCard" style="margin-top:0;">
      <div class="sectionTitle" style="font-size:22px; margin-bottom:12px;">Tracklist</div>
      <div class="trackList">${tracks || '<div class="resultMeta">No tracklist returned.</div>'}</div>
    </section>
  `;

  const artworkSlot = panel.querySelector("[id^='album-inline-art-']");
  const jewelcaseNode = createJewelcaseNode(album, { coverSize: 220, spineWidth: 24, frameInset: 0.972 });
  artworkSlot?.replaceWith(jewelcaseNode);

  panel.querySelector("[data-album-open-browser]")?.addEventListener("click", () => globalThis.ferrum.openExternal(album.url));
  panel.querySelector("[data-album-provider]")?.addEventListener("click", () => {
    startAlbumPlayback(album, "");
  });

  for (const button of panel.querySelectorAll("[data-track-title]")) {
    button.addEventListener("click", () => {
      startAlbumPlayback(album, button.dataset.trackTitle || "");
    });
  }

  for (const badge of panel.querySelectorAll("[data-album-type-badge]")) {
    applyAlbumTypeBadgeStyle(badge, badge.dataset.albumTypeBadge || "");
  }

  return panel;
}

function startAlbumPlayback(album, trackTitle = "") {
  const bandName = state.selectedBandDetail?.name || "";
  const albumTitle = album.title || "";
  const url = buildProviderUrl(bandName, albumTitle, trackTitle);
  globalThis.ferrum.openExternal(url);
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

async function refreshSelectedBandFromEndpoint() {
  const detail = state.selectedBandDetail;
  if (!detail?.profile_url) {
    return;
  }

  presentTaskLoading("Refreshing band", `Fetching fresh data for ${detail.name || "selected band"}...`);
  try {
    await globalThis.ferrum.api.clearBandCache(detail.profile_url);
    const refreshedDetail = await globalThis.ferrum.api.getBand(detail.profile_url);
    renderBandDetail(normalizeBandDetail(refreshedDetail));
  } catch (error) {
    window.alert(error?.message || "Could not refresh artist data.");
  } finally {
    closeTaskLoading();
  }
}

function updateAppMenuState() {
  const selectedBandName = state.selectedBandDetail?.name?.trim();
  if (selectedBandName) {
    elements.menuRefreshBandButton.disabled = false;
    elements.menuRefreshBandButton.textContent = `Refresh ${selectedBandName}`;
    return;
  }

  elements.menuRefreshBandButton.disabled = true;
  elements.menuRefreshBandButton.textContent = "Refresh selected band";
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
  applyFavoriteLogoOpacity();
  if (elements.favoriteImageOnlyCheckbox) {
    elements.favoriteImageOnlyCheckbox.checked = Boolean(state.settings.favoriteImageOnly);
  }
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
    favoriteLogoOpacity: clampFavoriteLogoOpacity(settings?.favoriteLogoOpacity ?? settings?.favorite_logo_opacity ?? 0),
    favoriteImageOnly: Boolean(settings?.favoriteImageOnly ?? settings?.favorite_image_only ?? false),
    albumTypeColors: settings?.albumTypeColors ?? settings?.album_type_colors ?? {},
    favoriteBands: Array.isArray(settings?.favoriteBands ?? settings?.favorite_bands)
      ? (settings?.favoriteBands ?? settings?.favorite_bands).map(normalizeBandSummary)
      : []
  };
  applyTheme();
  applyFavoriteLogoOpacity();
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
  state.resultsScrollTop[state.resultsMode] = elements.resultsBody.scrollTop;
  state.resultsMode = state.resultsMode === "favorites" ? "search" : "favorites";
  renderResultsList();
});

elements.resultsBody.addEventListener("scroll", () => {
  state.resultsScrollTop[state.resultsMode] = elements.resultsBody.scrollTop;
});

elements.menuButton.addEventListener("click", (event) => {
  event.stopPropagation();
  toggleAppMenu();
});

elements.menuRefreshBandButton.addEventListener("click", async () => {
  closeAppMenu();
  await refreshSelectedBandFromEndpoint();
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

elements.favoriteLogoOpacityRange?.addEventListener("input", () => {
  state.settings.favoriteLogoOpacity = clampFavoriteLogoOpacity(elements.favoriteLogoOpacityRange.value);
  applyFavoriteLogoOpacity();
});

elements.favoriteLogoOpacityRange?.addEventListener("change", () => {
  state.settings.favoriteLogoOpacity = clampFavoriteLogoOpacity(elements.favoriteLogoOpacityRange.value);
  applyFavoriteLogoOpacity();
  persistSettings();
});

elements.favoriteImageOnlyCheckbox?.addEventListener("change", () => {
  state.settings.favoriteImageOnly = elements.favoriteImageOnlyCheckbox.checked;
  persistSettings();
  if (state.resultsMode === "favorites") {
    renderResultsList();
  }
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
updateAppMenuState();
bootstrap();
