package cl.tracktec.ferrum.presentation;

import cl.tracktec.ferrum.core.domain.AlbumDetail;
import cl.tracktec.ferrum.core.domain.BandDetail;
import cl.tracktec.ferrum.core.domain.BandSearchType;
import cl.tracktec.ferrum.core.domain.BandSummary;
import cl.tracktec.ferrum.core.usecase.GetAlbumDetailsUseCase;
import cl.tracktec.ferrum.core.usecase.GetBandDetailsUseCase;
import cl.tracktec.ferrum.core.usecase.SearchBandsUseCase;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.Interactable.Result;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "ferrum.ui", name = "type", havingValue = "lanterna", matchIfMissing = true)
public class LanternaRunner implements FerrumUi {

    private final SearchBandsUseCase searchBands;
    private final GetBandDetailsUseCase getBandDetails;
    private final GetAlbumDetailsUseCase getAlbumDetails;
    private final FerrumUiProperties ferrumUiProperties;
    private final ConfigurableApplicationContext applicationContext;

    private final List<BandSummary> searchResults = new ArrayList<>();
    private final List<BandDetail.AlbumEntry> selectableAlbums = new ArrayList<>();

    private MultiWindowTextGUI gui;
    private BasicWindow window;
    private TextBox queryBox;
    private ComboBox<SearchTypeOption> searchTypeBox;
    private Table<String> resultsTable;
    private Table<String> albumsTable;
    private boolean exitRequested;
    private BandDetail selectedBandDetail;
    private I18n i18n;

    public LanternaRunner(SearchBandsUseCase searchBands,
                          GetBandDetailsUseCase getBandDetails,
                          GetAlbumDetailsUseCase getAlbumDetails,
                          FerrumUiProperties ferrumUiProperties,
                          ConfigurableApplicationContext applicationContext) {
        this.searchBands = searchBands;
        this.getBandDetails = getBandDetails;
        this.getAlbumDetails = getAlbumDetails;
        this.ferrumUiProperties = ferrumUiProperties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();

        try (Screen screen = new TerminalScreen(terminalFactory.createTerminal())) {
            screen.startScreen();
            screen.setCursorPosition(null);

            gui = new MultiWindowTextGUI(screen);
            initI18n();

            window = new BasicWindow(i18n.t("app.window_title")) {
                @Override
                public boolean handleInput(KeyStroke keyStroke) {
                    if (keyStroke.getKeyType() == KeyType.F2) {
                        toggleLanguage();
                        return true;
                    }
                    if (keyStroke.getKeyType() == KeyType.F3) {
                        queryBox.takeFocus();
                        return true;
                    }
                    if (keyStroke.getKeyType() == KeyType.F4) {
                        requestApplicationClose();
                        return true;
                    }
                    if (keyStroke.getKeyType() == KeyType.Escape) {
                        confirmExit();
                        return true;
                    }
                    return super.handleInput(keyStroke);
                }
            };
            window.setHints(List.of(Window.Hint.EXPANDED));
            window.setComponent(buildContent());

            if (args.length > 0) {
                queryBox.setText(String.join(" ", args).trim());
                search();
            }

            gui.addWindowAndWait(window);
        }

        if (exitRequested) {
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        }
    }

    private Panel buildContent() {
        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(createHeader());
        root.addComponent(createSearchPanel());
        root.addComponent(createResultsPanel());
        root.addComponent(createDetailsPanel());
        root.addComponent(createFooter());
        return root;
    }

    private Panel createHeader() {
        Panel header = new Panel(new LinearLayout(Direction.VERTICAL));
        header.addComponent(new Label(i18n.t("app.header_title"))
                .addStyle(SGR.BOLD)
                .setForegroundColor(TextColor.ANSI.RED));
        header.addComponent(new Label(i18n.t("app.header_subtitle"))
                .setForegroundColor(TextColor.ANSI.WHITE_BRIGHT));
        return header;
    }

    private Panel createSearchPanel() {
        Panel searchPanel = new Panel(new GridLayout(3));
        searchPanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

        queryBox = new TextBox(new TerminalSize(30, 1)) {
            @Override
            public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
                if (keyStroke.getKeyType() == KeyType.Enter) {
                    search();
                    return Result.HANDLED;
                }
                return super.handleKeyStroke(keyStroke);
            }
        };
        queryBox.setLayoutData(GridLayout.createHorizontallyFilledLayoutData(1));
        searchPanel.addComponent(queryBox);

