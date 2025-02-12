package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.GjeldendeIdenter;
import no.nav.pto.veilarbportefolje.gjeldende14aVedtak.Gjeldende14aVedtakService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.postgres.PostgresOpensearchMapper;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.gjeldende14aVedtak.Gjeldende14aVedtakService.LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FlettingAvGjeldende14aVedtakTest {
    private final PostgresOpensearchMapper postgresOpensearchMapper;
    private final Siste14aVedtakRepository siste14aVedtakRepository = mock(Siste14aVedtakRepository.class);
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2 = mock(OppfolgingRepositoryV2.class);

    FlettingAvGjeldende14aVedtakTest() {
        final Gjeldende14aVedtakService gjeldende14aVedtakService = new Gjeldende14aVedtakService(siste14aVedtakRepository, oppfolgingRepositoryV2);

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
                null,
                gjeldende14aVedtakService
        );
    }

    @BeforeEach
    public void resetMocks() {
        reset(siste14aVedtakRepository);
        reset(oppfolgingRepositoryV2);
    }

    @Test
    public void skal_flette_inn_gjeldende_14a_vedtak_nar_siste_14a_vedtak_er_fattet_før_lansering_av_veilarboppfolging() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbruker = new OppfolgingsBruker()
                .setFnr(ident1.getFnr().get())
                .setAktoer_id(ident1.getAktorId().get());

        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2015-11-10T19:37:25+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        ZonedDateTime startdatoForOppfolging = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE;

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
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2020-11-10T10:00:00+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        ZonedDateTime startdatoForOppfolging = ZonedDateTime.parse("2019-05-11T10:00:00+02:00");

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
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2018-11-10T10:00:00+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        ZonedDateTime startdatoForOppfolging = ZonedDateTime.parse("2019-03-02T10:00:00+02:00");

        Map<AktorId, Optional<ZonedDateTime>> aktorIdStartDatoForOppfolgingMap =
                Map.of(ident1.getAktorId(), Optional.of(startdatoForOppfolging));

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(aktorIdStartDatoForOppfolgingMap);

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(oppfolgingsbruker));

        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a()).isNull();
    }

    @Test
    public void skal_ikke_flette_inn_gjeldende_14a_vedtak_når_bruker_har_et_siste_14a_vedtak_men_ikke_er_under_oppfølging() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbruker = new OppfolgingsBruker()
                .setFnr(ident1.getFnr().get())
                .setAktoer_id(ident1.getAktorId().get());

        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2018-11-10T10:00:00+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(Collections.emptyMap());

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(oppfolgingsbruker));

        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a()).isNull();
    }

    @Test
    public void skal_ikke_flette_inn_gjeldende_14a_vedtak_når_siste_14a_vedtak_er_fattet_før_lansering_av_veilarboppfolging_men_oppfølgingsperiode_startdato_er_etter_lansering_av_veilarboppfolging() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbruker = new OppfolgingsBruker()
                .setFnr(ident1.getFnr().get())
                .setAktoer_id(ident1.getAktorId().get());

        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2015-01-20T10:00:00+02:00"))
                .build();

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(ident1.getAktorId(), siste14AVedtakForBruker);

        ZonedDateTime startdatoForOppfolging = ZonedDateTime.parse("2020-12-02T10:00:00+02:00");

        Map<AktorId, Optional<ZonedDateTime>> aktorIdStartDatoForOppfolgingMap =
                Map.of(ident1.getAktorId(), Optional.of(startdatoForOppfolging));

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(aktorIdStartDatoForOppfolgingMap);

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(oppfolgingsbruker));

        assertThat(oppfolgingsbruker.getGjeldendeVedtak14a()).isNull();
    }

    @Test
    public void skal_ikke_flette_inn_gjeldende_14a_vedtak_for_flere_brukere_med_ulike_utgangspunkt() {
        GjeldendeIdenter oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_gjeldendeIdenter = genererGjeldendeIdent();
        GjeldendeIdenter oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_gjeldendeIdenter = genererGjeldendeIdent();
        GjeldendeIdenter oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode_gjeldendeIdenter = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode = new OppfolgingsBruker()
                .setFnr(oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_gjeldendeIdenter.getFnr().get())
                .setAktoer_id(oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_gjeldendeIdenter.getAktorId().get());
        OppfolgingsBruker oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode = new OppfolgingsBruker()
                .setFnr(oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_gjeldendeIdenter.getFnr().get())
                .setAktoer_id(oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_gjeldendeIdenter.getAktorId().get());
        OppfolgingsBruker oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode = new OppfolgingsBruker()
                .setFnr(oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode_gjeldendeIdenter.getFnr().get())
                .setAktoer_id(oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode_gjeldendeIdenter.getAktorId().get());

        Siste14aVedtakForBruker oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_siste14AVedtak = Siste14aVedtakForBruker.builder()
                .aktorId(oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_gjeldendeIdenter.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2015-01-20T10:00:00+02:00"))
                .build();
        Siste14aVedtakForBruker oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_siste14AVedtak = Siste14aVedtakForBruker.builder()
                .aktorId(oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_gjeldendeIdenter.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .fattetDato(ZonedDateTime.parse("2020-01-20T10:00:00+02:00"))
                .build();


        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Map.of(
                oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_gjeldendeIdenter.getAktorId(),
                oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_siste14AVedtak,
                oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_gjeldendeIdenter.getAktorId(),
                oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_siste14AVedtak
        );

        ZonedDateTime oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_startDatoOppfølging = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE;
        ZonedDateTime oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_startDatoOppfølging = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE;
        ZonedDateTime oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode_startDatoOppfølging = ZonedDateTime.parse("2020-12-02T10:00:00+02:00");

        Map<AktorId, Optional<ZonedDateTime>> aktorIdStartDatoForOppfolgingMap =
                Map.of(
                        oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_gjeldendeIdenter.getAktorId(),
                        Optional.of(oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode_startDatoOppfølging),
                        oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_gjeldendeIdenter.getAktorId(),
                        Optional.of(oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode_startDatoOppfølging),
                        oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode_gjeldendeIdenter.getAktorId(),
                        Optional.of(oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode_startDatoOppfølging)
                );

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(aktorIdStartDatoForOppfolgingMap);

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(
                oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode,
                oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode,
                oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode
        ));

        assertThat(oppfolgingsbrukerMedVedtakFraFørLanseringAvOppfolgingsperiode.getGjeldendeVedtak14a()).isNotNull();
        assertThat(oppfolgingsbrukerMedVedtakFraEtterLanseringAvOppfolgingsperiode.getGjeldendeVedtak14a()).isNotNull();
        assertThat(oppfolgingsbrukerUtenVedtakIInneværendeOppfølgingsperiode.getGjeldendeVedtak14a()).isNull();
    }

    @Test
    public void skal_ikke_flette_inn_gjeldende_14a_vedtak_når_personen_ikke_har_noen_14a_vedtak() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        GjeldendeIdenter ident2 = genererGjeldendeIdent();

        OppfolgingsBruker oppfolgingsbruker1 = new OppfolgingsBruker()
                .setFnr(ident1.getFnr().get())
                .setAktoer_id(ident1.getAktorId().get());
        OppfolgingsBruker oppfolgingsbruker2 = new OppfolgingsBruker()
                .setFnr(ident2.getFnr().get())
                .setAktoer_id(ident2.getAktorId().get());

        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = Collections.emptyMap();

        ZonedDateTime startdatoForOppfolging1 = ZonedDateTime.parse("2018-12-02T19:37:25+02:00");
        ZonedDateTime startdatoForOppfolging2 = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE;

        Map<AktorId, Optional<ZonedDateTime>> aktorIdStartDatoForOppfolgingMap =
                Map.of(
                        ident1.getAktorId(), Optional.of(startdatoForOppfolging1),
                        ident2.getAktorId(), Optional.of(startdatoForOppfolging2)
                );

        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(any())).thenReturn(aktorIdSiste14aVedtakMap);
        when(oppfolgingRepositoryV2.hentStartDatoForOppfolging(any())).thenReturn(aktorIdStartDatoForOppfolgingMap);

        postgresOpensearchMapper.flettInnGjeldende14aVedtak(List.of(oppfolgingsbruker1, oppfolgingsbruker2));

        assertThat(oppfolgingsbruker1.getGjeldendeVedtak14a()).isNull();
        assertThat(oppfolgingsbruker2.getGjeldendeVedtak14a()).isNull();
    }

    private GjeldendeIdenter genererGjeldendeIdent() {
        return GjeldendeIdenter.builder().fnr(randomFnr()).aktorId(randomAktorId()).build();
    }
}
