package no.nav.pto.veilarbportefolje.ensligforsorger;

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(classes = ApplicationConfigTest.class)
public class EnsligeForsorgereRepositoryTest {

    @Autowired
    private EnsligeForsorgereRepository ensligeForsorgereRepository;

    @Test
    public void testInsertOfStonadType() {
        Integer test = ensligeForsorgereRepository.lagreStonadstype("Test");
        Integer test1 = ensligeForsorgereRepository.lagreStonadstype("Test1");
        Integer test2 = ensligeForsorgereRepository.lagreStonadstype("Test2");
    }

}