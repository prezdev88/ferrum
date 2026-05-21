package cl.tracktec.metallum.presentation;

import cl.tracktec.metallum.core.domain.AlbumDetail;
import cl.tracktec.metallum.core.domain.BandDetail;
import cl.tracktec.metallum.core.domain.BandSearchType;
import cl.tracktec.metallum.core.domain.BandSummary;
import cl.tracktec.metallum.core.usecase.GetAlbumDetailsUseCase;
import cl.tracktec.metallum.core.usecase.GetBandDetailsUseCase;
import cl.tracktec.metallum.core.usecase.SearchBandsUseCase;
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
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "metallum.ui", name = "type", havingValue = "lanterna", matchIfMissing = true)
public class LanternaRunner implements MetallumUi {

    private final SearchBandsUseCase searchBands;
    private final GetBandDetailsUseCase getBandDetails;
    private final GetAlbumDetailsUseCase getAlbumDetails;
    private final MetallumUiProperties metallumUiProperties;
    private final ConfigurableApplicationContext applicationContext;

    private final List<BandSummary> searchResults = new ArrayList<>();
    private final List<BandDetail.AlbumEntry> selectableAlbums = new ArrayList<>();

    private MultiWindowTextGUI gui;
    private BasicWindow window;
    private TextBox queryBox;
    private ComboBox<BandSearchType> searchTypeBox;
    private Table<String> resultsTable;
    private Table<String> albumsTable;
    private boolean exitRequested;
    private BandDetail selectedBandDetail;

