package cl.tracktec.ferrum.presentation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ferrum.ui", name = "type", havingValue = "refresh_session")
public class RefreshSessionRunner implements FerrumUi {

    private final ConfigurableApplicationContext applicationContext;

    public RefreshSessionRunner(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }
}
