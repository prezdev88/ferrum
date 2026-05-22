package cl.tracktec.ferrum.domain;

public enum BandSearchType {
    BAND_NAME("Band name", "band_name"),
    MUSIC_GENRE("Music genre", "band_genre"),
    THEMES("Themes", "band_themes"),
    ALBUM_TITLE("Album title", "album_title"),
    SONG_TITLE("Song title", "song_title"),
    LABEL("Label", "label_name"),
    ARTIST("Artist", "artist_name"),
    USER_PROFILE("User profile", "user_profile"),
    GOOGLE("Google", "google");

    private final String label;
    private final String requestType;

    BandSearchType(String label, String requestType) {
        this.label = label;
        this.requestType = requestType;
    }

    public String getRequestType() {
        return requestType;
    }

    @Override
    public String toString() {
        return label;
    }
}