    public LanternaRunner(SearchBandsUseCase searchBands,
                          GetBandDetailsUseCase getBandDetails,
                          GetAlbumDetailsUseCase getAlbumDetails,
                          MetallumUiProperties metallumUiProperties,
                          ConfigurableApplicationContext applicationContext) {
        this.searchBands = searchBands;
        this.getBandDetails = getBandDetails;
        this.getAlbumDetails = getAlbumDetails;
        this.metallumUiProperties = metallumUiProperties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();

        try (Screen screen = new TerminalScreen(terminalFactory.createTerminal())) {
            screen.startScreen();
            screen.setCursorPosition(null);

            gui = new MultiWindowTextGUI(screen);
            window = new BasicWindow("Metallum | F4 cerrar") {
                @Override
                public boolean handleInput(KeyStroke keyStroke) {
                    if (keyStroke.getKeyType() == KeyType.F3) {
                        queryBox.takeFocus();
                        return true;
                    }
                    if (keyStroke.getKeyType() == KeyType.F4) {
                        requestApplicationClose();
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
        header.addComponent(new Label("METALLUM")
                .addStyle(SGR.BOLD)
                .setForegroundColor(TextColor.ANSI.RED));
        header.addComponent(new Label("Encyclopaedia Metallum terminal UI")
                .setForegroundColor(TextColor.ANSI.WHITE_BRIGHT));
        return header;
    }

    private Panel createSearchPanel() {
        Panel searchPanel = new Panel(new GridLayout(3));
        searchPanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        searchPanel.addComponent(new Label("Buscar banda:"));

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

        searchTypeBox = new ComboBox<>(BandSearchType.values());
        searchTypeBox.setReadOnly(true);
        searchTypeBox.setDropDownNumberOfRows(BandSearchType.values().length);
        searchTypeBox.setSelectedItem(BandSearchType.BAND_NAME);
        searchPanel.addComponent(searchTypeBox);

        Panel wrapper = new Panel(new LinearLayout(Direction.VERTICAL));
        wrapper.addComponent(searchPanel.withBorder(Borders.singleLine("Busqueda")));
        return wrapper;
    }

    private Panel createResultsPanel() {
        resultsTable = new Table<>("#", "Resultado", "Dato 1", "Dato 2", "Dato 3");
        resultsTable.setSelectAction(this::loadSelectedBand);

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(resultsTable.withBorder(Borders.singleLine("Resultados")));
        return panel;
    }

    private Panel createDetailsPanel() {
        Panel details = new Panel(new LinearLayout(Direction.VERTICAL));
        albumsTable = new Table<>("#", "Ano", "Titulo", "Tipo");
        albumsTable.setSelectAction(this::loadSelectedAlbum);
        details.addComponent(albumsTable.withBorder(Borders.singleLine("Discografia")));
        return details;
    }

    private Panel createFooter() {
        Panel footer = new Panel(new LinearLayout(Direction.VERTICAL));
        footer.addComponent(new Label(
                "F3 enfoca buscar | F4 cierra la aplicacion | Enter ejecuta la accion del foco | ESC cierra dialogos | Ctrl+C termina la app"
        ).setForegroundColor(TextColor.ANSI.WHITE));
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
            showInfo("Busqueda", "Escribe un nombre de banda.");
            return;
        }

        try {
            searchResults.clear();
            searchResults.addAll(searchBands.execute(query, searchTypeBox.getSelectedItem()));
            refreshResultsTable();
            clearBandDetails();

            if (searchResults.isEmpty()) {
                showInfo("Sin resultados", "No se encontraron bandas con ese nombre.");
            }
        } catch (RuntimeException e) {
            showError("Error al buscar", e.getMessage());
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
                    band.genre(),
                    band.status()
            );
        }
    }

    private void loadSelectedBand() {
        int selectedRow = resultsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= searchResults.size()) {
            showInfo("Banda", "Selecciona una banda primero.");
            return;
        }

        BandSummary selectedBand = searchResults.get(selectedRow);
        if (selectedBand.profileUrl().isBlank()) {
            showInfo("Banda", "La banda seleccionada no tiene pagina disponible.");
            return;
        }

        try {
            BandDetail detail = getBandDetails.execute(selectedBand.profileUrl());
            selectedBandDetail = detail;
            refreshAlbumsTable(detail.discography());
        } catch (RuntimeException e) {
            showError("Error al cargar banda", e.getMessage());
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
            showInfo("Album", "Selecciona un album primero.");
            return;
        }

        String albumNumber = albumsTable.getTableModel().getRow(selectedRow).get(0);
        if ("-".equals(albumNumber)) {
            showInfo("Album", "Ese item no tiene pagina de album.");
            return;
        }

        int index = Integer.parseInt(albumNumber) - 1;
        if (index < 0 || index >= selectableAlbums.size()) {
            showInfo("Album", "No se pudo resolver el album seleccionado.");
            return;
        }

        try {
            AlbumDetail detail = getAlbumDetails.execute(selectableAlbums.get(index).url());
            showAlbumWindow(detail);
        } catch (RuntimeException e) {
            showError("Error al cargar album", e.getMessage());
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
        appendField(builder, "Pais", detail.country());
        appendField(builder, "Localidad", detail.location());
        appendField(builder, "Estado", detail.status());
        appendField(builder, "Formacion", detail.formedIn());
        appendField(builder, "Anos activo", detail.yearsActive());
        appendField(builder, "Genero", detail.genre());
        appendField(builder, "Tematicas", detail.lyricalThemes());
        appendField(builder, "Sello", detail.label());
        appendField(builder, "URL", detail.profileUrl());
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
        panel.addComponent(new Button("Cerrar", detailWindow::close));

        detailWindow.setComponent(panel);
        gui.addWindowAndWait(detailWindow);
    }

    private void showAlbumWindow(AlbumDetail detail) {
        BasicWindow detailWindow = new BasicWindow("Album");
        detailWindow.setHints(List.of(Window.Hint.MODAL, Window.Hint.CENTERED));
        detailWindow.setCloseWindowWithEscape(true);

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label(detail.title()).addStyle(SGR.BOLD));
        panel.addComponent(new Label("Tipo: " + detail.type()));
        panel.addComponent(new Label("Fecha: " + detail.releaseDate()));
        panel.addComponent(new Label("Sello: " + detail.label()));
        panel.addComponent(new Label("URL: " + detail.url()));

        Table<String> tracksTable = new Table<>("#", "Titulo", "Duracion");
        for (AlbumDetail.TrackEntry track : detail.tracks()) {
            tracksTable.getTableModel().addRow(
                    track.number(),
                    track.title(),
                    track.duration().isBlank() ? "-" : track.duration()
            );
        }
        tracksTable.setSelectAction(() -> openSelectedTrack(detail, tracksTable.getSelectedRow()));

        panel.addComponent(tracksTable.withBorder(Borders.singleLine("Tracklist")));
        Panel actions = new Panel(new GridLayout(2));
        actions.addComponent(new Button("Abrir track", () -> openSelectedTrack(detail, tracksTable.getSelectedRow())));
        actions.addComponent(new Button("Cerrar", detailWindow::close));
        panel.addComponent(actions);

        detailWindow.setComponent(panel);
        gui.addWindowAndWait(detailWindow);
    }

    private void openSelectedTrack(AlbumDetail albumDetail, int selectedRow) {
        if (selectedRow < 0 || selectedRow >= albumDetail.tracks().size()) {
            showInfo("Track", "Selecciona un track primero.");
            return;
        }

        AlbumDetail.TrackEntry track = albumDetail.tracks().get(selectedRow);
        String bandName = selectedBandDetail != null ? selectedBandDetail.name() : "";
        String query = buildTrackSearchQuery(bandName, albumDetail.title(), track.title());
        String searchUrl = buildMusicSearchUrl(query);

        try {
            openInBrowser(searchUrl);
        } catch (Exception e) {
            showError("Error al abrir navegador", e.getMessage());
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
        return switch (metallumUiProperties.getMusicSearchProvider()) {
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
}
