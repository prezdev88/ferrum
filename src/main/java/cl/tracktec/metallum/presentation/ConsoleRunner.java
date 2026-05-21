package cl.tracktec.metallum.presentation;

import cl.tracktec.metallum.core.domain.AlbumDetail;
import cl.tracktec.metallum.core.domain.BandDetail;
import cl.tracktec.metallum.core.domain.BandSearchType;
import cl.tracktec.metallum.core.domain.BandSummary;
import cl.tracktec.metallum.core.usecase.GetAlbumDetailsUseCase;
import cl.tracktec.metallum.core.usecase.GetBandDetailsUseCase;
import cl.tracktec.metallum.core.usecase.SearchBandsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
@ConditionalOnProperty(prefix = "metallum.ui", name = "type", havingValue = "console")
public class ConsoleRunner implements MetallumUi {

    private static final String RESET   = "[0m";
    private static final String BOLD    = "[1m";
    private static final String DIM     = "[2m";
    private static final String RED     = "[31m";
    private static final String GREEN   = "[32m";
    private static final String YELLOW  = "[33m";
    private static final String CYAN    = "[36m";
    private static final String MAGENTA = "[35m";
    private static final String WHITE   = "[37m";

    private final SearchBandsUseCase    searchBands;
    private final GetBandDetailsUseCase getBandDetails;
    private final GetAlbumDetailsUseCase getAlbumDetails;
    private final Scanner               scanner = new Scanner(System.in);

    public ConsoleRunner(SearchBandsUseCase searchBands,
                         GetBandDetailsUseCase getBandDetails,
                         GetAlbumDetailsUseCase getAlbumDetails) {
        this.searchBands    = searchBands;
        this.getBandDetails = getBandDetails;
        this.getAlbumDetails = getAlbumDetails;
    }

    @Override
    public void run(String... args) {
        printBanner();

        // Primer query puede venir como argumento de línea de comandos
        String initialQuery = args.length > 0 ? String.join(" ", args).trim() : null;

        while (true) {
            String query = (initialQuery != null) ? initialQuery : promptQuery();
            initialQuery = null; // solo se usa en la primera iteración

            if (query == null) break; // Ctrl+C o EOF
            if (query.isBlank()) {
                System.out.println(DIM + "Escribe un nombre para buscar, o Ctrl+C para salir." + RESET);
                continue;
            }

            System.out.printf("%nBuscando %s\"%s\"%s en Encyclopaedia Metallum...%n%n",
                    BOLD + CYAN, query, RESET);

            List<BandSummary> results;
            try {
                results = searchBands.execute(query, BandSearchType.BAND_NAME);
            } catch (RuntimeException e) {
                System.out.println(RED + "Error: " + e.getMessage() + RESET);
                continue;
            }

            if (results.isEmpty()) {
                System.out.println(YELLOW + "No se encontraron bandas con ese nombre." + RESET);
                continue;
            }

            printSearchResults(results);

            int choice = promptChoice(results.size());
            if (choice < 0) continue; // Enter en blanco → volver a buscar

            BandSummary selected = results.get(choice);
            if (selected.profileUrl().isBlank()) {
                System.out.println(YELLOW + "Esta banda no tiene página disponible." + RESET);
                continue;
            }

            System.out.printf("%nCargando información de %s%s%s...%n%n",
                    BOLD + MAGENTA, selected.name(), RESET);

            BandDetail detail;
            try {
                detail = getBandDetails.execute(selected.profileUrl());
            } catch (RuntimeException e) {
                System.out.println(RED + "Error: " + e.getMessage() + RESET);
                continue;
            }

            printBandDetail(detail);

            // ── selección de álbum ────────────────────────────────────────────
            List<BandDetail.AlbumEntry> albums = detail.discography().stream()
                    .filter(a -> !a.url().isBlank())
                    .toList();

            if (!albums.isEmpty()) {
                int albumChoice = promptAlbumChoice(albums.size());
                while (albumChoice >= 0) {
                    BandDetail.AlbumEntry picked = albums.get(albumChoice);
                    System.out.printf("%nCargando álbum %s%s%s...%n%n",
                            BOLD + YELLOW, picked.title(), RESET);
                    try {
                        printAlbumDetail(getAlbumDetails.execute(picked.url()));
                    } catch (RuntimeException e) {
                        System.out.println(RED + "Error: " + e.getMessage() + RESET);
                    }
                    albumChoice = promptAlbumChoice(albums.size());
                }
            }
        }

        System.out.println(DIM + "\n¡Hasta luego!" + RESET);
    }

