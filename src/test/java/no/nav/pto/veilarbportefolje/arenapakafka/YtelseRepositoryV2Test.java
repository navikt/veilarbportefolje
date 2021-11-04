package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
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

import java.sql.Timestamp;
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
        jdbcTemplate.execute("TRUNCATE TABLE YTELSESVEDTAK");
    }

    @Test
    public void skalOppretteYtelsesvedtak() {
        LocalDate iDag = ZonedDateTime.now().toLocalDate();

        List<YtelseDAO> ytelser = ytelsesRepositoryV2.getYtelser(bruker1);
        assertThat(ytelser.size()).isEqualTo(0);

        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag));

        ytelser = ytelsesRepositoryV2.getYtelser(bruker1);
        assertThat(ytelser.size()).isEqualTo(1);
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
    public void skalMappeYtelserDAO() {
        LocalDate iDag = ZonedDateTime.now().toLocalDate();
        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag, "Sak1", fnr, personId, 2, 4, 8));

        YtelseDAO ytelse = ytelsesRepositoryV2.getYtelser(bruker1).get(0);
        assertThat(ytelse.getAktorId()).isEqualTo(bruker1);
        assertThat(ytelse.getPersonId()).isEqualTo(personId);
        assertThat(ytelse.getType()).isEqualTo(TypeKafkaYtelse.AAP);
        assertThat(ytelse.getSaksId()).isEqualTo("Sak1");
        assertThat(ytelse.getSakstypeKode()).isEqualTo("AA");
        assertThat(ytelse.getRettighetstypeKode()).isEqualTo("AAP");
        assertThat(ytelse.getStartDato()).isEqualTo(Timestamp.valueOf(iDag.toString() + " 00:00:00"));
        assertThat(ytelse.getUtlopsDato()).isEqualTo(Timestamp.valueOf("2100-07-13 23:59:59"));
        assertThat(ytelse.getAntallUkerIgjenPermittert()).isEqualTo(4);
        assertThat(ytelse.getAntallDagerIgjenUnntak()).isEqualTo(8);
    }

    @Test
    public void skalOppdatereYtelsesvedtak() {
        LocalDate iDag = ZonedDateTime.now().toLocalDate();
        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag));
        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag, "Sak1", fnr, personId, 2, 4, 8));

        List<YtelseDAO> ytelser = ytelsesRepositoryV2.getYtelser(bruker1);
        assertThat(ytelser.size()).isEqualTo(1);
        assertThat(ytelser.get(0).getAntallUkerIgjen()).isEqualTo(2);
        assertThat(ytelser.get(0).getAntallUkerIgjenPermittert()).isEqualTo(4);
        assertThat(ytelser.get(0).getAntallDagerIgjenUnntak()).isEqualTo(8);
    }

    @Test
    public void skalSletteYtelse() {
        LocalDate iDag = ZonedDateTime.now().toLocalDate();
        ytelsesRepositoryV2.upsert(bruker1, TypeKafkaYtelse.AAP, lagInnhold("1", iDag));

        ytelsesRepositoryV2.slettYtelse("1");
        List<YtelseDAO> ytelser = ytelsesRepositoryV2.getYtelser(bruker1);
        assertThat(ytelser.size()).isEqualTo(0);
    }


    private YtelsesInnhold lagInnhold(String vedtaksId, LocalDate startDato) {
        return lagInnhold(vedtaksId, startDato, "Sak1", fnr, personId, 0, 0, 0);
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

        return innhold;
    }
}
