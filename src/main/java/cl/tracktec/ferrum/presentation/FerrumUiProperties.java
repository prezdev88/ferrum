package cl.tracktec.ferrum.presentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ferrum.ui")
public class FerrumUiProperties {

    private Type type = Type.LANTERNA;
    private MusicSearchProvider musicSearchProvider = MusicSearchProvider.YOUTUBE_MUSIC;
    /**
     * UI language used on startup. Supported: "es", "en".
     */
    private String language;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public MusicSearchProvider getMusicSearchProvider() {
        return musicSearchProvider;
    }

    public void setMusicSearchProvider(MusicSearchProvider musicSearchProvider) {
        this.musicSearchProvider = musicSearchProvider;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public enum Type {
        LANTERNA,
        CONSOLE
    }

    public enum MusicSearchProvider {
        YOUTUBE,
        YOUTUBE_MUSIC
    }
}
