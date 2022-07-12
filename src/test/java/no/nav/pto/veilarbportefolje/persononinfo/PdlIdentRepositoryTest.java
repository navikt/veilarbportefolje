package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingPeriodeService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class PdlIdentRepositoryTest {

    @Autowired
    private PdlIdentRepository pdlIdentRepository;

    @Autowired
    private OppfolgingPeriodeService oppfolgingPeriodeService;

    @Autowired
    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @Test
    public void identSplitt_allePersonerMedTidligereIdenterSkalSlettes() {
        PDLIdent identIKonflikt = new PDLIdent(randomAktorId().get(), true, AKTORID);
        List<PDLIdent> identerBrukerA = List.of(
                identIKonflikt,
                new PDLIdent(randomAktorId().get(), false, AKTORID),
                new PDLIdent(randomFnr().get(), false, FOLKEREGISTERIDENT)
        );
        List<PDLIdent> identerBrukerB = List.of(
                identIKonflikt,
                new PDLIdent(randomAktorId().get(), false, AKTORID),
                new PDLIdent(randomFnr().get(), false, FOLKEREGISTERIDENT)
        );
        pdlIdentRepository.upsertIdenter(identerBrukerA);
        String lokalIdentBrukerA = pdlIdentRepository.hentPerson(identIKonflikt.getIdent());
        var brukerAPreBrukerB = pdlIdentRepository.hentIdenter(lokalIdentBrukerA);

        pdlIdentRepository.upsertIdenter(identerBrukerB);

        String lokalIdentBrukerB = pdlIdentRepository.hentPerson(identIKonflikt.getIdent());
        var brukerAPostBrukerB = pdlIdentRepository.hentIdenter(lokalIdentBrukerA);
        var brukerB = pdlIdentRepository.hentIdenter(lokalIdentBrukerB);

        assertThat(lokalIdentBrukerA).isNotEqualTo(lokalIdentBrukerB);
        assertThat(brukerAPreBrukerB).containsExactlyInAnyOrderElementsOf(identerBrukerA);
        assertThat(brukerAPostBrukerB.stream().sorted()).isNotEqualTo(identerBrukerA.stream().sorted());
        assertThat(brukerB).containsExactlyInAnyOrderElementsOf(identerBrukerB);
        assertThat(brukerAPostBrukerB).hasSize(0);
    }

    @Test
    public void oppfolgingAvsluttet_flereIdenterUnderOppfolging_lokalIdentLagringSkalIkkeSlettes() {
        AktorId historiskIdent = randomAktorId();
        AktorId ident = randomAktorId();
        List<PDLIdent> identer = List.of(
                new PDLIdent(historiskIdent.get(), true, AKTORID),
                new PDLIdent(ident.get(), false, AKTORID),
                new PDLIdent(randomFnr().get(), false, FOLKEREGISTERIDENT)
        );
        var historiskOpfolgingStart = new SisteOppfolgingsperiodeV1(null, historiskIdent.get(), ZonedDateTime.now(), null);
        var nyOpfolgingStart = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), null);
        var nyOpfolgingAvslutt = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), ZonedDateTime.now());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(historiskOpfolgingStart);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(nyOpfolgingStart);
        // Mock PDL respons
        pdlIdentRepository.upsertIdenter(identer);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(nyOpfolgingAvslutt);

        var lokaleIdenter = hentLokaleIdenter(historiskIdent);
        assertThat(identer).containsExactlyInAnyOrderElementsOf(lokaleIdenter);
    }

    @Test
    public void oppfolgingAvsluttet_ingenAndreIdenterUnderOppfolging_identLagringSkalSlettes() {
        AktorId ident = randomAktorId();
        List<PDLIdent> identer = List.of(
                new PDLIdent(ident.get(), false, AKTORID)
        );
        var opfolgingStart = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), null);
        var opfolgingAvslutt = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), ZonedDateTime.now());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(opfolgingStart);
        // Mock PDL respons
        pdlIdentRepository.upsertIdenter(identer);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(opfolgingAvslutt);
        var lokaleIdenter = hentLokaleIdenter(ident);
        assertThat(lokaleIdenter).hasSize(0);
    }

    @Test
    public void henterIdenterForEnBruker() {
        Fnr brukersOppsagIdent = randomFnr();

        List<PDLIdent> brukersIdenter = List.of(
                new PDLIdent(randomAktorId().get(), false, AKTORID),
                new PDLIdent(randomFnr().get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(randomAktorId().get(), true, AKTORID),
                new PDLIdent(brukersOppsagIdent.get(), true, FOLKEREGISTERIDENT),
                new PDLIdent(randomFnr().get(), true, FOLKEREGISTERIDENT)
        );

        pdlIdentRepository.upsertIdenter(brukersIdenter);

        List<PDLIdent> annenBrukersIdenter = List.of(
                new PDLIdent(randomAktorId().get(), false, AKTORID),
                new PDLIdent(randomFnr().get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(randomAktorId().get(), true, AKTORID),
                new PDLIdent(randomFnr().get(), true, FOLKEREGISTERIDENT)
        );

        pdlIdentRepository.upsertIdenter(annenBrukersIdenter);

        IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(brukersOppsagIdent.get());

        assertThat(identer.identer())
                .containsExactlyInAnyOrderElementsOf(brukersIdenter.stream().map(PDLIdent::getIdent).toList());
    }

    @Test
    public void erBrukerUnderOppfolging__under_oppfølging_baset_på_gjeldende_og_historiske_identer() {

        Fnr fnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        AktorId aktorId = randomAktorId();
        AktorId historiskAktorId = randomAktorId();

        List<PDLIdent> brukersIdenter = List.of(
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(historiskFnr.get(), true, FOLKEREGISTERIDENT),
                new PDLIdent(historiskAktorId.get(), true, AKTORID)
        );

        pdlIdentRepository.upsertIdenter(brukersIdenter);
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());

        assertTrue(pdlIdentRepository.erBrukerUnderOppfolging(fnr.get()));
        assertTrue(pdlIdentRepository.erBrukerUnderOppfolging(historiskFnr.get()));
        assertTrue(pdlIdentRepository.erBrukerUnderOppfolging(aktorId.get()));
        assertTrue(pdlIdentRepository.erBrukerUnderOppfolging(historiskAktorId.get()));
    }

    @Test
    public void erBrukerUnderOppfolging__ikke_under_oppfølging_baset_på_gjeldende_og_historiske_identer() {
        Fnr fnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        AktorId aktorId = randomAktorId();
        AktorId historiskAktorId = randomAktorId();

        List<PDLIdent> brukersIdenter = List.of(
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(historiskFnr.get(), true, FOLKEREGISTERIDENT),
                new PDLIdent(historiskAktorId.get(), true, AKTORID)
        );

        AktorId annenBrukersAktorId = randomAktorId();
        List<PDLIdent> annenBrukersIdenter = List.of(
                new PDLIdent(randomFnr().get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(annenBrukersAktorId.get(), false, AKTORID)
        );

        pdlIdentRepository.upsertIdenter(brukersIdenter);
        pdlIdentRepository.upsertIdenter(annenBrukersIdenter);
        oppfolgingRepositoryV2.settUnderOppfolging(annenBrukersAktorId, ZonedDateTime.now());

        assertFalse(pdlIdentRepository.erBrukerUnderOppfolging(fnr.get()));
        assertFalse(pdlIdentRepository.erBrukerUnderOppfolging(historiskFnr.get()));
        assertFalse(pdlIdentRepository.erBrukerUnderOppfolging(aktorId.get()));
        assertFalse(pdlIdentRepository.erBrukerUnderOppfolging(historiskAktorId.get()));
        assertTrue(pdlIdentRepository.erBrukerUnderOppfolging(annenBrukersAktorId.get()));
    }

    private List<PDLIdent> hentLokaleIdenter(AktorId ident) {
        return pdlIdentRepository.hentIdenter(pdlIdentRepository.hentPerson(ident.get()));
    }
}
