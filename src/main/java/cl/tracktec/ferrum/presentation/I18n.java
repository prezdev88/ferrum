package cl.tracktec.ferrum.presentation;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

final class I18n {
    
    private final String baseName;
    private Locale locale;
    private ResourceBundle bundle;

    I18n(String baseName, Locale locale) {
        this.baseName = baseName;
        setLocale(locale);
    }

    void setLocale(Locale locale) {
        this.locale = locale == null ? Locale.ENGLISH : locale;
        this.bundle = ResourceBundle.getBundle(baseName, this.locale);
    }

    Locale getLocale() {
        return locale;
    }

    String t(String key, Object... args) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            pattern = "??" + key + "??";
        }
        if (args == null || args.length == 0) return pattern;
        return MessageFormat.format(pattern, args);
    }
}

