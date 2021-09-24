package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.TypeKafkaYtelse;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseDAO;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.sbl.sql.SqlUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.arenapakafka.YtelseRepositoryTest.lagInnhold;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class YtelserTest {
    private final YtelsesService ytelsesService;
    private final JdbcTemplate jdbcTemplate;
    private final UnleashService unleashService;

    private final AktorId aktorId = AktorId.of("1000123");
    private final Fnr fnr = Fnr.of("12345678912");
    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId testEnhet = EnhetId.of("0000");
    private final PersonId personId = PersonId.of("123");
    private final BrukerRepository brukerRepository;
    private final AktorClient aktorClient;
    private final BrukerDataService brukerDataService;

    @Autowired
    public YtelserTest(JdbcTemplate jdbcTemplate, BrukerDataService brukerDataService, YtelsesRepository ytelsesRepository, BrukerRepository brukerRepository, UnleashService unleashService) {
        this.jdbcTemplate = jdbcTemplate;
        this.brukerRepository = brukerRepository;
        this.unleashService = unleashService;
        ArenaHendelseRepository arenaHendelseRepository = mock(ArenaHendelseRepository.class);
        Mockito.when(arenaHendelseRepository.upsertYtelsesHendelse(anyString(), anyLong())).thenReturn(1);

        this.brukerDataService = brukerDataService;
        this.aktorClient = Mockito.mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        this.ytelsesService = new YtelsesService(aktorClient, mock(BrukerService.class), brukerDataService, ytelsesRepository,arenaHendelseRepository,mock(ElasticIndexer.class), unleashService, mock(OppfolgingRepository.class));
    }

    @BeforeEach
    public void reset() {
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGING_DATA.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.YTELSER.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.BRUKER_DATA.TABLE_NAME);
    }

    @Test
    public void skalPaAAP() {
        insertBruker();

        YtelsesInnhold innhold = new YtelsesInnhold();
        innhold.setFnr(fnr.get());
        innhold.setVedtakId("1");
        innhold.setSaksId("S1");
        innhold.setSakstypeKode("AA");
        innhold.setRettighetstypeKode("AAP");
        innhold.setPersonId(personId.getValue());
        innhold.setFraOgMedDato(new ArenaDato("2020-07-13 00:00:00"));
        innhold.setTilOgMedDato(new ArenaDato("2100-07-13 00:00:00"));
        innhold.setHendelseId(1L);
        YtelsesDTO dto = new YtelsesDTO();
        dto.setOperationType("I");
        dto.setAfter(innhold);

        ytelsesService.behandleKafkaMelding(dto, TypeKafkaYtelse.AAP);
        OppfolgingsBruker bruker = brukerRepository.hentBrukerFraView(aktorId).get();

        assertThat(bruker.getYtelse()).isEqualTo("AAP_MAXTID");
    }

    @Test
    public void finnSisteUtlopsDatoPaSak_basertPaSaksId() {
        String sak1 = "Sak1";
        String sak2 = "Sak2";
        Timestamp yesterday = Timestamp.valueOf(ZonedDateTime.now().minusDays(1).toLocalDateTime());
        Timestamp tomorrow = Timestamp.valueOf(ZonedDateTime.now().plusDays(1).toLocalDateTime());
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());
        Timestamp nextMonth = Timestamp.valueOf(ZonedDateTime.now().plusMonths(1).toLocalDateTime());

        YtelsesRepository mockRepository = mock(YtelsesRepository.class);
        YtelsesService tempYtelsesService = new YtelsesService(aktorClient, mock(BrukerService.class), brukerDataService, mockRepository, mock(ArenaHendelseRepository.class), mock(ElasticIndexer.class), unleashService, mock(OppfolgingRepository.class));
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(yesterday).setUtlopsDato(tomorrow),
                new YtelseDAO().setSaksId(sak1).setStartDato(tomorrow).setUtlopsDato(nextWeek),
                new YtelseDAO().setSaksId(sak2).setStartDato(nextWeek).setUtlopsDato(nextMonth)
        );
        Mockito.when(mockRepository.getYtelser(aktorId)).thenReturn(ytelser);

        Optional<YtelseDAO> lopendeYtelse = tempYtelsesService.finnLopendeYtelse(aktorId);

        assertThat(lopendeYtelse.isPresent()).isTrue();
        assertThat(lopendeYtelse.get().getUtlopsDato()).isEqualTo(nextWeek);
    }

    @Test
    public void finnSisteUtlopsDatoPaSak_startdatoErIkkePasert() {
        String sak1 = "Sak1";
        Timestamp tomorrow = Timestamp.valueOf(ZonedDateTime.now().plusDays(1).toLocalDateTime());
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());
        Timestamp nextMonth = Timestamp.valueOf(ZonedDateTime.now().plusMonths(1).toLocalDateTime());

        YtelsesRepository mockRepository = mock(YtelsesRepository.class);
        YtelsesService tempYtelsesService = new YtelsesService(aktorClient, mock(BrukerService.class), brukerDataService, mockRepository, mock(ArenaHendelseRepository.class), mock(ElasticIndexer.class), unleashService, mock(OppfolgingRepository.class));
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(tomorrow).setUtlopsDato(nextWeek),
                new YtelseDAO().setSaksId(sak1).setStartDato(nextWeek).setUtlopsDato(nextMonth)
        );
        Mockito.when(mockRepository.getYtelser(aktorId)).thenReturn(ytelser);

        Optional<YtelseDAO> lopendeYtelse = tempYtelsesService.finnLopendeYtelse(aktorId);

        assertThat(lopendeYtelse.isEmpty()).isTrue();
    }

    @Test
    public void skalPaDagpengerMedUlopsdatoLikNull() {
        String sak1 = "Sak1";
        Timestamp yesterday = Timestamp.valueOf(ZonedDateTime.now().minusDays(1).toLocalDateTime());

        YtelsesRepository mockRepository = mock(YtelsesRepository.class);
        YtelsesService tempYtelsesService = new YtelsesService(aktorClient, mock(BrukerService.class), brukerDataService, mockRepository, mock(ArenaHendelseRepository.class), mock(ElasticIndexer.class), unleashService, mock(OppfolgingRepository.class));
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(yesterday).setType(TypeKafkaYtelse.DAGPENGER)
        );
        Mockito.when(mockRepository.getYtelser(aktorId)).thenReturn(ytelser);
        Optional<YtelseDAO> lopendeYtelse = tempYtelsesService.finnLopendeYtelse(aktorId);

        assertThat(lopendeYtelse.isPresent()).isTrue();
    }

    @Test
    public void skalPaAAPMedUlopsdatoLikNull() {
        String sak1 = "Sak1";
        Timestamp yesterday = Timestamp.valueOf(ZonedDateTime.now().minusDays(1).toLocalDateTime());

        YtelsesRepository mockRepository = mock(YtelsesRepository.class);
        YtelsesService tempYtelsesService = new YtelsesService(aktorClient, mock(BrukerService.class), brukerDataService, mockRepository, mock(ArenaHendelseRepository.class), mock(ElasticIndexer.class), unleashService, mock(OppfolgingRepository.class));
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(yesterday).setType(TypeKafkaYtelse.AAP)
        );
        Mockito.when(mockRepository.getYtelser(aktorId)).thenReturn(ytelser);
        Optional<YtelseDAO> lopendeYtelse = tempYtelsesService.finnLopendeYtelse(aktorId);

        assertThat(lopendeYtelse.isPresent()).isTrue();
    }

    @Test
    public void sletterLøpendeYtelseOgAktivererNesteVedtakPåSammeSak(){
        String sak1 = "Sak1";
        String sak2 = "Sak2";
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());
        Timestamp nextMonth = Timestamp.valueOf(ZonedDateTime.now().plusMonths(1).toLocalDateTime());

        YtelsesRepository mockRepository = mock(YtelsesRepository.class);
        YtelsesService tempYtelsesService = new YtelsesService(aktorClient, mock(BrukerService.class), brukerDataService, mockRepository, mock(ArenaHendelseRepository.class), mock(ElasticIndexer.class), unleashService, mock(OppfolgingRepository.class));
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(nextWeek).setUtlopsDato(nextMonth)
                        .setType(TypeKafkaYtelse.DAGPENGER)
                        .setRettighetstypeKode("LONN")
                        .setSakstypeKode("DAGP"),
                new YtelseDAO().setSaksId(sak2).setStartDato(nextWeek).setUtlopsDato(nextWeek)
                        .setType(TypeKafkaYtelse.DAGPENGER)
                        .setRettighetstypeKode("LONN")
                        .setSakstypeKode("DAGP")
        );
        Mockito.when(mockRepository.getYtelser(aktorId)).thenReturn(ytelser);

        YtelsesInnhold sletteInnhold = lagInnhold("1", LocalDate.now(), sak1, fnr, personId);
        Optional<YtelseDAO> lopendeYtelse = tempYtelsesService.oppdaterYtelsesInformasjonMedUntaksLoggikForSletting(aktorId, sletteInnhold);

        assertThat(lopendeYtelse.isPresent()).isTrue();
        assertThat(lopendeYtelse.get().getSaksId()).isEqualTo(sak1);
        assertThat(lopendeYtelse.get().getUtlopsDato()).isEqualTo(nextMonth);

    }
    
    @Test
    public void sletterLøpendeYtelse(){
        String sak1 = "Sak1";
        String sak2 = "Sak2";
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());

        YtelsesRepository mockRepository = mock(YtelsesRepository.class);
        YtelsesService tempYtelsesService = new YtelsesService(aktorClient, mock(BrukerService.class), brukerDataService, mockRepository, mock(ArenaHendelseRepository.class), mock(ElasticIndexer.class), unleashService, mock(OppfolgingRepository.class));
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak2).setStartDato(nextWeek).setUtlopsDato(nextWeek)
                        .setType(TypeKafkaYtelse.DAGPENGER)
                        .setRettighetstypeKode("LONN")
                        .setSakstypeKode("DAGP")
        );
        Mockito.when(mockRepository.getYtelser(aktorId)).thenReturn(ytelser);

        YtelsesInnhold sletteInnhold = lagInnhold("1", LocalDate.now(), sak1, fnr, personId);
        Optional<YtelseDAO> lopendeYtelse = tempYtelsesService.oppdaterYtelsesInformasjonMedUntaksLoggikForSletting(aktorId, sletteInnhold);

        assertThat(lopendeYtelse.isEmpty()).isTrue();
    }

    @Test
    public void skalByggeKorrektDagpengejson() {
        String goldenGateDtoString = readFileAsJsonString("/goldenGateDagpenger.json", getClass());

        YtelsesDTO goldenGateDTO = fromJson(goldenGateDtoString, YtelsesDTO.class);
        assertThat(goldenGateDTO.getAfter().getClass()).isEqualTo(YtelsesInnhold.class);
        assertThat(goldenGateDTO.getAfter()).isNotNull();
        assertThat(goldenGateDTO.getBefore()).isNull();
        ytelsesService.behandleKafkaMelding(goldenGateDTO, TypeKafkaYtelse.AAP);
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
