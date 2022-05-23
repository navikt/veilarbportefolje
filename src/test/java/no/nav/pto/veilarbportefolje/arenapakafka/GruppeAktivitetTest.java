package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetsType;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.postgres.AktivitetOpensearchService;
import no.nav.pto.veilarbportefolje.postgres.utils.AvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.pto.veilarbportefolje.util.DateUtils.FAR_IN_THE_FUTURE_DATE;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomPersonId;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class GruppeAktivitetTest {
    private final GruppeAktivitetService gruppeAktivitetService;
    private final AktivitetOpensearchService aktivitetOpensearchService;
    private final JdbcTemplate jdbcTemplate;

    private final AktorId aktorId;
    private final Fnr fnr;
    private final PersonId personId;

    @Autowired
    public GruppeAktivitetTest(@Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate, GruppeAktivitetRepositoryV2 gruppeAktivitetRepositoryV2, AktivitetOpensearchService aktivitetOpensearchService) {
        this.aktivitetOpensearchService = aktivitetOpensearchService;
        this.jdbcTemplate = jdbcTemplate;
        this.personId = randomPersonId();
        this.aktorId = randomAktorId();
        this.fnr = randomFnr();

        AktorClient aktorClient = mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        this.gruppeAktivitetService = new GruppeAktivitetService(gruppeAktivitetRepositoryV2, mock(OpensearchIndexer.class), aktorClient);
    }

    @BeforeEach
    public void reset() {
        jdbcTemplate.execute("truncate table oppfolgingsbruker_arena_v2");
        jdbcTemplate.execute("truncate table gruppe_aktiviter");
        jdbcTemplate.execute("truncate table oppfolging_data");
    }


    @Test
    public void skal_komme_i_gruppe_aktivitet() {
        GruppeAktivitetDTO gruppeAktivitet = getInsertDTO();
        gruppeAktivitetService.behandleKafkaMelding(gruppeAktivitet);

        AvtaltAktivitetEntity postgresAktivitet = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(aktivitetOpensearchService
                .hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId));

        //Opensearch mapping
        Assertions.assertThat(postgresAktivitet.getTiltak().size()).isEqualTo(0);
        Assertions.assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetsType.gruppeaktivitet.name())).isTrue();

        Assertions.assertThat(postgresAktivitet.getAktivitetGruppeaktivitetUtlopsdato()).isNotNull();
        Assertions.assertThat(postgresAktivitet.getNesteAktivitetStart()).isNull();
        Assertions.assertThat(postgresAktivitet.getAktivitetStart()).isNull();
    }

    @Test
    public void skal_ut_av_aktivitet() {
        gruppeAktivitetService.behandleKafkaMelding(getInsertDTO());

        AvtaltAktivitetEntity aktiviteter_pre = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(
                aktivitetOpensearchService.hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId));
        gruppeAktivitetService.behandleKafkaMelding(getDeleteDTO());

        AvtaltAktivitetEntity aktiviteter_post = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(
                aktivitetOpensearchService.hentAvtaltAktivitetData(List.of(aktorId))
                        .get(aktorId));

        Assertions.assertThat(aktiviteter_pre.getAktiviteter().size()).isEqualTo(1);
        Assertions.assertThat(aktiviteter_pre.getAktiviteter().contains(AktivitetsType.gruppeaktivitet.name())).isTrue();

        Assertions.assertThat(aktiviteter_post.getAktiviteter().size()).isEqualTo(0);
        Assertions.assertThat(aktiviteter_post.getAktivitetGruppeaktivitetUtlopsdato()).isEqualTo(FAR_IN_THE_FUTURE_DATE);
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
}

