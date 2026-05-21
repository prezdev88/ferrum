package cl.tracktec.metallum.infrastructure.scraper;

import cl.tracktec.metallum.core.domain.AlbumDetail;
import cl.tracktec.metallum.core.domain.BandDetail;
import cl.tracktec.metallum.core.domain.BandSearchType;
import cl.tracktec.metallum.core.domain.BandSummary;
import cl.tracktec.metallum.core.port.BandSearchGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component("remoteBandSearchGateway")
public class MetallumScraperGateway implements BandSearchGateway {

    private static final String BASE_URL   = "https://www.metal-archives.com/";
    private static final String SEARCH_URL = BASE_URL +
            "search?searchString=%s&type=%s";

    private static final Path SESSION_FILE =
            Path.of(System.getProperty("user.home"), ".config", "metallum", "session.json");

    @Value("${metallum.browser.visible:false}")
    private boolean visible;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Playwright playwright;
    private Browser    browser;
    private Page       page;

    @PostConstruct
    void init() throws IOException {
        playwright = Playwright.create();
        System.out.println();

        if (Files.exists(SESSION_FILE)) {
            printSessionExpiry();
            if (tryHeadlessWithSavedSession()) {
                System.out.println("Sesión establecida con Metal Archives.\n");
                return;
            }
            System.out.println("Sesión expirada, se requiere nueva verificación.");
        }

        solveCloudflareAndSaveSession();
        System.out.println("Sesión establecida con Metal Archives.\n");
    }

    private void printSessionExpiry() {
        try {
            JsonNode root    = objectMapper.readTree(SESSION_FILE.toFile());
            JsonNode cookies = root.get("cookies");
            if (cookies == null) return;

            DateTimeFormatter fmt = DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

            for (JsonNode cookie : cookies) {
                if ("cf_clearance".equals(cookie.path("name").asText())) {
                    long expires = cookie.path("expires").asLong(-1);
                    if (expires > 0) {
                        Instant expiry = Instant.ofEpochSecond(expires);
                        boolean vigente = expiry.isAfter(Instant.now());
                        System.out.printf("Sesión guardada — caduca el %s%s%n",
                                fmt.format(expiry),
                                vigente ? "" : " (EXPIRADA)");
                    }
                    return;
                }
            }
            System.out.println("Cargando sesión guardada...");
        } catch (IOException ignored) {
            System.out.println("Cargando sesión guardada...");
        }
    }

    private boolean tryHeadlessWithSavedSession() throws IOException {
        String storageState = Files.readString(SESSION_FILE);
        browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(!visible));
        page = browser.newContext(
                new Browser.NewContextOptions().setStorageState(storageState)
        ).newPage();

        page.navigate(BASE_URL,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        if (page.title().toLowerCase().contains("moment")) {
            // Cookies expiradas — cerrar y volver a verificar
            browser.close();
            return false;
        }
        return true;
    }

