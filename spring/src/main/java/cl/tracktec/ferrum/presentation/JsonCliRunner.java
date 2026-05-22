package cl.tracktec.ferrum.presentation;

import cl.tracktec.ferrum.core.domain.BandSearchType;
import cl.tracktec.ferrum.core.usecase.GetAlbumDetailsUseCase;
import cl.tracktec.ferrum.core.usecase.GetBandDetailsUseCase;
import cl.tracktec.ferrum.core.usecase.SearchBandsUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "ferrum.ui", name = "type", havingValue = "json")
public class JsonCliRunner implements FerrumUi {

    private final SearchBandsUseCase searchBands;
    private final GetBandDetailsUseCase getBandDetails;
    private final GetAlbumDetailsUseCase getAlbumDetails;
    private final ObjectMapper objectMapper;
    private final ConfigurableApplicationContext applicationContext;

    public JsonCliRunner(
            SearchBandsUseCase searchBands,
            GetBandDetailsUseCase getBandDetails,
            GetAlbumDetailsUseCase getAlbumDetails,
            ObjectMapper objectMapper,
            ConfigurableApplicationContext applicationContext
    ) {
        this.searchBands = searchBands;
        this.getBandDetails = getBandDetails;
        this.getAlbumDetails = getAlbumDetails;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing command. Supported: search, band, album");
        }

        String command = args[0].trim().toLowerCase(Locale.ROOT);
        Map<String, String> options = parseOptions(args);

        Object payload = switch (command) {
            case "search" -> searchBands.execute(
                    requiredOption(options, "query"),
                    parseSearchType(requiredOption(options, "search-type"))
            );
            case "band" -> getBandDetails.execute(requiredOption(options, "url"));
            case "album" -> getAlbumDetails.execute(requiredOption(options, "url"));
            default -> throw new IllegalArgumentException(
                    "Unsupported command: " + command + ". Supported: search, band, album"
            );
        };

        System.out.println(objectMapper.writeValueAsString(payload));
        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }

    private Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Invalid option: " + arg);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for option: " + arg);
            }
            options.put(arg.substring(2), args[++i]);
        }
        return options;
    }

    private String requiredOption(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option: --" + key);
        }
        return value;
    }

    private BandSearchType parseSearchType(String rawValue) {
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return BandSearchType.valueOf(normalized);
    }
}