    private String promptQuery() {
        System.out.printf("%n%sBuscar banda%s (Enter para salir): ",
                BOLD, RESET);
        try {
            String line = scanner.nextLine().trim();
            return line.isBlank() ? null : line;
        } catch (Exception e) {
            return null;
        }
    }

    private void printSearchResults(List<BandSummary> results) {
        int colName    = Math.max(25, results.stream().mapToInt(b -> b.name().length()).max().orElse(25) + 2);
        int colCountry = 20;
        int colGenre   = 35;
        int colStatus  = 12;
        int total      = 5 + colName + colCountry + colGenre + colStatus;

        String header = String.format("%-4s %-" + colName + "s %-" + colCountry + "s %-" + colGenre + "s %s",
                "#", "Banda", "País", "Género", "Estado");

        System.out.println(BOLD + WHITE + header + RESET);
        System.out.println(DIM + "─".repeat(total) + RESET);

        for (int i = 0; i < results.size(); i++) {
            BandSummary b = results.get(i);
            System.out.printf(
                    DIM + "%-4d" + RESET +
                    BOLD + CYAN + "%-" + colName + "s" + RESET +
                    "%-" + colCountry + "s" +
                    DIM + "%-" + colGenre + "s" + RESET +
                    "%s%s%s%n",
                    i + 1,
                    truncate(b.name(), colName - 1),
                    truncate(b.country(), colCountry - 1),
                    truncate(b.genre(), colGenre - 1),
                    statusColor(b.status()), b.status(), RESET
            );
        }
        System.out.println(DIM + "─".repeat(total) + RESET);
        System.out.printf(DIM + "%d resultado(s) encontrado(s)%n" + RESET, results.size());
    }

    private int promptChoice(int max) {
        while (true) {
            System.out.printf("%n%sSelecciona una banda [1-%d] (Enter para nueva búsqueda): %s",
                    BOLD, max, RESET);
            try {
                String raw = scanner.nextLine().trim();
                if (raw.isBlank()) return -1;
                int choice = Integer.parseInt(raw);
                if (choice >= 1 && choice <= max) return choice - 1;
                System.out.printf(YELLOW + "Opción inválida, elige entre 1 y %d.%n" + RESET, max);
            } catch (NumberFormatException e) {
                System.out.println(YELLOW + "Ingresa un número válido." + RESET);
            } catch (Exception e) {
                return -1;
            }
        }
    }

    private void printBandDetail(BandDetail d) {
        int width = 60;
        String border = "═".repeat(width);

        System.out.println(BOLD + MAGENTA + "╔" + border + "╗" + RESET);
        System.out.println(BOLD + MAGENTA + "║" + RESET +
                center(d.name(), width) +
                BOLD + MAGENTA + "║" + RESET);
        System.out.println(BOLD + MAGENTA + "╠" + border + "╣" + RESET);

        printField("País de origen",  d.country(),       width);
        printField("Localidad",       d.location(),      width);
        printField("Estado",          colorStatus(d.status(), d.status()), width);
        printField("Formación",       d.formedIn(),      width);
        printField("Años activo",     d.yearsActive(),   width);
        printField("Género",          d.genre(),         width);
        printField("Temáticas",       d.lyricalThemes(), width);
        printField("Sello",           d.label(),         width);

        System.out.println(BOLD + MAGENTA + "╠" + border + "╣" + RESET);
        System.out.println(BOLD + MAGENTA + "║" + RESET +
                center("DISCOGRAFÍA", width) +
                BOLD + MAGENTA + "║" + RESET);
        System.out.println(BOLD + MAGENTA + "╠" + border + "╣" + RESET);

        if (d.discography().isEmpty()) {
            printField("", DIM + "Sin discografía disponible" + RESET, width);
        } else {
            int albumNumber = 1;
            for (BandDetail.AlbumEntry e : d.discography()) {
                String marker = e.url().isBlank()
                        ? " - "
                        : String.format("%2d.", albumNumber++);
                String line = String.format("%s %-6s %-32s %s", marker, e.year(), e.title(), e.type());
                printField("", line, width);
            }
        }

        System.out.println(BOLD + MAGENTA + "╠" + border + "╣" + RESET);
        printField("URL", DIM + d.profileUrl() + RESET, width);
        System.out.println(BOLD + MAGENTA + "╚" + border + "╝" + RESET);
    }

