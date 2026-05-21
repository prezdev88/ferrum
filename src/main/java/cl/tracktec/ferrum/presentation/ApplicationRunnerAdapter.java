package cl.tracktec.ferrum.presentation;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationRunnerAdapter implements CommandLineRunner {

    private final FerrumUi ferrumUi;

    public ApplicationRunnerAdapter(FerrumUi ferrumUi) {
        this.ferrumUi = ferrumUi;
    }

    @Override
    public void run(String... args) throws Exception {
        ferrumUi.run(args);
    }
}
