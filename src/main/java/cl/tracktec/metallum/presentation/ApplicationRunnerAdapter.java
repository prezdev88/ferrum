package cl.tracktec.metallum.presentation;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationRunnerAdapter implements CommandLineRunner {

    private final MetallumUi metallumUi;

    public ApplicationRunnerAdapter(MetallumUi metallumUi) {
        this.metallumUi = metallumUi;
    }

    @Override
    public void run(String... args) throws Exception {
        metallumUi.run(args);
    }
}
