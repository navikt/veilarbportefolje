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

import static no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO.Hovedmal.BEHOLDE_ARBEID;
import static no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO.Hovedmal.SKAFFE_ARBEID;
import static no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS;
import static no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO.Innsatsgruppe.STANDARD_INNSATS;
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

        Siste14aVedtak siste14aVedtakForAnnenBruker = new Siste14aVedtak(
                aktorIdForAnnenBruker.get(),
                Siste14aVedtakKafkaDTO.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                Siste14aVedtakKafkaDTO.Hovedmal.OKE_DELTAKELSE,
                ZonedDateTime.parse("2022-01-01T11:33:22.133000+02:00"),
                false
        );

        siste14aVedtakRepository.upsert(siste14aVedtakForAnnenBruker, identerForAnnenBruker);

        assertLagretVedtak(siste14aVedtakForAnnenBruker, identerForAnnenBruker);

        Siste14aVedtak siste14aVedtak = new Siste14aVedtak(
                aktorId1.get(),
                STANDARD_INNSATS,
                SKAFFE_ARBEID,
                ZonedDateTime.parse("2021-05-04T09:48:58.762000+02:00"),
                false
        );

        siste14aVedtakRepository.upsert(siste14aVedtak, identerForBruker);

        assertLagretVedtak(siste14aVedtak, identerForBruker);

        Siste14aVedtak oppdatert14aVedtak = new Siste14aVedtak(
                aktorId2.get(),
                SITUASJONSBESTEMT_INNSATS,
                BEHOLDE_ARBEID,
                ZonedDateTime.parse("2022-01-04T10:01:32.689000+02:00"),
                true
        );

        siste14aVedtakRepository.upsert(oppdatert14aVedtak, identerForBruker);

        assertLagretVedtak(oppdatert14aVedtak, identerForBruker);

        siste14aVedtakRepository.delete(identerForBruker);

        assertSlettetVedtak(identerForBruker);
        assertLagretVedtak(siste14aVedtakForAnnenBruker, identerForAnnenBruker);
    }

    private void assertLagretVedtak(Siste14aVedtak expected, IdenterForBruker identer) {
        Optional<Siste14aVedtak> kanskjeResultat =
                siste14aVedtakRepository.hentSiste14aVedtak(identer);

        assertTrue(kanskjeResultat.isPresent());

        Siste14aVedtak resultat = kanskjeResultat.get();

        assertEquals(expected.getFattetDato().toInstant(), resultat.getFattetDato().toInstant());
        assertEquals(expected.getBrukerId(), resultat.getBrukerId());
        assertEquals(expected.getHovedmal(), resultat.getHovedmal());
        assertEquals(expected.getInnsatsgruppe(), resultat.getInnsatsgruppe());
        assertEquals(expected.isFraArena(), resultat.isFraArena());
    }
    private void assertSlettetVedtak(IdenterForBruker identer) {
        Optional<Siste14aVedtak> kanskjeResultat =
                siste14aVedtakRepository.hentSiste14aVedtak(identer);

        assertFalse(kanskjeResultat.isPresent());
    }
}
