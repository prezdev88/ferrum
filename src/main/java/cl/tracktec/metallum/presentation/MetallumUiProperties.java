package cl.tracktec.metallum.presentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "metallum.ui")
public class MetallumUiProperties {

    private Type type = Type.LANTERNA;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {
        LANTERNA,
        CONSOLE
    }
}
