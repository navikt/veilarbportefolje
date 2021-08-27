package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakServiceV2;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.sbl.sql.SqlUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class TiltakTest {
    private final TiltakServiceV2 tiltakServiceV2;
    private final JdbcTemplate jdbcTemplate;
    private final AktivitetDAO aktivitetDAO;

    private final AktorId aktorId = AktorId.of("1000123");
    private final Fnr fnr = Fnr.of("12345678912");
    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId testEnhet = EnhetId.of("0000");
    private final EnhetId annenEnhet = EnhetId.of("0001");
    private final PersonId personId = PersonId.of("123");


    @Autowired
    public TiltakTest(TiltakRepositoryV2 tiltakRepositoryV2, JdbcTemplate jdbcTemplate, AktivitetDAO aktivitetDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktivitetDAO = aktivitetDAO;

        ArenaHendelseRepository arenaHendelseRepository = mock(ArenaHendelseRepository.class);
        Mockito.when(arenaHendelseRepository.upsertAktivitetHendelse(anyString(), anyLong())).thenReturn(1);
        AktorClient aktorClient = Mockito.mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);

        this.tiltakServiceV2 = new TiltakServiceV2(tiltakRepositoryV2, aktorClient, arenaHendelseRepository, mock(BrukerDataService.class), mock(ElasticIndexer.class));
    }


    @BeforeEach
    public void reset() {
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGING_DATA.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.BRUKERTILTAK_V2.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.TILTAKKODEVERK_V2.TABLE_NAME);
        jdbcTemplate.execute("truncate table BRUKERSTATUS_AKTIVITETER");
    }

    @Test
    public void skal_komme_i_tiltak() {
        insertBruker();
        TiltakDTO tiltakDTO = new TiltakDTO()
                .setAfter(new TiltakInnhold()
                        .setFnr(fnr.get())
                        .setPersonId(personId.toInteger())
                        .setHendelseId(1)
                        .setTiltaksnavn("Test")
                        .setTiltakstype("T123")
                        .setDeltakerStatus("GJENN")
                        .setEndretDato(new ArenaDato("2021-01-01"))
                        .setAktivitetid("TA-123456789")
                );
        tiltakDTO.setOperationType(GoldenGateOperations.INSERT);
        tiltakServiceV2.behandleKafkaMelding(tiltakDTO);

        Optional<AktivitetStatus> tiltak = hentAktivitetStatus();
        assertThat(tiltak).isPresent();
    }


    @Test
    public void skal_ha_tiltak_pa_enhet() {
        insertBruker();
        TiltakDTO tiltakDTO = new TiltakDTO()
                .setAfter(new TiltakInnhold()
                        .setFnr(fnr.get())
                        .setPersonId(personId.toInteger())
                        .setHendelseId(1)
                        .setTiltaksnavn("Test")
                        .setTiltakstype("T123")
                        .setDeltakerStatus("GJENN")
                        .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                        .setAktivitetperiodeTil(new ArenaDato("2030-01-01"))
                        .setEndretDato(new ArenaDato("2021-01-01"))
                        .setAktivitetid("TA-123456789")
                );
        tiltakDTO.setOperationType(GoldenGateOperations.INSERT);
        tiltakServiceV2.behandleKafkaMelding(tiltakDTO);
        EnhetTiltak enhetTiltak = tiltakServiceV2.hentEnhettiltak(testEnhet);
        EnhetTiltak annenEnhetTiltak = tiltakServiceV2.hentEnhettiltak(annenEnhet);

        assertThat(enhetTiltak.getTiltak().size()).isEqualTo(1);
        assertThat(annenEnhetTiltak.getTiltak().size()).isEqualTo(0);
        assertThat(enhetTiltak.getTiltak().get("T123")).isEqualTo("Test");
    }

    private Optional<AktivitetStatus> hentAktivitetStatus() {
        Set<AktivitetStatus> aktivitetstatusForBrukere = aktivitetDAO.getAktivitetstatusForBrukere(List.of(personId)).get(personId);
        if(aktivitetstatusForBrukere == null){
            return Optional.empty();
        }
        return aktivitetstatusForBrukere.stream()
                .filter(AktivitetStatus::isAktiv)
                .filter(x -> x.getAktivitetType().equals(AktivitetTyper.tiltak.name()))
                .findFirst();
    }

    private void insertBruker() {
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, testEnhet.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .execute();
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktorId.toString())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .value(Table.OPPFOLGING_DATA.VEILEDERIDENT, veilederId.toString())
                .execute();
        SqlUtils.insert(jdbcTemplate, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktorId.toString())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.toString())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, 1)
                .execute();
    }
}
