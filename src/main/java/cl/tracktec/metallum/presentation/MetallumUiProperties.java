package cl.tracktec.metallum.presentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "metallum.ui")
public class MetallumUiProperties {

    private Type type = Type.LANTERNA;
    private MusicSearchProvider musicSearchProvider = MusicSearchProvider.YOUTUBE_MUSIC;

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

    public enum Type {
        LANTERNA,
        CONSOLE
    }

    public enum MusicSearchProvider {
        YOUTUBE,
        YOUTUBE_MUSIC
    }
}
