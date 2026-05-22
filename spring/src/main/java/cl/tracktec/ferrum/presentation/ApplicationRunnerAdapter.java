package cl.tracktec.ferrum.presentation;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationRunnerAdapter implements CommandLineRunner {

    private final ObjectProvider<FerrumUi> ferrumUiProvider;

    public ApplicationRunnerAdapter(ObjectProvider<FerrumUi> ferrumUiProvider) {
        this.ferrumUiProvider = ferrumUiProvider;
    }

    @Override
    public void run(String... args) throws Exception {
        FerrumUi ferrumUi = ferrumUiProvider.getIfAvailable();
        if (ferrumUi == null) {
            return;
        }
        ferrumUi.run(args);
    }
}
