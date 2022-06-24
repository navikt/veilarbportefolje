package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class Siste14aVedtakRepositoryTest {

    @Autowired
    private Siste14aVedtakRepository siste14aVedtakRepository;

    @Test
    public void upsert14aVedtak() {
        Siste14aVedtakDTO siste14aVedtak = new Siste14aVedtakDTO(
                AktorId.of("1234"),
                Siste14aVedtakDTO.Innsatsgruppe.STANDARD_INNSATS,
                Siste14aVedtakDTO.Hovedmal.SKAFFE_ARBEID,
                ZonedDateTime.parse("2021-05-04T09:48:58.762000+02:00")
        );

        siste14aVedtakRepository.upsert(siste14aVedtak);

        Optional<Siste14aVedtakDTO> kanskjeResultat =
                siste14aVedtakRepository.hentSiste14aVedtak(siste14aVedtak.aktorId);

        assertTrue(kanskjeResultat.isPresent());

        Siste14aVedtakDTO resultat = kanskjeResultat.get();

        assertEquals(siste14aVedtak.getFattetDato().toInstant(), resultat.getFattetDato().toInstant());
        assertEquals(siste14aVedtak.getAktorId(), resultat.getAktorId());
        assertEquals(siste14aVedtak.getHovedmal(), resultat.getHovedmal());
        assertEquals(siste14aVedtak.getInnsatsgruppe(), resultat.getInnsatsgruppe());
    }
}