    private void printField(String label, String value, int width) {
        if (value == null || value.isBlank()) return;
        String raw = label.isBlank()
                ? "  " + value
                : String.format("  %s%-16s%s %s", BOLD, label + ":", RESET, value);
        // Para líneas largas, imprimir sin padding fijo
        System.out.println(BOLD + MAGENTA + "║" + RESET + " " + raw);
    }

    private String center(String text, int width) {
        int padding = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(padding) + BOLD + text + RESET + " ".repeat(padding);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private String statusColor(String status) {
        if (status == null) return WHITE;
        return switch (status.toLowerCase()) {
            case "active"     -> GREEN;
            case "split-up"   -> RED;
            case "on hold"    -> YELLOW;
            default           -> DIM;
        };
    }

    private String colorStatus(String label, String value) {
        return statusColor(value) + value + RESET;
    }

    private int promptAlbumChoice(int max) {
        while (true) {
            System.out.printf("%n%sVer álbum [1-%d] (Enter para nueva búsqueda): %s",
                    BOLD, max, RESET);
            try {
                String raw = scanner.nextLine().trim();
                if (raw.isBlank()) return -1;
                int choice = Integer.parseInt(raw);
                if (choice >= 1 && choice <= max) return choice - 1;
                System.out.printf(YELLOW + "Elige entre 1 y %d.%n" + RESET, max);
            } catch (NumberFormatException e) {
                System.out.println(YELLOW + "Ingresa un número válido." + RESET);
            } catch (Exception e) {
                return -1;
            }
        }
    }

    private void printAlbumDetail(AlbumDetail a) {
        int width = 60;
        String border = "─".repeat(width);

        System.out.println(BOLD + YELLOW + "┌" + border + "┐" + RESET);
        System.out.println(BOLD + YELLOW + "│" + RESET + center(a.title(), width) + BOLD + YELLOW + "│" + RESET);
        System.out.println(BOLD + YELLOW + "├" + border + "┤" + RESET);

        printAlbumField("Tipo",           a.type(),        width);
        printAlbumField("Fecha",          a.releaseDate(), width);
        printAlbumField("Sello",          a.label(),       width);

        System.out.println(BOLD + YELLOW + "├" + border + "┤" + RESET);
        System.out.println(BOLD + YELLOW + "│" + RESET + center("TRACKLIST", width) + BOLD + YELLOW + "│" + RESET);
        System.out.println(BOLD + YELLOW + "├" + border + "┤" + RESET);

        if (a.tracks().isEmpty()) {
            System.out.println(BOLD + YELLOW + "│" + RESET + "  " + DIM + "Sin tracklist disponible" + RESET);
        } else {
            for (AlbumDetail.TrackEntry t : a.tracks()) {
                String line = String.format("  %s%-4s%s %-44s %s%s%s",
                        DIM, t.number(), RESET,
                        truncate(t.title(), 44),
                        DIM, t.duration(), RESET);
                System.out.println(BOLD + YELLOW + "│" + RESET + line);
            }
        }

        System.out.println(BOLD + YELLOW + "└" + border + "┘" + RESET);
    }

    private void printAlbumField(String label, String value, int width) {
        if (value == null || value.isBlank()) return;
        System.out.println(BOLD + YELLOW + "│" + RESET +
                String.format("  %s%-10s%s %s", BOLD, label + ":", RESET, value));
    }

    private void printBanner() {
        System.out.println(BOLD + RED + """
                 __  __   _____   _____   _____    _     _       _       _   _   __  __
                |  \\/  | | ____| | ____| |_   _|  / \\   | |     | |     | | | | |  \\/  |
                | |\\/| | |  _|   |  _|     | |   / _ \\  | |     | |     | | | | | |\\/| |
                | |  | | | |___  | |___    | |  / ___ \\ | |___  | |___  | |_| | | |  | |
                |_|  |_| |_____| |_____|   |_| /_/  \\_\\|_____| |_____| \\___/  |_|  |_|
                """ + RESET);
        System.out.println(DIM + "  Encyclopaedia Metallum — Scraper de consola" + RESET);
        System.out.println();
    }
}
