package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepositoryV2;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import no.nav.pto_schema.enums.arena.SikkerhetstiltakType;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class OppfolgingsbrukerServiceTest {
    private final JdbcTemplate db;
    private final OppfolginsbrukerRepositoryV2 oppfolginsbrukerRepositoryV2;
    private final OppfolginsbrukerService oppfolginsbrukerService;
    private final AktorClient aktorClientMock;
    private final AktorId aktoerId = randomAktorId();
    private final Fnr fnr = Fnr.ofValidFnr("10108000399"); //TESTFAMILIE

    @Autowired
    public OppfolgingsbrukerServiceTest(@Qualifier("PostgresJdbc") JdbcTemplate db, OppfolginsbrukerRepositoryV2 oppfolginsbrukerRepositoryV2) {
        this.db = db;
        this.oppfolginsbrukerRepositoryV2 = oppfolginsbrukerRepositoryV2;
        aktorClientMock = mock(AktorClient.class);
        oppfolginsbrukerService = new OppfolginsbrukerService(oppfolginsbrukerRepositoryV2, mock(VedtakStatusRepositoryV2.class), mock(OpensearchIndexerV2.class), aktorClientMock);
    }

    @BeforeEach
    public void setup() {
        db.update("DELETE FROM OPPFOLGINGSBRUKER_ARENA");
    }


    @Test
    public void skalKonsumereOgLagreData() {
        Mockito.when(aktorClientMock.hentAktorId(fnr)).thenReturn(aktoerId);

        LocalDate iserv_fra_dato = ZonedDateTime.now().minusDays(2).toLocalDate();
        LocalDate doed_fra_dato = ZonedDateTime.now().minusDays(1).toLocalDate();
        ZonedDateTime endret_dato = DateUtils.now();

        OppfolgingsbrukerEntity forventetResultat = new OppfolgingsbrukerEntity(aktoerId.get(), fnr.get(), "RARBS", ZonedDateTime.of(iserv_fra_dato.atStartOfDay(), ZoneId.systemDefault()),
                "Testerson", "Test", "007", "BKART", "AAP", "SKAFFEA", "FYUS",
                "6", false, false, true, ZonedDateTime.of(doed_fra_dato.atStartOfDay(), ZoneId.systemDefault()), endret_dato);

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr.get()).formidlingsgruppe(Formidlingsgruppe.RARBS).iservFraDato(iserv_fra_dato)
                .etternavn("Testerson").fornavn("Test").oppfolgingsenhet("007").kvalifiseringsgruppe(Kvalifiseringsgruppe.BKART).rettighetsgruppe(Rettighetsgruppe.AAP).hovedmaal(Hovedmaal.SKAFFEA).sikkerhetstiltakType(SikkerhetstiltakType.FYUS)
                .diskresjonskode("6").harOppfolgingssak(false).sperretAnsatt(false).erDoed(true).doedFraDato(doed_fra_dato).sistEndretDato(endret_dato)
                .build();
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        Optional<OppfolgingsbrukerEntity> oppfolgingsBruker = oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId);
        assertTrue(oppfolgingsBruker.isPresent());
        assertThat(oppfolgingsBruker.get()).isEqualTo(forventetResultat);
    }

    @Test
    public void skalKonsumereData() {
        Mockito.when(aktorClientMock.hentAktorId(fnr)).thenReturn(aktoerId);
        ZonedDateTime endret_dato = DateUtils.now();

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr.get()).formidlingsgruppe(Formidlingsgruppe.ARBS).iservFraDato(null)
                .etternavn("Testerson").fornavn("Test").oppfolgingsenhet("0220").kvalifiseringsgruppe(Kvalifiseringsgruppe.IVURD).rettighetsgruppe(Rettighetsgruppe.IYT).hovedmaal(Hovedmaal.SKAFFEA).sikkerhetstiltakType(null)
                .diskresjonskode(null).harOppfolgingssak(true).sperretAnsatt(false).erDoed(false).doedFraDato(null).sistEndretDato(endret_dato)
                .build();
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        Optional<OppfolgingsbrukerEntity> oppfolgingsBruker = oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId);
        assertTrue(oppfolgingsBruker.isPresent());
    }

}
