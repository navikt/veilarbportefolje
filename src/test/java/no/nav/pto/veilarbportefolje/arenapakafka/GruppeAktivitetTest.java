package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetType;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.opensearch.AktivitetOpensearchMapper;
import no.nav.pto.veilarbportefolje.postgres.opensearch.PostgresAktivitetEntity;
import no.nav.sbl.sql.SqlUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomPersonId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class GruppeAktivitetTest {
    private final GruppeAktivitetService gruppeAktivitetService;
    private final AktivitetOpensearchMapper aktivitetOpensearchMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AktivitetDAO aktivitetDAO;

    private final AktorId aktorId;
    private final Fnr fnr;
    private final VeilederId veilederId;
    private final EnhetId testEnhet = EnhetId.of("0000");
    private final PersonId personId;

    @Autowired
    public GruppeAktivitetTest(AktivitetDAO aktivitetDAO, JdbcTemplate jdbcTemplate, GruppeAktivitetRepository gruppeAktivitetRepository, GruppeAktivitetRepositoryV2 gruppeAktivitetRepositoryV2, AktivitetOpensearchMapper aktivitetOpensearchMapper) {
        this.aktivitetOpensearchMapper = aktivitetOpensearchMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.aktivitetDAO = aktivitetDAO;
        this.veilederId = randomVeilederId();
        this.personId = randomPersonId();
        this.aktorId = randomAktorId();
        this.fnr = randomFnr();

        AktorClient aktorClient = mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        this.gruppeAktivitetService = new GruppeAktivitetService(gruppeAktivitetRepository, gruppeAktivitetRepositoryV2, aktorClient, mock(BrukerDataService.class), mock(OpensearchIndexer.class));
    }

    @BeforeEach
    public void reset() {
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGING_DATA.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.GRUPPE_AKTIVITER.TABLE_NAME);
        jdbcTemplate.execute("truncate table BRUKERSTATUS_AKTIVITETER");
    }

    @Test
    public void skal_komme_i_gruppe_aktivitet() {
        insertBruker();
        GruppeAktivitetDTO gruppeAktivitet = getInsertDTO();
        gruppeAktivitetService.behandleKafkaMeldingOracle(gruppeAktivitet);

        Optional<AktivitetStatus> gruppe = hentAktivitetStatus();
        assertThat(gruppe).isPresent();
    }

    @Test
    public void skal_komme_i_gruppe_aktivitet_V2() {
        GruppeAktivitetDTO gruppeAktivitet = getInsertDTO();
        gruppeAktivitetService.behandleKafkaMeldingPostgres(gruppeAktivitet);

        PostgresAktivitetEntity postgresAktivitet = aktivitetOpensearchMapper
                .mapBulk(List.of(new OppfolgingsBruker().setAktoer_id(aktorId.get())))
                .get(aktorId.get())
                .bygg();

        //Opensearch mapping
        Assertions.assertThat(postgresAktivitet.getTiltak().size()).isEqualTo(0);
        Assertions.assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetType.gruppeaktivitet.name())).isTrue();

        Assertions.assertThat(postgresAktivitet.getAktivitetGruppeaktivitetUtlopsdato()).isNotNull();
        Assertions.assertThat(postgresAktivitet.getNesteAktivitetStart()).isNull();
        Assertions.assertThat(postgresAktivitet.getAktivitetStart()).isNull();
    }

    @Test
    public void skal_ut_av_aktivitet() {
        insertBruker();
        gruppeAktivitetService.behandleKafkaMeldingOracle(getInsertDTO());

        Optional<AktivitetStatus> utdanningPre = hentAktivitetStatus();
        gruppeAktivitetService.behandleKafkaMeldingOracle(getDeleteDTO());
        Optional<AktivitetStatus> utdanningPost = hentAktivitetStatus();

        assertThat(utdanningPre).isPresent();
        assertThat(utdanningPost).isEmpty();
    }

    private GruppeAktivitetDTO getInsertDTO() {
        return new GruppeAktivitetDTO()
                .setAfter(new GruppeAktivitetInnhold()
                        .setVeiledningdeltakerId("1")
                        .setMoteplanId("1")
                        .setFnr(fnr.get())
                        .setPersonId(personId.toInteger())
                        .setHendelseId(1)
                        .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                        .setAktivitetperiodeTil(new ArenaDato("2030-01-01"))
                        .setEndretDato(new ArenaDato("2021-01-01"))
                        .setAktivitetid("UA-123456789")
                );
    }

    private GruppeAktivitetDTO getDeleteDTO() {
        return new GruppeAktivitetDTO()
                .setBefore(new GruppeAktivitetInnhold()
                        .setVeiledningdeltakerId("1")
                        .setMoteplanId("1")
                        .setFnr(fnr.get())
                        .setPersonId(personId.toInteger())
                        .setHendelseId(1)
                        .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                        .setAktivitetperiodeTil(new ArenaDato("2030-01-01"))
                        .setEndretDato(new ArenaDato("2021-01-01"))
                        .setAktivitetid("UA-123456789")
                );
    }

    private Optional<AktivitetStatus> hentAktivitetStatus() {
        Set<AktivitetStatus> aktivitetstatusForBrukere = aktivitetDAO.getAktivitetstatusForBrukere(List.of(personId)).get(personId);
        if (aktivitetstatusForBrukere == null) {
            return Optional.empty();
        }
        return aktivitetstatusForBrukere.stream()
                .filter(AktivitetStatus::isAktiv)
                .filter(x -> x.getAktivitetType().equals(AktivitetType.gruppeaktivitet.name()))
                .findFirst();
    }

    private void insertBruker() {
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.get())
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, testEnhet.get())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.getValue())
                .execute();
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktorId.get())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .value(Table.OPPFOLGING_DATA.VEILEDERIDENT, veilederId.getValue())
                .execute();
        SqlUtils.insert(jdbcTemplate, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktorId.get())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.getValue())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, 1)
                .execute();
    }
}

