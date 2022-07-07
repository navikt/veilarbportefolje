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
        AktorId aktorId = AktorId.of("1234");

        Siste14aVedtakDTO siste14aVedtak = new Siste14aVedtakDTO(
                aktorId,
                Siste14aVedtakDTO.Innsatsgruppe.STANDARD_INNSATS,
                Siste14aVedtakDTO.Hovedmal.SKAFFE_ARBEID,
                ZonedDateTime.parse("2021-05-04T09:48:58.762000+02:00"),
                false
        );

        siste14aVedtakRepository.upsert(siste14aVedtak);

        assertStoredVedtak(siste14aVedtak);

        Siste14aVedtakDTO oppdatert14aVedtak = new Siste14aVedtakDTO(
                aktorId,
                Siste14aVedtakDTO.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Siste14aVedtakDTO.Hovedmal.BEHOLDE_ARBEID,
                ZonedDateTime.parse("2022-01-04T10:01:32.689000+02:00"),
                true
        );

        siste14aVedtakRepository.upsert(oppdatert14aVedtak);

        assertStoredVedtak(oppdatert14aVedtak);
    }

    private void assertStoredVedtak(Siste14aVedtakDTO expected) {
        Optional<Siste14aVedtakDTO> kanskjeResultat =
                siste14aVedtakRepository.hentSiste14aVedtak(expected.aktorId);

        assertTrue(kanskjeResultat.isPresent());

        Siste14aVedtakDTO resultat = kanskjeResultat.get();

        assertEquals(expected.getFattetDato().toInstant(), resultat.getFattetDato().toInstant());
        assertEquals(expected.getAktorId(), resultat.getAktorId());
        assertEquals(expected.getHovedmal(), resultat.getHovedmal());
        assertEquals(expected.getInnsatsgruppe(), resultat.getInnsatsgruppe());
        assertEquals(expected.isFraArena(), resultat.isFraArena());
    }
}
