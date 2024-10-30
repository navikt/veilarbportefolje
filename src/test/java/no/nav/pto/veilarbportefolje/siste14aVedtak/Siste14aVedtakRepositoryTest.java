package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal.BEHOLDE_ARBEID;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal.SKAFFE_ARBEID;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe.STANDARD_INNSATS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class Siste14aVedtakRepositoryTest {

    @Autowired
    private Siste14aVedtakRepository siste14aVedtakRepository;

    @Autowired
    private PdlIdentRepository pdlIdentRepository;

    private final Random random = new Random();

    @Test
    public void upsert_hent_og_slett_siste_14a_vedtak() {
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        IdenterForBruker identerForBruker = new IdenterForBruker(List.of(aktorId1.get(), aktorId2.get()));

        AktorId aktorIdForAnnenBruker = randomAktorId();
        IdenterForBruker identerForAnnenBruker = new IdenterForBruker(List.of(aktorIdForAnnenBruker.get(), randomAktorId().get()));

        Siste14aVedtak siste14aVedtakForAnnenBruker = new Siste14aVedtak(
                aktorIdForAnnenBruker,
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                Hovedmal.OKE_DELTAKELSE,
                ZonedDateTime.parse("2022-01-01T11:33:22.133000+02:00"),
                false
        );

        siste14aVedtakRepository.upsert(siste14aVedtakForAnnenBruker, identerForAnnenBruker);

        assertLagretVedtak(siste14aVedtakForAnnenBruker, identerForAnnenBruker);

        Siste14aVedtak siste14aVedtak = new Siste14aVedtak(
                aktorId1,
                STANDARD_INNSATS,
                SKAFFE_ARBEID,
                ZonedDateTime.parse("2021-05-04T09:48:58.762000+02:00"),
                false
        );

        siste14aVedtakRepository.upsert(siste14aVedtak, identerForBruker);

        assertLagretVedtak(siste14aVedtak, identerForBruker);

        Siste14aVedtak oppdatert14aVedtak = new Siste14aVedtak(
                aktorId2,
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

    @Test
    public void henter_siste_14a_vedtak_for_brukere() {
        List<List<PDLIdent>> pdlIdenterForBrukere = List.of(
                lagreIdenterForTilfeldigBruker(),
                lagreIdenterForTilfeldigBruker(),
                lagreIdenterForTilfeldigBruker(),
                lagreIdenterForTilfeldigBruker()
        );

        Map<AktorId, Siste14aVedtak> forventet =
                pdlIdenterForBrukere.stream().collect(Collectors.toMap(
                        identer -> AktorId.of(identer.stream().filter(pdlIdent -> pdlIdent.getGruppe() == AKTORID).map(PDLIdent::getIdent).findAny().get()),
                        this::upsertSiste14aForBrukersIdenterOgReturnerSiste));

        Map<AktorId, Siste14aVedtak> resultat =
                siste14aVedtakRepository.hentSiste14aVedtakForBrukere(forventet.keySet());

        assertThat(resultat).containsExactlyInAnyOrderEntriesOf(forventet);
    }

    private void assertLagretVedtak(Siste14aVedtak expected, IdenterForBruker identer) {
        Optional<Siste14aVedtak> kanskjeResultat =
                siste14aVedtakRepository.hentSiste14aVedtak(identer);

        assertTrue(kanskjeResultat.isPresent());

        Siste14aVedtak resultat = kanskjeResultat.get();

        assertEquals(expected.getFattetDato().toInstant(), resultat.getFattetDato().toInstant());
        assertEquals(expected.getAktorId(), resultat.getAktorId());
        assertEquals(expected.getHovedmal(), resultat.getHovedmal());
        assertEquals(expected.getInnsatsgruppe(), resultat.getInnsatsgruppe());
        assertEquals(expected.isFraArena(), resultat.isFraArena());
    }

    private void assertSlettetVedtak(IdenterForBruker identer) {
        Optional<Siste14aVedtak> kanskjeResultat =
                siste14aVedtakRepository.hentSiste14aVedtak(identer);

        assertFalse(kanskjeResultat.isPresent());
    }

    private List<PDLIdent> lagreIdenterForTilfeldigBruker() {
        Stream<PDLIdent> fnrIdenter = IntStream.range(0, random.nextInt(1, 4))
                .mapToObj(x -> new PDLIdent(randomFnr().get(), x != 0, FOLKEREGISTERIDENT));

        Stream<PDLIdent> aktorIdIdenter = IntStream.range(0, random.nextInt(1, 4))
                .mapToObj(x -> new PDLIdent(randomAktorId().get(), x != 0, AKTORID));

        List<PDLIdent> alleIdenter = Stream.concat(fnrIdenter, aktorIdIdenter).toList();

        pdlIdentRepository.upsertIdenter(alleIdenter);

        return alleIdenter;
    }

    private Siste14aVedtak upsertSiste14aForBrukersIdenterOgReturnerSiste(List<PDLIdent> identer) {
        List<Siste14aVedtak> vedtakListe = identer.stream().map(ident ->
                new Siste14aVedtak(
                        AktorId.of(ident.getIdent()),
                        randomInnsatsgruppe(),
                        randomHovedmal(),
                        randomZonedDate(),
                        random.nextBoolean()
                )
        ).collect(Collectors.toList());

        Collections.shuffle(vedtakListe);

        IdenterForBruker identerForBruker = new IdenterForBruker(identer.stream().map(PDLIdent::getIdent).toList());
        vedtakListe.forEach(vedtak -> siste14aVedtakRepository.upsert(vedtak, identerForBruker));

        return vedtakListe.get(vedtakListe.size() - 1);
    }
}
