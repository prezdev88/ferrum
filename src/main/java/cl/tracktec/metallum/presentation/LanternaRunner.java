package cl.tracktec.metallum.presentation;

import cl.tracktec.metallum.core.domain.AlbumDetail;
import cl.tracktec.metallum.core.domain.BandDetail;
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
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
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

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "metallum.ui", name = "type", havingValue = "lanterna", matchIfMissing = true)
public class LanternaRunner implements MetallumUi {

    private final SearchBandsUseCase searchBands;
    private final GetBandDetailsUseCase getBandDetails;
    private final GetAlbumDetailsUseCase getAlbumDetails;
    private final ConfigurableApplicationContext applicationContext;

    private final List<BandSummary> searchResults = new ArrayList<>();
    private final List<BandDetail.AlbumEntry> selectableAlbums = new ArrayList<>();

    private MultiWindowTextGUI gui;
    private BasicWindow window;
    private TextBox queryBox;
    private Table<String> resultsTable;
    private TextBox bandDetailsBox;
    private Table<String> albumsTable;
    private TextBox albumDetailsBox;
    private boolean exitRequested;

    public LanternaRunner(SearchBandsUseCase searchBands,
                          GetBandDetailsUseCase getBandDetails,
                          GetAlbumDetailsUseCase getAlbumDetails,
                          ConfigurableApplicationContext applicationContext) {
        this.searchBands = searchBands;
        this.getBandDetails = getBandDetails;
        this.getAlbumDetails = getAlbumDetails;
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
        Panel searchPanel = new Panel(new GridLayout(4));
        searchPanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        searchPanel.addComponent(new Label("Buscar banda:"));

        queryBox = new TextBox(new TerminalSize(30, 1));
        queryBox.setLayoutData(GridLayout.createHorizontallyFilledLayoutData(1));
        searchPanel.addComponent(queryBox);

        searchPanel.addComponent(new Button("Buscar", this::search));
        searchPanel.addComponent(new Button("Salir (F4)", this::requestApplicationClose));

        Panel wrapper = new Panel(new LinearLayout(Direction.VERTICAL));
        wrapper.addComponent(searchPanel.withBorder(Borders.singleLine("Busqueda")));
        return wrapper;
    }

    private Panel createResultsPanel() {
        resultsTable = new Table<>("#", "Banda", "Pais", "Genero", "Estado");
        resultsTable.setSelectAction(this::loadSelectedBand);

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(resultsTable.withBorder(Borders.singleLine("Resultados")));
        panel.addComponent(new Button("Ver banda seleccionada", this::loadSelectedBand));
        return panel;
    }

    private Panel createDetailsPanel() {
        Panel columns = new Panel(new GridLayout(2));
        columns.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

        bandDetailsBox = createReadOnlyTextBox(new TerminalSize(50, 10));
        columns.addComponent(bandDetailsBox.withBorder(Borders.singleLine("Banda")));

        Panel rightColumn = new Panel(new LinearLayout(Direction.VERTICAL));
        albumsTable = new Table<>("#", "Ano", "Tipo", "Titulo");
        albumsTable.setSelectAction(this::loadSelectedAlbum);
        rightColumn.addComponent(albumsTable.withBorder(Borders.singleLine("Discografia")));
        rightColumn.addComponent(new Button("Ver album seleccionado", this::loadSelectedAlbum));

        albumDetailsBox = createReadOnlyTextBox(new TerminalSize(50, 6));
        rightColumn.addComponent(albumDetailsBox.withBorder(Borders.singleLine("Album")));

        columns.addComponent(rightColumn);
        return columns;
    }

    private Panel createFooter() {
        Panel footer = new Panel(new LinearLayout(Direction.VERTICAL));
        footer.addComponent(new Label(
                "F4 cierra la aplicacion | Enter ejecuta la accion del foco | ESC cierra dialogos | Ctrl+C termina la app"
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
            searchResults.addAll(searchBands.execute(query));
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
            bandDetailsBox.setText(formatBandDetails(detail));
            refreshAlbumsTable(detail.discography());
            albumDetailsBox.setText("");
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
                albumsTable.getTableModel().addRow("-", album.year(), album.type(), album.title());
                continue;
            }

            selectableAlbums.add(album);
            albumsTable.getTableModel().addRow(
                    String.valueOf(albumNumber++),
                    album.year(),
                    album.type(),
                    album.title()
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
            albumDetailsBox.setText(formatAlbumDetails(detail));
        } catch (RuntimeException e) {
            showError("Error al cargar album", e.getMessage());
        }
    }

    private void clearBandDetails() {
        bandDetailsBox.setText("");
        albumDetailsBox.setText("");
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

    private String formatAlbumDetails(AlbumDetail detail) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, detail.title());
        appendField(builder, "Tipo", detail.type());
        appendField(builder, "Fecha", detail.releaseDate());
        appendField(builder, "Sello", detail.label());
        appendField(builder, "URL", detail.url());

        if (!detail.tracks().isEmpty()) {
            builder.append(System.lineSeparator()).append("Tracklist").append(System.lineSeparator());
            for (AlbumDetail.TrackEntry track : detail.tracks()) {
                builder.append(track.number())
                        .append(" ")
                        .append(track.title());
                if (!track.duration().isBlank()) {
                    builder.append(" [").append(track.duration()).append("]");
                }
                builder.append(System.lineSeparator());
            }
        }

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

    private void showError(String title, String message) {
        MessageDialog.showMessageDialog(gui, title, message, MessageDialogButton.OK);
    }

    private void showInfo(String title, String message) {
        MessageDialog.showMessageDialog(gui, title, message, MessageDialogButton.OK);
    }
}
