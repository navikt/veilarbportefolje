package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.TypeKafkaYtelse;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesRepository;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.util.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.database.Table.YTELSER.TABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class YtelseRepositoryTest {
    private static YtelsesRepository ytelsesRepository;
    private static JdbcTemplate jdbcTemplate;
    private final AktorId bruker1 = AktorId.of("1000123");
    private final AktorId bruker2 = AktorId.of("1000124");
    private final AktorId bruker3 = AktorId.of("1000125");
    private final Fnr fnr = Fnr.of("12345678912");
    private final PersonId personId = PersonId.of("123");

    @BeforeAll
    public static void beforeEach() {
        SingleConnectionDataSource ds = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(ds);
        ytelsesRepository = new YtelsesRepository(jdbcTemplate);
    }

    @AfterEach
    public void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE " + TABLE_NAME);
    }


    @Test
    public void skalHenteYtelserSomStarterIdag() {
        LocalDate iDag = ZonedDateTime.now().toLocalDate();
        LocalDate imorgen = ZonedDateTime.now().toLocalDate().plusDays(1);

        ytelsesRepository.upsertYtelse(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag));
        ytelsesRepository.upsertYtelse(bruker1, TypeKafkaYtelse.AAP, lagInnhold("2", iDag));

        ytelsesRepository.upsertYtelse(bruker2, TypeKafkaYtelse.AAP, lagInnhold("3", iDag));
        ytelsesRepository.upsertYtelse(bruker2, TypeKafkaYtelse.AAP, lagInnhold("4", imorgen));

        ytelsesRepository.upsertYtelse(bruker3, TypeKafkaYtelse.AAP, lagInnhold("5", imorgen));

        List<AktorId> brukere = ytelsesRepository.hentBrukereMedYtelserSomStarterIDag();

        assertThat(brukere.size()).isEqualTo(2);
        assertThat(brukere.contains(bruker1)).isTrue();
        assertThat(brukere.contains(bruker2)).isTrue();
        assertThat(brukere.contains(bruker3)).isFalse();
    }

    private YtelsesInnhold lagInnhold(String id, LocalDate startDato) {
        return lagInnhold(id, startDato,"Sak1", fnr, personId);
    }

    public static YtelsesInnhold lagInnhold(String vedtakId, LocalDate startDato, String sakId, Fnr fnr, PersonId personId) {
        YtelsesInnhold innhold = new YtelsesInnhold();
        innhold.setFnr(fnr.get());
        innhold.setVedtakId(vedtakId);
        innhold.setSaksId(sakId);
        innhold.setSakstypeKode("AA");
        innhold.setRettighetstypeKode("AAP");
        innhold.setPersonId(personId.getValue());
        innhold.setFraOgMedDato(new ArenaDato(startDato.toString() + " 00:00:00"));
        innhold.setTilOgMedDato(new ArenaDato("2100-07-13 00:00:00"));
        innhold.setHendelseId(1L);
        YtelsesDTO dto = new YtelsesDTO();
        dto.setOperationType("I");
        dto.setAfter(innhold);

        return innhold;
    }
}