        searchTypeBox = new ComboBox<>(buildSearchTypeOptions());
        searchTypeBox.setReadOnly(true);
        searchTypeBox.setDropDownNumberOfRows(BandSearchType.values().length);
        searchTypeBox.setSelectedItem(SearchTypeOption.of(BandSearchType.BAND_NAME, i18n));
        searchPanel.addComponent(searchTypeBox);

        Panel wrapper = new Panel(new LinearLayout(Direction.VERTICAL));
        wrapper.addComponent(searchPanel.withBorder(Borders.singleLine(i18n.t("search.border"))));
        return wrapper;
    }

    private Panel createResultsPanel() {
        resultsTable = new Table<>(
                i18n.t("results.col.index"),
                i18n.t("results.col.band"),
                i18n.t("results.col.country"),
                i18n.t("results.col.genre")
        );
        resultsTable.setSelectAction(this::loadSelectedBand);

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(resultsTable.withBorder(Borders.singleLine(i18n.t("results.border"))));
        return panel;
    }

    private Panel createDetailsPanel() {
        Panel details = new Panel(new LinearLayout(Direction.VERTICAL));
        albumsTable = new Table<>(
                i18n.t("albums.col.index"),
                i18n.t("albums.col.year"),
                i18n.t("albums.col.title"),
                i18n.t("albums.col.type")
        );
        albumsTable.setSelectAction(this::loadSelectedAlbum);
        details.addComponent(albumsTable.withBorder(Borders.singleLine(i18n.t("details.border"))));
        return details;
    }

    private Panel createFooter() {
        Panel footer = new Panel(new LinearLayout(Direction.VERTICAL));
        footer.addComponent(new Label(i18n.t("footer.help"))
                .setForegroundColor(TextColor.ANSI.WHITE));
        return footer;
    }

    private void requestApplicationClose() {
        exitRequested = true;
        window.close();
    }

    private TextBox createReadOnlyTextBox(TerminalSize size) {
        TextBox textBox = new TextBox(size, "", TextBox.Style.MULTI_LINE);
        textBox.setReadOnly(true);
        return textBox;
    }

    private void search() {
        String query = queryBox.getText().trim();
        if (query.isBlank()) {
            showInfo(i18n.t("dialog.search.title"), i18n.t("dialog.search.empty"));
            return;
        }

        try {
            searchResults.clear();
            searchResults.addAll(searchBands.execute(query, searchTypeBox.getSelectedItem().type));
            refreshResultsTable();
            clearBandDetails();

            if (searchResults.isEmpty()) {
                showInfo(i18n.t("dialog.search.no_results_title"), i18n.t("dialog.search.no_results"));
            } else {
                resultsTable.takeFocus();
            }
        } catch (RuntimeException e) {
            showError(i18n.t("dialog.search_error"), e.getMessage());
        }
    }

    private void refreshResultsTable() {
        resultsTable.getTableModel().clear();
        for (int i = 0; i < searchResults.size(); i++) {
            BandSummary band = searchResults.get(i);
            resultsTable.getTableModel().addRow(
                    String.valueOf(i + 1),
                    band.name(),
                    band.country(),
                    band.genre()
            );
        }
    }

    private void loadSelectedBand() {
        int selectedRow = resultsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= searchResults.size()) {
            showInfo(i18n.t("dialog.band.title"), i18n.t("dialog.band.select_first"));
            return;
        }

        BandSummary selectedBand = searchResults.get(selectedRow);
        if (selectedBand.profileUrl().isBlank()) {
            showInfo(i18n.t("dialog.band.title"), i18n.t("dialog.band.no_page"));
            return;
        }

        try {
            BandDetail detail = getBandDetails.execute(selectedBand.profileUrl());
            selectedBandDetail = detail;
            refreshAlbumsTable(detail.discography());
        } catch (RuntimeException e) {
            showError(i18n.t("dialog.band.load_error"), e.getMessage());
        }
    }

    private void refreshAlbumsTable(List<BandDetail.AlbumEntry> discography) {
        selectableAlbums.clear();
        albumsTable.getTableModel().clear();

        int albumNumber = 1;
        for (BandDetail.AlbumEntry album : discography) {
            if (album.url().isBlank()) {
                albumsTable.getTableModel().addRow("-", album.year(), album.title(), album.type());
                continue;
            }

            selectableAlbums.add(album);
            albumsTable.getTableModel().addRow(
                    String.valueOf(albumNumber++),
                    album.year(),
                    album.title(),
                    album.type()
            );
        }
    }

    private void loadSelectedAlbum() {
        int selectedRow = albumsTable.getSelectedRow();
        if (selectedRow < 0) {
            showInfo(i18n.t("dialog.album.title"), i18n.t("dialog.album.select_first"));
            return;
        }

        String albumNumber = albumsTable.getTableModel().getRow(selectedRow).get(0);
        if ("-".equals(albumNumber)) {
            showInfo(i18n.t("dialog.album.title"), i18n.t("dialog.album.no_page"));
            return;
        }

        int index = Integer.parseInt(albumNumber) - 1;
        if (index < 0 || index >= selectableAlbums.size()) {
            showInfo(i18n.t("dialog.album.title"), i18n.t("dialog.album.resolve_error"));
            return;
        }

        try {
            AlbumDetail detail = getAlbumDetails.execute(selectableAlbums.get(index).url());
            showAlbumWindow(detail);
        } catch (RuntimeException e) {
            showError(i18n.t("dialog.album.load_error"), e.getMessage());
        }
    }

    private void clearBandDetails() {
        selectedBandDetail = null;
        selectableAlbums.clear();
        albumsTable.getTableModel().clear();
    }

    private String formatBandDetails(BandDetail detail) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, detail.name());
        appendField(builder, i18n.t("band.field.country"), detail.country());
        appendField(builder, i18n.t("band.field.location"), detail.location());
        appendField(builder, i18n.t("band.field.status"), detail.status());
        appendField(builder, i18n.t("band.field.formed_in"), detail.formedIn());
        appendField(builder, i18n.t("band.field.years_active"), detail.yearsActive());
        appendField(builder, i18n.t("band.field.genre"), detail.genre());
        appendField(builder, i18n.t("band.field.themes"), detail.lyricalThemes());
        appendField(builder, i18n.t("band.field.label"), detail.label());
        appendField(builder, i18n.t("band.field.url"), detail.profileUrl());
        return builder.toString();
    }

    private void appendField(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label)
                .append(": ")
                .append(value)
                .append(System.lineSeparator());
    }

    private void appendLine(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(value)
                .append(System.lineSeparator())
                .append(System.lineSeparator());
    }

    private void showTextWindow(String title, String content, TerminalSize size) {
        BasicWindow detailWindow = new BasicWindow(title);
        detailWindow.setHints(List.of(Window.Hint.MODAL, Window.Hint.CENTERED));
        detailWindow.setCloseWindowWithEscape(true);

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        TextBox contentBox = createReadOnlyTextBox(size);
        contentBox.setText(content);
        panel.addComponent(contentBox.withBorder(Borders.singleLine(title)));
        panel.addComponent(new Button(i18n.t("button.close"), detailWindow::close));

        detailWindow.setComponent(panel);
        gui.addWindowAndWait(detailWindow);
    }

    private void showAlbumWindow(AlbumDetail detail) {
        BasicWindow detailWindow = new BasicWindow(i18n.t("dialog.album.title"));
        detailWindow.setHints(List.of(Window.Hint.MODAL, Window.Hint.CENTERED));
        detailWindow.setCloseWindowWithEscape(true);

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label(detail.title()).addStyle(SGR.BOLD));
        panel.addComponent(new Label(i18n.t("album.field.type", detail.type())));
        panel.addComponent(new Label(i18n.t("album.field.date", detail.releaseDate())));
        panel.addComponent(new Label(i18n.t("album.field.label", detail.label())));
        panel.addComponent(new Label(i18n.t("album.field.url", detail.url())));

        Table<String> tracksTable = new Table<>(
                i18n.t("tracks.col.index"),
                i18n.t("tracks.col.title"),
                i18n.t("tracks.col.duration")
        );
        for (AlbumDetail.TrackEntry track : detail.tracks()) {
            tracksTable.getTableModel().addRow(
                    track.number(),
                    track.title(),
                    track.duration().isBlank() ? "-" : track.duration()
            );
        }
        tracksTable.setSelectAction(() -> openSelectedTrack(detail, tracksTable.getSelectedRow()));

        panel.addComponent(tracksTable.withBorder(Borders.singleLine(i18n.t("tracks.border"))));
        Panel actions = new Panel(new GridLayout(2));
        actions.addComponent(new Button(i18n.t("button.open_track"), () -> openSelectedTrack(detail, tracksTable.getSelectedRow())));
        actions.addComponent(new Button(i18n.t("button.close"), detailWindow::close));
        panel.addComponent(actions);

        detailWindow.setComponent(panel);
        gui.addWindowAndWait(detailWindow);
    }

    private void openSelectedTrack(AlbumDetail albumDetail, int selectedRow) {
        if (selectedRow < 0 || selectedRow >= albumDetail.tracks().size()) {
            showInfo(i18n.t("dialog.track.title"), i18n.t("dialog.track.select_first"));
            return;
        }

        AlbumDetail.TrackEntry track = albumDetail.tracks().get(selectedRow);
        String bandName = selectedBandDetail != null ? selectedBandDetail.name() : "";
        String query = buildTrackSearchQuery(bandName, albumDetail.title(), track.title());
        String searchUrl = buildMusicSearchUrl(query);

        try {
            openInBrowser(searchUrl);
        } catch (Exception e) {
            showError(i18n.t("dialog.browser_error"), e.getMessage());
        }
    }

    private String buildTrackSearchQuery(String bandName, String albumTitle, String trackTitle) {
        return String.join(" ",
                List.of(bandName, trackTitle, albumTitle).stream()
                        .filter(value -> value != null && !value.isBlank())
                        .toList()
        );
    }

    private String buildMusicSearchUrl(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return switch (ferrumUiProperties.getMusicSearchProvider()) {
            case YOUTUBE -> "https://www.youtube.com/results?search_query=" + encodedQuery;
            case YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=" + encodedQuery;
        };
    }

    private void openInBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
            return;
        }

        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            new ProcessBuilder("open", url)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } else if (osName.contains("win")) {
            new ProcessBuilder("cmd", "/c", "start", "", url)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } else {
            new ProcessBuilder("xdg-open", url)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        }
    }

    private void showError(String title, String message) {
        MessageDialog.showMessageDialog(gui, title, message, MessageDialogButton.OK);
    }

    private void showInfo(String title, String message) {
        MessageDialog.showMessageDialog(gui, title, message, MessageDialogButton.OK);
    }

    private void confirmExit() {
        BasicWindow confirm = new BasicWindow(i18n.t("dialog.exit.title"));
        confirm.setHints(List.of(Window.Hint.MODAL, Window.Hint.CENTERED));
        confirm.setCloseWindowWithEscape(true);

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label(i18n.t("dialog.exit.message")));

        Panel actions = new Panel(new GridLayout(2));
        Button yes = new Button(i18n.t("button.yes"), () -> {
            confirm.close();
            requestApplicationClose();
        });
        Button no = new Button(i18n.t("button.no"), confirm::close);
        actions.addComponent(yes);
        actions.addComponent(no);
        panel.addComponent(actions);

        confirm.setComponent(panel);
        gui.addWindowAndWait(confirm);
    }

    private void initI18n() {
        i18n = new I18n("i18n.messages", toLocale(ferrumUiProperties.getLanguage()));
    }

    private void toggleLanguage() {
        Locale next = "es".equalsIgnoreCase(i18n.getLocale().getLanguage()) ? Locale.ENGLISH : new Locale("es");
        i18n.setLocale(next);
        rebuildUi();
    }

    private Locale toLocale(String language) {
        if (language == null || language.isBlank()) return Locale.ENGLISH;
        String l = language.trim().toLowerCase();
        return "en".equals(l) ? Locale.ENGLISH : new Locale("es");
    }

    private void rebuildUi() {
        // Preserve minimal state across rebuilds (borders/titles are not easily mutable).
        String query = queryBox != null ? queryBox.getText() : "";
        BandSearchType selectedType = searchTypeBox != null && searchTypeBox.getSelectedItem() != null
                ? searchTypeBox.getSelectedItem().type
                : BandSearchType.BAND_NAME;

        window.setTitle(i18n.t("app.window_title"));
        window.setComponent(buildContent());

        queryBox.setText(query);
        searchTypeBox.setSelectedItem(SearchTypeOption.of(selectedType, i18n));

        refreshResultsTable();
        if (selectedBandDetail != null) {
            refreshAlbumsTable(selectedBandDetail.discography());
        } else {
            clearBandDetails();
        }
    }

    private List<SearchTypeOption> buildSearchTypeOptions() {
        List<SearchTypeOption> options = new ArrayList<>();
        for (BandSearchType type : BandSearchType.values()) {
            options.add(SearchTypeOption.of(type, i18n));
        }
        return options;
    }

    private static final class SearchTypeOption {
        private final BandSearchType type;
        private final String label;

        private SearchTypeOption(BandSearchType type, String label) {
            this.type = type;
            this.label = label;
        }

        static SearchTypeOption of(BandSearchType type, I18n i18n) {
            // Fallback to the enum's own label if the key is missing.
            String key = "searchType." + type.name();
            String localized = i18n.t(key);
            if (localized.startsWith("??") && localized.endsWith("??")) {
                localized = type.toString();
            }
            return new SearchTypeOption(type, localized);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
