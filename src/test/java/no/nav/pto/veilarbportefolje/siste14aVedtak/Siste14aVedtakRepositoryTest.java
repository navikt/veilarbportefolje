package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.common.types.identer.AktorId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static org.junit.Assert.assertEquals;

public class Siste14aVedtakRepositoryTest {
    private Siste14aVedtakRepository siste14aVedtakRepository;
    private Siste14aVedtakDTO siste14aVedtak;

    @Before
    public void setUp() {
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        siste14aVedtakRepository = new Siste14aVedtakRepository(db);
    }

    @Test
    public void upsert14aVedtak() {
        siste14aVedtak = new Siste14aVedtakDTO(
                AktorId.of("1234"),
                Siste14aVedtakDTO.Innsatsgruppe.STANDARD_INNSATS,
                Siste14aVedtakDTO.Hovedmal.SKAFFE_ARBEID,
                ZonedDateTime.parse("2021-05-04T09:48:58.762000+02:00")
        );

        siste14aVedtakRepository.upsert(siste14aVedtak);

        Siste14aVedtakDTO resultat = siste14aVedtakRepository.hentSiste14aVedtak(siste14aVedtak.aktorId);

        assertEquals(siste14aVedtak.getFattetDato().toInstant(), resultat.getFattetDato().toInstant());
        assertEquals(siste14aVedtak.getAktorId(), resultat.getAktorId());
        assertEquals(siste14aVedtak.getHovedmal(), resultat.getHovedmal());
        assertEquals(siste14aVedtak.getInnsatsgruppe(), resultat.getInnsatsgruppe());
    }
}