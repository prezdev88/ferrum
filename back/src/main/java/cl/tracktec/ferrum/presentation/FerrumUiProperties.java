package cl.tracktec.ferrum.presentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ferrum.ui")
public class FerrumUiProperties {

    private Type type = Type.API;
    private MusicSearchProvider musicSearchProvider = MusicSearchProvider.YOUTUBE_MUSIC;
    /**
     * UI language used on startup. Supported: "es", "en".
     */
    private String language;
    /**
     * Lanterna theme used by the TUI. Supported: default, bigsnake, businessmachine, conqueror, defrost, blaster.
     */
    private Theme theme = Theme.DEFAULT;

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

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public enum Type {
        API,
        LANTERNA,
        CONSOLE,
        JSON,
        REFRESH_SESSION
    }

    public enum MusicSearchProvider {
        YOUTUBE,
        YOUTUBE_MUSIC
    }

    public enum Theme {
        DEFAULT("default"),
        BIGSNAKE("bigsnake"),
        BUSINESSMACHINE("businessmachine"),
        CONQUEROR("conqueror"),
        DEFROST("defrost"),
        BLASTER("blaster");

        private final String propertyValue;

        Theme(String propertyValue) {
            this.propertyValue = propertyValue;
        }

        public String propertyValue() {
            return propertyValue;
        }
    }
}
