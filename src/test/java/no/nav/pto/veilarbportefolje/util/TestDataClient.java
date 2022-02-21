package no.nav.pto.veilarbportefolje.util;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV2;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepositoryV2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

public class TestDataClient {

    private final JdbcTemplate jdbcTemplate;
    private final RegistreringRepositoryV2 registreringRepositoryV2;
    private final OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2;
    private final ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2;
    private final OpensearchTestClient opensearchTestClient;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    public TestDataClient(@Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate, RegistreringRepositoryV2 registreringRepositoryV2, OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2, ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2, OpensearchTestClient opensearchTestClient, OppfolgingRepositoryV2 oppfolgingRepositoryV2) {
        this.jdbcTemplate = jdbcTemplate;
        this.registreringRepositoryV2 = registreringRepositoryV2;
        this.oppfolgingsbrukerRepositoryV2 = oppfolgingsbrukerRepositoryV2;
        this.arbeidslisteRepositoryV2 = arbeidslisteRepositoryV2;
        this.opensearchTestClient = opensearchTestClient;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
    }

    public void endreNavKontorForBruker(AktorId aktoerId, NavKontor navKontor) {
        jdbcTemplate.update("update oppfolgingsbruker_arena set nav_kontor = ? where aktoerid = ?",
                navKontor.getValue(), aktoerId.get());
    }

    public void setupBrukerMedArbeidsliste(AktorId aktoerId, NavKontor navKontor, VeilederId veilederId, ZonedDateTime startDato) {
        final Fnr fnr = TestDataUtils.randomFnr();
        arbeidslisteRepositoryV2.insertArbeidsliste(new ArbeidslisteDTO(fnr)
                .setAktorId(aktoerId)
                .setNavKontorForArbeidsliste(navKontor.getValue())
        );
        setupBruker(aktoerId, fnr, navKontor, veilederId, startDato);

        opensearchTestClient.oppdaterArbeidsliste(aktoerId, true);
    }

    public void setupBruker(AktorId aktoerId, Fnr fnr, NavKontor navKontor, VeilederId veilederId, ZonedDateTime startDato) {
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId, startDato);
        oppfolgingRepositoryV2.settVeileder(aktoerId, veilederId);
        registreringRepositoryV2.upsertBrukerRegistrering(new ArbeidssokerRegistrertEvent(aktoerId.get(), null, null, null, null, null));
        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(aktoerId.get(), fnr.get(), null, null,
                        null, null, navKontor.getValue(), null, null,
                        null, null, null, true, false,
                        false, null, null));

        opensearchTestClient.createUserInOpensearch(aktoerId);
    }

    public void setupBruker(AktorId aktoerId, NavKontor navKontor, VeilederId veilederId, ZonedDateTime startDato) {
        final Fnr fnr = TestDataUtils.randomFnr();
        setupBruker(aktoerId, fnr, navKontor, veilederId, startDato);
    }

    public boolean hentOppfolgingFlaggFraDatabase(AktorId aktoerId) {
        return oppfolgingRepositoryV2.erUnderOppfolging(aktoerId);
    }
}
