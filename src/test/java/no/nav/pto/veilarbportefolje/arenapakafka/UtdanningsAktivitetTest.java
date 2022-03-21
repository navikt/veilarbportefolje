package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.UtdanningsAktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.UtdanningsAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.UtdanningsAktivitetInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.postgres.opensearch.AktivitetOpensearchService;
import no.nav.pto.veilarbportefolje.postgres.opensearch.PostgresAktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.PostgresAktivitetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.UtdanningsAktivitetService.mapTilKafkaAktivitetMelding;
import static no.nav.pto.veilarbportefolje.util.DateUtils.FAR_IN_THE_FUTURE_DATE;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class UtdanningsAktivitetTest {
    private final UtdanningsAktivitetService utdanningsAktivitetService;
    private final AktivitetOpensearchService aktivitetOpensearchService;
    private final AktivitetService aktivitetService;
    private final JdbcTemplate jbPostgres;
    private final AktorId aktorId = AktorId.of("1000123");
    private final Fnr fnr = Fnr.of("12345678912");

    @Autowired
    public UtdanningsAktivitetTest(AktivitetService aktivitetService, AktivitetOpensearchService aktivitetOpensearchService, @Qualifier("PostgresJdbc") JdbcTemplate jbPostgres) {
        this.aktivitetOpensearchService = aktivitetOpensearchService;
        this.jbPostgres = jbPostgres;
        this.aktivitetService = aktivitetService;

        AktorClient aktorClient = mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        this.utdanningsAktivitetService = new UtdanningsAktivitetService(aktivitetService, aktorClient, mock(ArenaHendelseRepository.class));
    }

    @BeforeEach
    public void reset() {
        jbPostgres.execute("truncate table aktiviteter");
    }


    @Test
    public void utdannningsaktivitet_skalInnKommeIAktivitet() {
        String utlopsdato = "2040-01-01";
        PostgresAktivitetEntity pre_apostgresAktivitet = PostgresAktivitetMapper.build(aktivitetOpensearchService
                .hentAktivitetData(List.of(aktorId))
                .get(aktorId));

        utdanningsAktivitetService.behandleKafkaMelding(
                new UtdanningsAktivitetDTO()
                        .setAfter(new UtdanningsAktivitetInnhold()
                                .setFnr(fnr.get())
                                .setHendelseId(1)
                                .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                                .setAktivitetperiodeTil(new ArenaDato(utlopsdato))
                                .setEndretDato(new ArenaDato("2021-01-01"))
                                .setAktivitetid("UA-123456789")
                        ));
        PostgresAktivitetEntity post_apostgresAktivitet = PostgresAktivitetMapper.build(aktivitetOpensearchService
                .hentAktivitetData(List.of(aktorId))
                .get(aktorId));

        assertThat(pre_apostgresAktivitet.getAktivitetUtdanningaktivitetUtlopsdato()).isEqualTo(FAR_IN_THE_FUTURE_DATE);
        assertThat(post_apostgresAktivitet.getAktivitetUtdanningaktivitetUtlopsdato().substring(0, 10))
                .isEqualTo(utlopsdato);
    }


    @Test
    public void utdannningsaktivitet_AktiviteterMedPassertTilDatoSkalIkkeLagres() {
        String utlopsdato_igar = LocalDate.now().minusDays(1).toString();
        utdanningsAktivitetService.behandleKafkaMelding(
                new UtdanningsAktivitetDTO()
                        .setAfter(new UtdanningsAktivitetInnhold()
                                .setFnr(fnr.get())
                                .setHendelseId(1)
                                .setAktivitetperiodeFra(new ArenaDato("1990-01-01"))
                                .setAktivitetperiodeTil(new ArenaDato(utlopsdato_igar))
                                .setEndretDato(new ArenaDato("2021-01-01"))
                                .setAktivitetid("UA-1234")
                        ));

        List<AktivitetEntity> aktiviteter = aktivitetOpensearchService
                .hentAktivitetData(List.of(aktorId)).get(aktorId);
        assertThat(aktiviteter).isNull();
    }


    @Test
    public void utdannningsaktivitet_gaarsdagensAktiviteterErIkkeAktive() {
        String utlopsdato_forrigeUke = LocalDate.now().minusDays(7).toString();
        String utlopsdato_igar = LocalDate.now().minusDays(1).toString();
        String utlopsdato_idag = LocalDate.now().toString();
        utdanningsAktivitetService.behandleKafkaMelding(
                new UtdanningsAktivitetDTO()
                        .setAfter(new UtdanningsAktivitetInnhold()
                                .setFnr(fnr.get())
                                .setHendelseId(1)
                                .setAktivitetperiodeFra(new ArenaDato("1990-01-01"))
                                .setAktivitetperiodeTil(new ArenaDato(utlopsdato_idag))
                                .setEndretDato(new ArenaDato("2021-01-01"))
                                .setAktivitetid("UA-123")
                        ));
        // Aktiviteter med passert utlopsdato må lagres direkte for å komme inn i basen
        aktivitetService.upsertOgIndekserUtdanningsAktivitet(
                mapTilKafkaAktivitetMelding(
                        new UtdanningsAktivitetInnhold()
                                .setFnr(fnr.get())
                                .setHendelseId(1)
                                .setAktivitetperiodeFra(new ArenaDato("1990-01-01"))
                                .setAktivitetperiodeTil(new ArenaDato(utlopsdato_igar))
                                .setEndretDato(new ArenaDato("2021-01-01"))
                                .setAktivitetid("UA-1234"),
                        aktorId)
        );
        aktivitetService.upsertOgIndekserUtdanningsAktivitet(
                mapTilKafkaAktivitetMelding(
                        new UtdanningsAktivitetInnhold()
                                .setFnr(fnr.get())
                                .setHendelseId(1)
                                .setAktivitetperiodeFra(new ArenaDato("1990-01-01"))
                                .setAktivitetperiodeTil(new ArenaDato(utlopsdato_forrigeUke))
                                .setEndretDato(new ArenaDato("2021-01-01"))
                                .setAktivitetid("UA-12345"),
                        aktorId)
        );


        List<AktivitetEntity> pre_aktiviteter = aktivitetOpensearchService
                .hentAktivitetData(List.of(aktorId)).get(aktorId);
        aktivitetService.deaktiverUtgatteUtdanningsAktivteter();
        List<AktivitetEntity> post_aktiviteter = aktivitetOpensearchService
                .hentAktivitetData(List.of(aktorId)).get(aktorId);
        assertThat(pre_aktiviteter.size()).isEqualTo(3);
        assertThat(post_aktiviteter.size()).isEqualTo(1);
        assertThat(toIsoUTC(post_aktiviteter.get(0).getUtlop()).substring(0, 10))
                .isEqualTo(utlopsdato_idag);
    }
}
