package no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak;


import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.ArenaHovedmal;
import no.nav.pto.veilarbportefolje.domene.ArenaInnsatsgruppe;
import no.nav.pto.veilarbportefolje.domene.ArenaServicegruppe;
import no.nav.pto.veilarbportefolje.domene.GjeldendeIdenter;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.Gjeldende14aVedtak;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.Gjeldende14aVedtakService;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

public class Avvik14aVedtakServiceTest {

    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3 = mock(OppfolgingsbrukerRepositoryV3.class);
    private Gjeldende14aVedtakService gjeldende14aVedtakService = mock(Gjeldende14aVedtakService.class);
    private final Avvik14aVedtakService avvik14aVedtakService = new Avvik14aVedtakService(oppfolgingsbrukerRepositoryV3, gjeldende14aVedtakService);

    @BeforeEach
    public void resetMocks() {
        reset(oppfolgingsbrukerRepositoryV3);
        reset(gjeldende14aVedtakService);
    }

    @Test
    public void skalIkkeFinneAvvikForBrukereMedTilsvarendeDataIArenaOgVedtaksstotte() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(ArenaInnsatsgruppe.IKVAL.name())
                .hovedmaalkode(ArenaHovedmal.BEHOLDEA.name())
                .build();
        Gjeldende14aVedtak gjeldende14AVedtakForBruker = new Gjeldende14aVedtak(
                ident1.getAktorId(),
                Innsatsgruppe.STANDARD_INNSATS,
                Hovedmal.BEHOLDE_ARBEID,
                randomZonedDate()
        );

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(gjeldende14aVedtakService.hentGjeldende14aVedtak(anySet())).thenReturn(Map.of(gjeldende14AVedtakForBruker.getAktorId(), Optional.of(gjeldende14AVedtakForBruker)));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.keySet()).containsAll(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.INGEN_AVVIK);
    }

    @Test
    public void skalIkkeFinneAvvikNaarArenaInnsatsgruppeErNull() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(null)
                .hovedmaalkode(ArenaHovedmal.BEHOLDEA.name())
                .build();
        Gjeldende14aVedtak gjeldende14aVedtak = new Gjeldende14aVedtak(
                ident1.getAktorId(),
                Innsatsgruppe.STANDARD_INNSATS,
                Hovedmal.BEHOLDE_ARBEID,
                randomZonedDate()
        );

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(gjeldende14aVedtakService.hentGjeldende14aVedtak(anySet())).thenReturn(Map.of(gjeldende14aVedtak.getAktorId(), Optional.of(gjeldende14aVedtak)));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.keySet()).containsAll(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.INGEN_AVVIK);
    }

    @Test
    public void skalIkkeFinneAvvikNaarArenaKvalifiseringsgruppeErServiceGruppe() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(ArenaServicegruppe.VURDU.name())
                .hovedmaalkode(ArenaHovedmal.BEHOLDEA.name())
                .build();
        Gjeldende14aVedtak gjeldende14AVedtakForBruker = new Gjeldende14aVedtak(
                ident1.getAktorId(),
                Innsatsgruppe.STANDARD_INNSATS,
                Hovedmal.BEHOLDE_ARBEID,
                randomZonedDate()
        );

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(gjeldende14aVedtakService.hentGjeldende14aVedtak(anySet())).thenReturn(Map.of(gjeldende14AVedtakForBruker.getAktorId(), Optional.of(gjeldende14AVedtakForBruker)));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.keySet()).containsAll(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.INGEN_AVVIK);
    }

    @Test
    public void skalIkkeFinneAvvikNaarInnsatsgruppeErLikOgArenaHovedmaalErNull() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(ArenaInnsatsgruppe.IKVAL.name())
                .hovedmaalkode(null)
                .build();
        Gjeldende14aVedtak gjeldende14AVedtakForBruker = new Gjeldende14aVedtak(
                ident1.getAktorId(),
                Innsatsgruppe.STANDARD_INNSATS,
                Hovedmal.BEHOLDE_ARBEID,
                randomZonedDate()
        );

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(gjeldende14aVedtakService.hentGjeldende14aVedtak(anySet())).thenReturn(Map.of(gjeldende14AVedtakForBruker.getAktorId(), Optional.of(gjeldende14AVedtakForBruker)));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.keySet()).containsAll(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.INGEN_AVVIK);
    }

    @Test
    public void skalFinneAvvikDersomInnsatsgruppeErUlik() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(ArenaInnsatsgruppe.IKVAL.name())
                .hovedmaalkode(ArenaHovedmal.BEHOLDEA.name())
                .build();
        Gjeldende14aVedtak gjeldende14AVedtakForBruker = new Gjeldende14aVedtak(
                ident1.getAktorId(),
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Hovedmal.BEHOLDE_ARBEID,
                randomZonedDate()
        );

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(gjeldende14aVedtakService.hentGjeldende14aVedtak(anySet())).thenReturn(Map.of(gjeldende14AVedtakForBruker.getAktorId(), Optional.of(gjeldende14AVedtakForBruker)));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.INNSATSGRUPPE_ULIK);
    }

    @Test
    public void skalFinneAvvikDersomHovedmaalErUlik() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(ArenaInnsatsgruppe.IKVAL.name())
                .hovedmaalkode(ArenaHovedmal.OKEDELT.name())
                .build();
        Gjeldende14aVedtak gjeldende14AVedtakForBruker = new Gjeldende14aVedtak(
                ident1.getAktorId(),
                Innsatsgruppe.STANDARD_INNSATS,
                Hovedmal.BEHOLDE_ARBEID,
                randomZonedDate()
        );

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(gjeldende14aVedtakService.hentGjeldende14aVedtak(anySet())).thenReturn(Map.of(gjeldende14AVedtakForBruker.getAktorId(), Optional.of(gjeldende14AVedtakForBruker)));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.HOVEDMAAL_ULIK);
    }

    @Test
    public void skalFinneAvvikDersomInnsatsgruppeOgHovedmaalErUlik() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(ArenaInnsatsgruppe.IKVAL.name())
                .hovedmaalkode(ArenaHovedmal.OKEDELT.name())
                .build();
        Gjeldende14aVedtak gjeldende14AVedtakForBruker = new Gjeldende14aVedtak(
                ident1.getAktorId(),
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Hovedmal.BEHOLDE_ARBEID,
                randomZonedDate()
        );

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(gjeldende14aVedtakService.hentGjeldende14aVedtak(anySet())).thenReturn(Map.of(gjeldende14AVedtakForBruker.getAktorId(), Optional.of(gjeldende14AVedtakForBruker)));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK);
    }

    private GjeldendeIdenter genererGjeldendeIdent() {
        return GjeldendeIdenter.builder().fnr(randomFnr()).aktorId(randomAktorId()).build();
    }
}
