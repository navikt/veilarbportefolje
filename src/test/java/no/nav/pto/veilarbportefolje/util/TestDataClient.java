package no.nav.pto.veilarbportefolje.util;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV2;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepositoryV2;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor;

public class TestDataClient {
    private final JdbcTemplate jdbcTemplatePostgres;
    private final RegistreringRepositoryV2 registreringRepositoryV2;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2;
    private final OpensearchTestClient opensearchTestClient;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;

    public TestDataClient(JdbcTemplate jdbcTemplatePostgres, RegistreringRepositoryV2 registreringRepositoryV2, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository, ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2, OpensearchTestClient opensearchTestClient, OppfolgingRepositoryV2 oppfolgingRepositoryV2, PdlIdentRepository pdlIdentRepository, PdlPersonRepository pdlPersonRepository) {
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.registreringRepositoryV2 = registreringRepositoryV2;
        this.oppfolgingsbrukerRepository = oppfolgingsbrukerRepository;
        this.arbeidslisteRepositoryV2 = arbeidslisteRepositoryV2;
        this.opensearchTestClient = opensearchTestClient;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        this.pdlIdentRepository = pdlIdentRepository;
        this.pdlPersonRepository = pdlPersonRepository;
    }

    public void endreNavKontorForBruker(AktorId aktoerId, NavKontor navKontor) {
        jdbcTemplatePostgres.update("""
                        update oppfolgingsbruker_arena_v2 set nav_kontor = ?
                        where fodselsnr = (select fnr from aktive_identer where aktorId = ?)
                        """,
                navKontor.getValue(), aktoerId.get());
    }

    public void setupBrukerMedArbeidsliste(AktorId aktoerId, NavKontor navKontor, VeilederId veilederId, ZonedDateTime startDato) {
        final Fnr fnr = TestDataUtils.randomFnr();
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(aktoerId.get(), false, AKTORID),
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT)
        ));
        arbeidslisteRepositoryV2.insertArbeidsliste(new ArbeidslisteDTO(fnr)
                .setAktorId(aktoerId)
                .setNavKontorForArbeidsliste(navKontor.getValue())
                .setVeilederId(veilederId)
                .setKategori(Arbeidsliste.Kategori.GUL)
        );

        lagreBrukerUnderOppfolging(aktoerId, fnr, navKontor, veilederId, startDato);
        opensearchTestClient.oppdaterArbeidsliste(aktoerId, true);
    }

    public void lagreBrukerUnderOppfolging(AktorId aktoerId, ZonedDateTime startDato) {
        final Fnr fnr = TestDataUtils.randomFnr();
        lagreBrukerUnderOppfolging(aktoerId, fnr, randomNavKontor(), VeilederId.of(null), startDato);
    }

    public void lagreBrukerUnderOppfolging(AktorId aktoerId,
                                           NavKontor navKontor,
                                           VeilederId veilederId,
                                           ZonedDateTime startDato) {
        final Fnr fnr = TestDataUtils.randomFnr();
        lagreBrukerUnderOppfolging(aktoerId, fnr, navKontor, veilederId, startDato);
    }

    public void lagreBrukerUnderOppfolging(AktorId aktoerId, Fnr fnr, String navKontor) {
        final VeilederId veilederId = TestDataUtils.randomVeilederId();
        lagreBrukerUnderOppfolging(aktoerId, fnr, NavKontor.of(navKontor), veilederId, ZonedDateTime.now());
    }

    public boolean hentUnderOppfolgingOgAktivIdent(AktorId aktoerId) {
        return oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktoerId);
    }

    private void lagreBrukerUnderOppfolging(AktorId aktoerId,
                                            Fnr fnr,
                                            NavKontor navKontor,
                                            VeilederId veilederId,
                                            ZonedDateTime startDato) {
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(aktoerId.get(), false, AKTORID),
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT)
        ));
        pdlPersonRepository.upsertPerson(fnr, new PDLPerson().setFoedsel(LocalDate.now()).setKjonn(Kjonn.K));
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId, startDato);
        oppfolgingRepositoryV2.settVeileder(aktoerId, veilederId);
        registreringRepositoryV2.upsertBrukerRegistrering(
                new ArbeidssokerRegistrertEvent(aktoerId.get(), null, null, null, null, null)
        );
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr.get(), null, null,
                        navKontor.getValue(), null, null, null,
                         ZonedDateTime.now()));
        opensearchTestClient.createUserInOpensearch(aktoerId);
    }
}
