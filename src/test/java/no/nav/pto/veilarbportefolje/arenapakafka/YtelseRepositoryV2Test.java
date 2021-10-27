package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.TypeKafkaYtelse;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseDAO;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesRepositoryV2;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = ApplicationConfigTest.class)
public class YtelseRepositoryV2Test {
    private final YtelsesRepositoryV2 ytelsesRepositoryV2;
    private final AktorId bruker1 = AktorId.of("1000123");
    private final AktorId bruker2 = AktorId.of("1000124");
    private final AktorId bruker3 = AktorId.of("1000125");
    private final Fnr fnr = Fnr.of("12345678912");
    private final PersonId personId = PersonId.of("123");
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate jdbcTemplate;

    public YtelseRepositoryV2Test() {
        jdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate();
        ytelsesRepositoryV2 = new YtelsesRepositoryV2(jdbcTemplate);
    }

    @AfterEach
    public void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE YTELSER");
    }

    @Test
    public void skalHenteYtelserSomStarterIdag() {
        LocalDate iDag = ZonedDateTime.now().toLocalDate();
        LocalDate imorgen = ZonedDateTime.now().toLocalDate().plusDays(1);

        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag));
        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("2", iDag));

        ytelsesRepositoryV2.upsert(bruker2, TypeKafkaYtelse.AAP, lagInnhold("3", iDag));
        ytelsesRepositoryV2.upsert(bruker2, TypeKafkaYtelse.AAP, lagInnhold("4", imorgen));

        ytelsesRepositoryV2.upsert(bruker3, TypeKafkaYtelse.AAP, lagInnhold("5", imorgen));

        List<AktorId> brukere = ytelsesRepositoryV2.hentBrukereMedYtelserSomStarterIDag();

        assertThat(brukere.size()).isEqualTo(2);
        assertThat(brukere.contains(bruker1)).isTrue();
        assertThat(brukere.contains(bruker2)).isTrue();
        assertThat(brukere.contains(bruker3)).isFalse();
    }

    @Test
    public void skalHenteAlleYtelserKnyttetTilEnAktorId() {
        LocalDate iDag = ZonedDateTime.now().toLocalDate();
        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag));
        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("2", iDag));
        ytelsesRepositoryV2.upsert(bruker2, TypeKafkaYtelse.AAP, lagInnhold("3", iDag));

        List<YtelseDAO> ytelser = ytelsesRepositoryV2.getYtelser(bruker1);
        assertThat(ytelser.size()).isEqualTo(2);
    }

    @Test
    public void skalMappeYtelserDAORiktig() {
        LocalDate iDag = ZonedDateTime.now().toLocalDate();
        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag, "Sak1", fnr, personId, 2, 4, 8));

        YtelseDAO ytelse = ytelsesRepositoryV2.getYtelser(bruker1).get(0);


    }

    private YtelsesInnhold lagInnhold(String id, LocalDate startDato) {
        return lagInnhold(id, startDato, "Sak1", fnr, personId);
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

    public static YtelsesInnhold lagInnhold(String vedtaksId, LocalDate startDato, String saksId, Fnr fnr, PersonId personId, Integer ukerIgjen, Integer ukerIgjenPermittert, Integer dagerIgjenUnntak) {
        YtelsesInnhold innhold = new YtelsesInnhold();
        innhold.setFnr(fnr.get());
        innhold.setVedtakId(vedtaksId);
        innhold.setSaksId(saksId);
        innhold.setSakstypeKode("AA");
        innhold.setRettighetstypeKode("AAP");
        innhold.setPersonId(personId.getValue());
        innhold.setFraOgMedDato(new ArenaDato(startDato.toString() + " 00:00:00"));
        innhold.setTilOgMedDato(new ArenaDato("2100-07-13 00:00:00"));
        innhold.setHendelseId(1L);
        innhold.setAntallUkerIgjen(ukerIgjen);
        innhold.setAntallUkerIgjenUnderPermittering(ukerIgjenPermittert);
        innhold.setAntallDagerIgjenUnntak(dagerIgjenUnntak);
        YtelsesDTO dto = new YtelsesDTO();
        dto.setOperationType("I");
        dto.setAfter(innhold);

        return innhold;
    }
}
