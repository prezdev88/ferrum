package cl.tracktec.metallum.core.cache;

public enum CacheNamespace {
    SEARCH("search"),
    BAND("band"),
    ALBUM("album");

    private final String directoryName;

    CacheNamespace(String directoryName) {
        this.directoryName = directoryName;
    }

    public String getDirectoryName() {
        return directoryName;
    }
}
