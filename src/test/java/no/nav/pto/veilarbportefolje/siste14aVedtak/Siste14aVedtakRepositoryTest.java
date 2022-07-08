package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class Siste14aVedtakRepositoryTest {

    @Autowired
    private Siste14aVedtakRepository siste14aVedtakRepository;

    @Test
    public void upsert_hent_og_slett_siste_14a_vedtak() {
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        IdenterForBruker identerForBruker = new IdenterForBruker(List.of(aktorId1.get(), aktorId2.get()));

        AktorId aktorIdForAnnenBruker = randomAktorId();
        IdenterForBruker identerForAnnenBruker = new IdenterForBruker(List.of(aktorIdForAnnenBruker.get(), randomAktorId().get()));

        Siste14aVedtakDTO siste14aVedtakForAnnenBruker = new Siste14aVedtakDTO(
                aktorIdForAnnenBruker,
                Siste14aVedtakDTO.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                Siste14aVedtakDTO.Hovedmal.OKE_DELTAKELSE,
                ZonedDateTime.parse("2022-01-01T11:33:22.133000+02:00"),
                false
        );

        siste14aVedtakRepository.upsert(siste14aVedtakForAnnenBruker, identerForAnnenBruker);

        assertLagretVedtak(siste14aVedtakForAnnenBruker, identerForAnnenBruker);

        Siste14aVedtakDTO siste14aVedtak = new Siste14aVedtakDTO(
                aktorId1,
                Siste14aVedtakDTO.Innsatsgruppe.STANDARD_INNSATS,
                Siste14aVedtakDTO.Hovedmal.SKAFFE_ARBEID,
                ZonedDateTime.parse("2021-05-04T09:48:58.762000+02:00"),
                false
        );

        siste14aVedtakRepository.upsert(siste14aVedtak, identerForBruker);

        assertLagretVedtak(siste14aVedtak, identerForBruker);

        Siste14aVedtakDTO oppdatert14aVedtak = new Siste14aVedtakDTO(
                aktorId2,
                Siste14aVedtakDTO.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Siste14aVedtakDTO.Hovedmal.BEHOLDE_ARBEID,
                ZonedDateTime.parse("2022-01-04T10:01:32.689000+02:00"),
                true
        );

        siste14aVedtakRepository.upsert(oppdatert14aVedtak, identerForBruker);

        assertLagretVedtak(oppdatert14aVedtak, identerForBruker);

        siste14aVedtakRepository.delete(identerForBruker);

        assertSlettetVedtak(identerForBruker);
        assertLagretVedtak(siste14aVedtakForAnnenBruker, identerForAnnenBruker);
    }

    private void assertLagretVedtak(Siste14aVedtakDTO expected, IdenterForBruker identer) {
        Optional<Siste14aVedtakDTO> kanskjeResultat =
                siste14aVedtakRepository.hentSiste14aVedtak(identer);

        assertTrue(kanskjeResultat.isPresent());

        Siste14aVedtakDTO resultat = kanskjeResultat.get();

        assertEquals(expected.getFattetDato().toInstant(), resultat.getFattetDato().toInstant());
        assertEquals(expected.getAktorId(), resultat.getAktorId());
        assertEquals(expected.getHovedmal(), resultat.getHovedmal());
        assertEquals(expected.getInnsatsgruppe(), resultat.getInnsatsgruppe());
        assertEquals(expected.isFraArena(), resultat.isFraArena());
    }
    private void assertSlettetVedtak(IdenterForBruker identer) {
        Optional<Siste14aVedtakDTO> kanskjeResultat =
                siste14aVedtakRepository.hentSiste14aVedtak(identer);

        assertFalse(kanskjeResultat.isPresent());
    }
}