    private void solveCloudflareAndSaveSession() throws IOException {
        browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newContext().newPage();

        System.out.println("Abriendo navegador...");
        page.navigate(BASE_URL,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        if (page.title().toLowerCase().contains("moment")) {
            System.out.println("Completa el captcha en la ventana del navegador...");
            page.waitForFunction(
                    "() => !document.title.toLowerCase().includes('moment')",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(120_000).setPollingInterval(500)
            );
            System.out.println("¡Verificación superada! Cerrando ventana...");
        }

        String storageState = page.context().storageState();
        browser.close();

        // Persistir sesión para próximas ejecuciones
        Files.createDirectories(SESSION_FILE.getParent());
        Files.writeString(SESSION_FILE, storageState);

        // Relanzar con el modo configurado en properties
        browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(!visible));
        page = browser.newContext(
                new Browser.NewContextOptions().setStorageState(storageState)
        ).newPage();
        page.navigate(BASE_URL,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    @PreDestroy
    void destroy() {
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();
    }

    // ── BandSearchGateway ─────────────────────────────────────────────────────

    @Override
    public List<BandSummary> search(String query, BandSearchType searchType) {
        try {
            String searchPageUrl = String.format(
                    SEARCH_URL,
                    URLEncoder.encode(query, StandardCharsets.UTF_8),
                    searchType.getRequestType()
            );

            /*
             * Navegar a la página de búsqueda y capturar la respuesta AJAX
             * que el propio sitio genera. De esta forma el navegador gestiona
             * cookies y cabeceras por sí solo — no los falsificamos nosotros.
             */
            Response ajaxResponse = page.waitForResponse(
                    r -> r.url().contains("ajax") && r.status() == 200,
                    () -> page.navigate(searchPageUrl,
                            new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
            );

            return parseSearchResults(ajaxResponse.text(), searchType);

        } catch (TimeoutError e) {
            return new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException("Error al buscar en Metal Archives: " + e.getMessage(), e);
        }
    }

    @Override
    public BandDetail getDetails(String profileUrl) {
        try {
            Thread.sleep(500);

            /*
             * Navegar a la página de la banda y esperar a que la discografía
             * se cargue (es una llamada AJAX separada que el sitio hace al abrir
             * el tab por defecto).
             */
            try {
                page.waitForResponse(
                        r -> r.url().contains("/band/discography/id/") && r.status() == 200,
                        () -> page.navigate(profileUrl,
                                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
                );
            } catch (TimeoutError ignored) {
                // Banda sin discografía o tab de disco no activo — igual procesamos
            }

            return parseDetails(page.content(), profileUrl);

        } catch (Exception e) {
            throw new RuntimeException("Error al obtener detalles: " + e.getMessage(), e);
        }
    }

    // ── parsers ───────────────────────────────────────────────────────────────

    private List<BandSummary> parseSearchResults(String json, BandSearchType searchType) throws IOException {
        JsonNode root   = objectMapper.readTree(json);
        JsonNode aaData = root.get("aaData");
        List<BandSummary> results = new ArrayList<>();

        if (aaData != null && aaData.isArray()) {
            for (JsonNode row : aaData) {
                results.add(parseSearchResultRow(row, searchType));
            }
        }
        return results;
    }

    private BandSummary parseSearchResultRow(JsonNode row, BandSearchType searchType) {
        List<String> columnTexts = new ArrayList<>();
        Element bandAnchor = null;

        for (JsonNode column : row) {
            String html = column.asText();
            Document columnDoc = Jsoup.parseBodyFragment(html);
            columnTexts.add(columnDoc.text().trim());

            if (bandAnchor == null) {
                bandAnchor = columnDoc.selectFirst("a[href*=/bands/]");
            }
        }

        String name = bandAnchor != null
                ? bandAnchor.text().trim()
                : getColumnText(columnTexts, 0);
        String profileUrl = bandAnchor != null
                ? bandAnchor.attr("href")
                : "";

        if (searchType == BandSearchType.BAND_NAME
                || searchType == BandSearchType.MUSIC_GENRE
                || searchType == BandSearchType.THEMES) {
            String genre = getColumnText(columnTexts, 1);
            String country = getColumnText(columnTexts, 2);
            String status = getColumnText(columnTexts, 3);
            return new BandSummary(name, country, genre, status, profileUrl);
        }

        return new BandSummary(
                name,
                getColumnText(columnTexts, 1),
                getColumnText(columnTexts, 2),
                getColumnText(columnTexts, 3),
                profileUrl
        );
    }

    private String getColumnText(List<String> columnTexts, int index) {
        return index < columnTexts.size() ? columnTexts.get(index) : "";
    }

    private BandDetail parseDetails(String html, String profileUrl) {
        Document doc = Jsoup.parse(html, profileUrl);

        String bandName = "";
        Element h1 = doc.selectFirst("h1.band_name");
        if (h1 != null) bandName = h1.text();

        String country = "", location = "", status = "", formedIn = "",
               yearsActive = "", genre = "", lyricalThemes = "", label = "";

        Element statsDiv = doc.selectFirst("div#band_stats");
        if (statsDiv != null) {
            Elements dts = statsDiv.select("dt");
            Elements dds = statsDiv.select("dd");
            for (int i = 0; i < dts.size() && i < dds.size(); i++) {
                String key = dts.get(i).text().toLowerCase().replace(":", "").trim();
                String val = dds.get(i).text().trim();
                switch (key) {
                    case "country of origin" -> country       = val;
                    case "location"          -> location      = val;
                    case "status"            -> status        = val;
                    case "formed in"         -> formedIn      = val;
                    case "years active"      -> yearsActive   = val;
                    case "genre"             -> genre         = val;
                    case "lyrical themes"    -> lyricalThemes = val;
                    case "current label",
                         "last label"        -> label         = val;
                }
            }
        }

        List<BandDetail.AlbumEntry> discography = new ArrayList<>();
        Element discoSection = doc.selectFirst("div#band_disco");
        if (discoSection != null) {
            Elements rows = discoSection.select("table.discog tbody tr");
            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() >= 3) {
                    Element albumAnchor = cols.get(0).selectFirst("a");
                    String albumTitle   = albumAnchor != null ? albumAnchor.text() : cols.get(0).text();
                    String albumUrl     = albumAnchor != null ? albumAnchor.attr("abs:href") : "";
                    discography.add(new BandDetail.AlbumEntry(
                            albumTitle,
                            cols.get(1).text(),
                            cols.get(2).text(),
                            albumUrl
                    ));
                }
            }
        }

        return new BandDetail(bandName, country, location, status, formedIn,
                              yearsActive, genre, lyricalThemes, label,
                              profileUrl, discography);
    }

    @Override
    public AlbumDetail getAlbumDetails(String albumUrl) {
        try {
            Thread.sleep(400);
            page.navigate(albumUrl,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // El tracklist se carga vía AJAX; esperamos hasta que aparezca en el DOM
            try {
                page.waitForSelector("table.table_lyrics",
                        new Page.WaitForSelectorOptions().setTimeout(10_000));
            } catch (TimeoutError ignored) {
                // Sin tracklist o estructura diferente — parseamos lo que haya
            }

            return parseAlbumDetails(page.content(), albumUrl);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener álbum: " + e.getMessage(), e);
        }
    }

    private AlbumDetail parseAlbumDetails(String html, String albumUrl) {
        Document doc = Jsoup.parse(html, albumUrl);

        String title = "";
        Element h1 = doc.selectFirst("h1.album_name");
        if (h1 != null) title = h1.text();

        String type = "", releaseDate = "", label = "";
        Element infoDiv = doc.selectFirst("dl#album_info");
        if (infoDiv == null) infoDiv = doc.selectFirst("div#album_info");
        if (infoDiv != null) {
            Elements dts = infoDiv.select("dt");
            Elements dds = infoDiv.select("dd");
            for (int i = 0; i < dts.size() && i < dds.size(); i++) {
                String key = dts.get(i).text().toLowerCase().replace(":", "").trim();
                String val = dds.get(i).text().trim();
                switch (key) {
                    case "type"         -> type        = val;
                    case "release date" -> releaseDate = val;
                    case "label"        -> label       = val;
                }
            }
        }

        List<AlbumDetail.TrackEntry> tracks = new ArrayList<>();
        // Buscar la tabla directamente (el div padre puede tener ids distintos)
        Element songsTable = doc.selectFirst("table.table_lyrics");
        if (songsTable != null) {
            for (Element row : songsTable.select("tbody tr")) {
                Elements cols = row.select("td");
                if (cols.size() < 2) continue;
                String num        = cols.get(0).text().trim();
                String trackTitle = extractTrackTitle(cols.get(1));
                String duration   = cols.size() >= 3 ? cols.get(2).text().trim() : "";
                if (num.isBlank() || trackTitle.isBlank()) continue;
                tracks.add(new AlbumDetail.TrackEntry(num, trackTitle, duration));
            }
        }

        return new AlbumDetail(title, type, releaseDate, label, albumUrl, tracks);
    }

    private String extractTrackTitle(Element titleCell) {
        Element cleanedCell = titleCell.clone();
        cleanedCell.select(".lyricsButton, a[title*=lyrics], a[href*=lyrics], span[id*=lyrics]").remove();

        String trackTitle = cleanedCell.text().trim();
        if (trackTitle.endsWith("Show Lyrics")) {
            trackTitle = trackTitle.substring(0, trackTitle.length() - "Show Lyrics".length()).trim();
        }
        return trackTitle;
    }
}
