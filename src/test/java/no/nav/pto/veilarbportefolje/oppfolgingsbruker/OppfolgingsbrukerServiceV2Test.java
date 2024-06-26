package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OppfolgingsbrukerServiceV2Test extends EndToEndTest {
    private final JdbcTemplate db;
    private final OppfolgingsbrukerServiceV2 oppfolginsbrukerService;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    private final Fnr fnr = randomFnr();
    private final AktorId aktorId = randomAktorId();

    @Autowired
    public OppfolgingsbrukerServiceV2Test(JdbcTemplate db, OppfolgingsbrukerServiceV2 oppfolginsbrukerService, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3) {
        this.db = db;
        this.oppfolginsbrukerService = oppfolginsbrukerService;
        this.oppfolgingsbrukerRepositoryV3 = oppfolgingsbrukerRepositoryV3;
    }

    @BeforeEach
    public void setup() {
        db.update("truncate oppfolgingsbruker_arena_v2");
        db.update("truncate oppfolging_data");
        db.update("truncate bruker_identer");
    }


    @Test
    public void skalKonsumereOgLagreDataNaarBrukerErUnderOppfolging() {
        insertOppfolgingsInformasjon(aktorId, fnr);

        LocalDate iserv_fra_dato = ZonedDateTime.now().minusDays(2).toLocalDate();
        ZonedDateTime endret_dato = DateUtils.now();

        OppfolgingsbrukerEntity forventetResultat = new OppfolgingsbrukerEntity(
                fnr.get(),
                "RARBS",
                ZonedDateTime.of(iserv_fra_dato.atStartOfDay(), ZoneId.systemDefault()),
                "007",
                "BKART",
                "AAP",
                "SKAFFEA",
                endret_dato
        );

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr.get()).formidlingsgruppe(Formidlingsgruppe.RARBS).iservFraDato(iserv_fra_dato)
                .oppfolgingsenhet("007").kvalifiseringsgruppe(Kvalifiseringsgruppe.BKART).rettighetsgruppe(Rettighetsgruppe.AAP).hovedmaal(Hovedmaal.SKAFFEA)
                .sperretAnsatt(false).sistEndretDato(endret_dato)
                .build();
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        Optional<OppfolgingsbrukerEntity> oppfolgingsBruker = oppfolgingsbrukerRepositoryV3.getOppfolgingsBruker(fnr);
        assertTrue(oppfolgingsBruker.isPresent());
        assertThat(oppfolgingsBruker.get()).isEqualTo(forventetResultat);
    }

    @Test
    public void skalIkkeKonsumereOgLagreDataNaarBrukerIkkeErUnderOppfolging() {
        LocalDate iserv_fra_dato = ZonedDateTime.now().minusDays(2).toLocalDate();
        ZonedDateTime endret_dato = DateUtils.now();

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr.get()).formidlingsgruppe(Formidlingsgruppe.RARBS).iservFraDato(iserv_fra_dato)
                .oppfolgingsenhet("007").kvalifiseringsgruppe(Kvalifiseringsgruppe.BKART).rettighetsgruppe(Rettighetsgruppe.AAP).hovedmaal(Hovedmaal.SKAFFEA)
                .sperretAnsatt(false).sistEndretDato(endret_dato)
                .build();
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        Optional<OppfolgingsbrukerEntity> oppfolgingsBruker = oppfolgingsbrukerRepositoryV3.getOppfolgingsBruker(fnr);
        assertTrue(oppfolgingsBruker.isEmpty());
    }

    @Test
    public void skalKonsumereDataNaarBrukerErUnderOppfolging() {
        insertOppfolgingsInformasjon(aktorId, fnr);

        ZonedDateTime endret_dato = DateUtils.now();

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr.get()).formidlingsgruppe(Formidlingsgruppe.ARBS).iservFraDato(null)
                .etternavn("Testerson").fornavn("Test").oppfolgingsenhet("0220").kvalifiseringsgruppe(Kvalifiseringsgruppe.IVURD).rettighetsgruppe(Rettighetsgruppe.IYT).hovedmaal(Hovedmaal.SKAFFEA).sikkerhetstiltakType(null)
                .diskresjonskode(null).harOppfolgingssak(false).sperretAnsatt(false).erDoed(false).doedFraDato(null).sistEndretDato(endret_dato)
                .build();
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        Optional<OppfolgingsbrukerEntity> oppfolgingsBruker = oppfolgingsbrukerRepositoryV3.getOppfolgingsBruker(fnr);
        assertTrue(oppfolgingsBruker.isPresent());
    }

    @Test
    public void skalIkkeKonsumereDataNaarBrukerIkkeErUnderOppfolging() {
        ZonedDateTime endret_dato = DateUtils.now();

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr.get()).formidlingsgruppe(Formidlingsgruppe.ARBS).iservFraDato(null)
                .etternavn("Testerson").fornavn("Test").oppfolgingsenhet("0220").kvalifiseringsgruppe(Kvalifiseringsgruppe.IVURD).rettighetsgruppe(Rettighetsgruppe.IYT).hovedmaal(Hovedmaal.SKAFFEA).sikkerhetstiltakType(null)
                .diskresjonskode(null).harOppfolgingssak(false).sperretAnsatt(false).erDoed(false).doedFraDato(null).sistEndretDato(endret_dato)
                .build();
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        Optional<OppfolgingsbrukerEntity> oppfolgingsBruker = oppfolgingsbrukerRepositoryV3.getOppfolgingsBruker(fnr);
        assertTrue(oppfolgingsBruker.isEmpty());
    }
}
