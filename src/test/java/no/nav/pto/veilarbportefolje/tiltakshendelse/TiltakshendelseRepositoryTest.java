package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Avsender;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = ApplicationConfigTest.class)
class TiltakshendelseRepositoryTest {
    @Autowired
    private TiltakshendelseRepository repository;


    @Test
    public void testLagreTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse kafkaData = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr);

        assertTrue(repository.tryLagreTiltakshendelseData(kafkaData));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert(tiltakshendelser.size() == 1);
        assert(tiltakshendelser.getFirst().equals(expected));
    }

//    @Test
//    public void testOppdatereTiltakshendelse() {
//        // legg inn eit test-objekt
//        // send inn eit nytt
//        // sjekk at det nye er det vi får inn.
//    }
}
