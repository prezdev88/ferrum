package cl.tracktec.metallum.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "metallum.cache")
public class MetallumCacheProperties {

    private boolean enabled = true;
    private Path directory = defaultDirectory();
    private final Entry search = new Entry(Duration.ofDays(1));
    private final Entry band = new Entry(Duration.ofDays(7));
    private final Entry album = new Entry(Duration.ofDays(30));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Path getDirectory() {
        return directory;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    public Entry getSearch() {
        return search;
    }

    public Entry getBand() {
        return band;
    }

    public Entry getAlbum() {
        return album;
    }

    private Path defaultDirectory() {
        return Path.of(System.getProperty("user.home"), ".config", "metallum", "cache");
    }

    public static class Entry {

        private Duration ttl;

        public Entry(Duration ttl) {
            this.ttl = ttl;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
