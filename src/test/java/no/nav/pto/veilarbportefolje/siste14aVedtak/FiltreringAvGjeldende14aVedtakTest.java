package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.ArenaHovedmal;
import no.nav.pto.veilarbportefolje.domene.GjeldendeIdenter;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerDTO;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.postgres.PostgresOpensearchMapper;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.time.ZonedDateTime;
import java.util.*;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FiltreringAvGjeldende14aVedtakTest {

    private final Siste14aVedtakRepository siste14aVedtakRepository = mock(Siste14aVedtakRepository.class);
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2 = mock(OppfolgingRepositoryV2.class);

    private final PostgresOpensearchMapper postgresOpensearchMapper;

    FiltreringAvGjeldende14aVedtakTest() {
        this.postgresOpensearchMapper = new PostgresOpensearchMapper(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                siste14aVedtakRepository,
                null,
                oppfolgingRepositoryV2
        );
    }

    @Test
    public void skal_flette_inn_gjeldende_14a_vedtak_nar_siste_14a_vedtak_er_fattet_for_lansering_av_veilarboppfolging() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbruker = new OppfolgingsBruker()
                .setFnr(ident1.getFnr().get())
                .setAktoer_id(ident1.getAktorId().get());

        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2015-11-10T19:37:25+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        ZonedDateTime startdatoForOppfolging = ZonedDateTime.parse("2017-12-02T19:37:25+02:00");

        Map<AktorId, Optional<ZonedDateTime>> aktorIdStartDatoForOppfolgingMap =
                Map.of(ident1.getAktorId(), Optional.of(startdatoForOppfolging));

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(aktorIdStartDatoForOppfolgingMap);

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(oppfolgingsbruker));

        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a()).isNotNull();
        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a().fattetDato()).isEqualTo(siste14AVedtakForBruker.fattetDato);
        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a().innsatsgruppe()).isEqualTo(siste14AVedtakForBruker.getInnsatsgruppe());
        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a().hovedmal()).isEqualTo(siste14AVedtakForBruker.getHovedmal());
    }

    @Test
    public void skal_flette_inn_gjeldende_14a_vedtak_nar_siste_14a_vedtak_er_fattet_etter_lansering_av_veilarboppfolging_og_i_inneværende_oppfølgingsperiode() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbruker = new OppfolgingsBruker()
                .setFnr(ident1.getFnr().get())
                .setAktoer_id(ident1.getAktorId().get());

        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2018-11-10T19:37:25+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        ZonedDateTime startdatoForOppfolging = ZonedDateTime.parse("2017-12-02T19:37:25+02:00");

        Map<AktorId, Optional<ZonedDateTime>> aktorIdStartDatoForOppfolgingMap =
                Map.of(ident1.getAktorId(), Optional.of(startdatoForOppfolging));

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(aktorIdStartDatoForOppfolgingMap);

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(oppfolgingsbruker));

        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a()).isNotNull();
        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a().fattetDato()).isEqualTo(siste14AVedtakForBruker.fattetDato);
        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a().innsatsgruppe()).isEqualTo(siste14AVedtakForBruker.getInnsatsgruppe());
        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a().hovedmal()).isEqualTo(siste14AVedtakForBruker.getHovedmal());
    }

    @Test
    public void skal_ikke_flette_inn_gjeldende_14a_vedtak_når_siste_14a_vedtak_er_fattet_etter_lansering_av_veilarboppfolging_men_utenfor_inneværende_oppfølgingsperiode() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbruker = new OppfolgingsBruker()
                .setFnr(ident1.getFnr().get())
                .setAktoer_id(ident1.getAktorId().get());

        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2018-11-10T19:37:25+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        ZonedDateTime startdatoForOppfolging = ZonedDateTime.parse("2019-12-02T19:37:25+02:00");

        Map<AktorId, Optional<ZonedDateTime>> aktorIdStartDatoForOppfolgingMap =
                Map.of(ident1.getAktorId(), Optional.of(startdatoForOppfolging));

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(aktorIdStartDatoForOppfolgingMap);

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(oppfolgingsbruker));

        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a()).isNull();
    }

    @Test
    public void skal_ikke_flette_inn_gjeldende_14a_vedtak_når_siste_14a_vedtak_når_bruker_ikke_er_under_oppfølging() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbruker = new OppfolgingsBruker()
                .setFnr(ident1.getFnr().get())
                .setAktoer_id(ident1.getAktorId().get());

        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2018-11-10T19:37:25+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(Collections.emptyMap());

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(oppfolgingsbruker));

        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a()).isNull();
    }

    private GjeldendeIdenter genererGjeldendeIdent() {
        return GjeldendeIdenter.builder().fnr(randomFnr()).aktorId(randomAktorId()).build();
    }
}
