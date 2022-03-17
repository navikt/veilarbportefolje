package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.TypeKafkaYtelse;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseDAO;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesServicePostgres;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.postgres.AktoerDataOpensearchMapper;
import no.nav.pto.veilarbportefolje.postgres.utils.PostgresAktorIdEntity;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.arenapakafka.YtelseRepositoryTest.lagInnhold;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomPersonId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class YtelserTest extends EndToEndTest {
    private final YtelsesService ytelsesService;
    private final JdbcTemplate jdbcTemplatePostgres;

    private final AktorId aktorId = randomAktorId();
    private final PersonId personId = randomPersonId();
    private final Fnr fnr = randomFnr();
    private final AktorClient aktorClient;
    private final BrukerDataService brukerDataService;
    private final AktoerDataOpensearchMapper aktoerDataOpensearchMapper;

    @Autowired
    public YtelserTest(YtelsesRepositoryV2 ytelsesRepositoryV2, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplatePostgres, BrukerDataService brukerDataService, YtelsesRepository ytelsesRepository, AktoerDataOpensearchMapper aktoerDataOpensearchMapper) {
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.aktoerDataOpensearchMapper = aktoerDataOpensearchMapper;
        ArenaHendelseRepository arenaHendelseRepository = mock(ArenaHendelseRepository.class);
        Mockito.when(arenaHendelseRepository.upsertYtelsesHendelse(anyString(), anyLong())).thenReturn(1);

        this.brukerDataService = brukerDataService;
        this.aktorClient = Mockito.mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        YtelsesServicePostgres ytelsesServicePostgres = new YtelsesServicePostgres(aktorClient, brukerDataService, ytelsesRepositoryV2);
        this.ytelsesService = new YtelsesService(aktorClient, mock(BrukerService.class), brukerDataService, ytelsesRepository, ytelsesServicePostgres, arenaHendelseRepository, mock(OpensearchIndexer.class));
    }

    @BeforeEach
    public void reset() {
        jdbcTemplatePostgres.execute("TRUNCATE TABLE ytelsesvedtak");
        jdbcTemplatePostgres.execute("TRUNCATE TABLE ytelse_status_for_bruker");
    }

    @Test
    public void skalPaAAP() {
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
        testDataClient.setupBruker(aktorId, randomNavKontor(), randomVeilederId(), ZonedDateTime.now());

        ytelsesService.behandleKafkaMeldingPostgres(dto, TypeKafkaYtelse.AAP);

        PostgresAktorIdEntity aktoerData = aktoerDataOpensearchMapper.hentAktoerData(List.of(aktorId)).get(aktorId);
        assertThat(aktoerData.getYtelse()).isEqualTo("AAP_MAXTID");
    }

    @Test
    public void finnLopendeYtelse_sisteUtlopsDatoPaSaksId() {
        String sak1 = "Sak1";
        String sak2 = "Sak2";
        Timestamp yesterday = Timestamp.valueOf(ZonedDateTime.now().minusDays(1).toLocalDateTime());
        Timestamp tomorrow = Timestamp.valueOf(ZonedDateTime.now().plusDays(1).toLocalDateTime());
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());
        Timestamp nextMonth = Timestamp.valueOf(ZonedDateTime.now().plusMonths(1).toLocalDateTime());

        YtelsesRepositoryV2 mockRepositoryV2 = mock(YtelsesRepositoryV2.class);
        YtelsesServicePostgres tempYtelsesServicePostgres = new YtelsesServicePostgres(aktorClient, brukerDataService, mockRepositoryV2);
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(yesterday).setUtlopsDato(tomorrow),
                new YtelseDAO().setSaksId(sak1).setStartDato(tomorrow).setUtlopsDato(nextWeek),
                new YtelseDAO().setSaksId(sak2).setStartDato(nextWeek).setUtlopsDato(nextMonth)
        );
        Mockito.when(mockRepositoryV2.getYtelser(aktorId)).thenReturn(ytelser);


        Optional<YtelseDAO> lopendeYtelsePostgres = tempYtelsesServicePostgres.finnLopendeYtelsePostgres(aktorId);

        assertThat(lopendeYtelsePostgres.isPresent()).isTrue();
        assertThat(lopendeYtelsePostgres.get().getUtlopsDato()).isEqualTo(nextWeek);
    }

    @Test
    public void finnLopendeYtelse_startdatoErIkkePassert() {
        String sak1 = "Sak1";
        Timestamp tomorrow = Timestamp.valueOf(ZonedDateTime.now().plusDays(1).toLocalDateTime());
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());
        Timestamp nextMonth = Timestamp.valueOf(ZonedDateTime.now().plusMonths(1).toLocalDateTime());

        YtelsesRepositoryV2 mockRepositoryV2 = mock(YtelsesRepositoryV2.class);
        YtelsesServicePostgres tempYtelsesServicePostgres = new YtelsesServicePostgres(aktorClient, brukerDataService, mockRepositoryV2);
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(tomorrow).setUtlopsDato(nextWeek),
                new YtelseDAO().setSaksId(sak1).setStartDato(nextWeek).setUtlopsDato(nextMonth)
        );
        Mockito.when(mockRepositoryV2.getYtelser(aktorId)).thenReturn(ytelser);

        Optional<YtelseDAO> lopendeYtelsePostgres = tempYtelsesServicePostgres.finnLopendeYtelsePostgres(aktorId);
        assertThat(lopendeYtelsePostgres.isEmpty()).isTrue();
    }

    @Test
    public void finnLopendeYtelse_skalSetteUtlopsdatoLikNullPaDagpengeytelse() {
        String sak1 = "Sak1";
        Timestamp yesterday = Timestamp.valueOf(ZonedDateTime.now().minusDays(1).toLocalDateTime());
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());

        YtelsesRepositoryV2 mockRepositoryV2 = mock(YtelsesRepositoryV2.class);
        YtelsesServicePostgres tempYtelsesServicePostgres = new YtelsesServicePostgres(aktorClient, brukerDataService, mockRepositoryV2);
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(yesterday).setUtlopsDato(nextWeek).setType(TypeKafkaYtelse.DAGPENGER)
        );
        Mockito.when(mockRepositoryV2.getYtelser(aktorId)).thenReturn(ytelser);

        Optional<YtelseDAO> lopendeYtelsePostgres = tempYtelsesServicePostgres.finnLopendeYtelsePostgres(aktorId);

        assertThat(lopendeYtelsePostgres.get().getUtlopsDato()).isEqualTo((String) null);
    }

    @Test
    public void finnLopendeYtelse_skalPaAAPMedUlopsdatoLikNull() {
        String sak1 = "Sak1";
        Timestamp yesterday = Timestamp.valueOf(ZonedDateTime.now().minusDays(1).toLocalDateTime());

        YtelsesRepositoryV2 mockRepositoryV2 = mock(YtelsesRepositoryV2.class);
        YtelsesServicePostgres tempYtelsesServicePostgres = new YtelsesServicePostgres(aktorClient, brukerDataService, mockRepositoryV2);
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak1).setStartDato(yesterday).setType(TypeKafkaYtelse.AAP)
        );
        Mockito.when(mockRepositoryV2.getYtelser(aktorId)).thenReturn(ytelser);

        Optional<YtelseDAO> lopendeYtelsePostgres = tempYtelsesServicePostgres.finnLopendeYtelsePostgres(aktorId);

        assertThat(lopendeYtelsePostgres.get().getType()).isEqualTo(TypeKafkaYtelse.AAP);
        assertThat(lopendeYtelsePostgres.get().getUtlopsDato()).isEqualTo((String) null);
    }

    @Test
    public void sletterLopendeYtelseOgAktivererNesteVedtakPaSammeSak() {
        String sak1 = "Sak1";
        String sak2 = "Sak2";
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());
        Timestamp nextMonth = Timestamp.valueOf(ZonedDateTime.now().plusMonths(1).toLocalDateTime());

        YtelsesRepositoryV2 mockRepositoryV2 = mock(YtelsesRepositoryV2.class);
        YtelsesServicePostgres tempYtelsesServicePostgres = new YtelsesServicePostgres(aktorClient, brukerDataService, mockRepositoryV2);
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
        Mockito.when(mockRepositoryV2.getYtelser(aktorId)).thenReturn(ytelser);

        YtelsesInnhold sletteInnhold = lagInnhold("1", LocalDate.now(), sak1, fnr, personId);
        Optional<YtelseDAO> lopendeYtelseP = tempYtelsesServicePostgres.oppdaterYtelsesInformasjonMedUnntaksLogikkForSletting(aktorId, sletteInnhold);

        assertThat(lopendeYtelseP.get().getSaksId()).isEqualTo(sak1);
        assertThat(lopendeYtelseP.get().getUtlopsDato()).isEqualTo(nextMonth);
    }

    @Test
    public void sletterLopendeYtelse() {
        String sak1 = "Sak1";
        String sak2 = "Sak2";
        Timestamp nextWeek = Timestamp.valueOf(ZonedDateTime.now().plusWeeks(1).toLocalDateTime());

        YtelsesRepositoryV2 mockRepositoryV2 = mock(YtelsesRepositoryV2.class);
        YtelsesServicePostgres tempYtelsesServicePostgres = new YtelsesServicePostgres(aktorClient, brukerDataService, mockRepositoryV2);
        List<YtelseDAO> ytelser = List.of(
                new YtelseDAO().setSaksId(sak2).setStartDato(nextWeek).setUtlopsDato(nextWeek)
                        .setType(TypeKafkaYtelse.DAGPENGER)
                        .setRettighetstypeKode("LONN")
                        .setSakstypeKode("DAGP")
        );
        Mockito.when(mockRepositoryV2.getYtelser(aktorId)).thenReturn(ytelser);

        YtelsesInnhold sletteInnhold = lagInnhold("1", LocalDate.now(), sak1, fnr, personId);
        Optional<YtelseDAO> lopendeYtelseP = tempYtelsesServicePostgres.oppdaterYtelsesInformasjonMedUnntaksLogikkForSletting(aktorId, sletteInnhold);

        assertThat(lopendeYtelseP.isEmpty()).isTrue();
    }

    @Test
    public void skalByggeKorrektDagpengejson() {
        String goldenGateDtoString = readFileAsJsonString("/goldenGateDagpenger.json", getClass());
        YtelsesDTO goldenGateDTO = fromJson(goldenGateDtoString, YtelsesDTO.class);
        assertThat(goldenGateDTO.getAfter().getClass()).isEqualTo(YtelsesInnhold.class);
        assertThat(goldenGateDTO.getAfter()).isNotNull();
        assertThat(goldenGateDTO.getBefore()).isNull();
    }
}
