package no.nav.pto.veilarbportefolje.siste14aVedtak;


import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.ArenaHovedmal;
import no.nav.pto.veilarbportefolje.domene.ArenaInnsatsgruppe;
import no.nav.pto.veilarbportefolje.domene.ArenaServicegruppe;
import no.nav.pto.veilarbportefolje.domene.GjeldendeIdenter;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Avvik14aVedtakServiceTest {

    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3 = mock(OppfolgingsbrukerRepositoryV3.class);
    private final Siste14aVedtakRepository siste14aVedtakRepository = mock(Siste14aVedtakRepository.class);
    private final Avvik14aVedtakService avvik14aVedtakService =
            new Avvik14aVedtakService(oppfolgingsbrukerRepositoryV3, siste14aVedtakRepository);


    @Test
    public void skalIkkeFinneAvvikForBrukereMedTilsvarendeDataIArenaOgVedtaksstotte() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(ArenaInnsatsgruppe.IKVAL.name())
                .hovedmaalkode(ArenaHovedmal.BEHOLDEA.name())
                .build();
        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .build();

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(anySet())).thenReturn(Map.of(siste14AVedtakForBruker.aktorId, siste14AVedtakForBruker));

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
        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .build();

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(anySet())).thenReturn(Map.of(siste14AVedtakForBruker.aktorId, siste14AVedtakForBruker));

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
        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .build();

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(anySet())).thenReturn(Map.of(siste14AVedtakForBruker.aktorId, siste14AVedtakForBruker));

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
        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .build();

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(anySet())).thenReturn(Map.of(siste14AVedtakForBruker.aktorId, siste14AVedtakForBruker));

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
        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.SITUASJONSBESTEMT_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .build();

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(anySet())).thenReturn(Map.of(siste14AVedtakForBruker.aktorId, siste14AVedtakForBruker));

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
        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .build();

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(anySet())).thenReturn(Map.of(siste14AVedtakForBruker.aktorId, siste14AVedtakForBruker));

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
        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .innsatsgruppe(Innsatsgruppe.SITUASJONSBESTEMT_INNSATS)
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .build();

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(anySet())).thenReturn(Map.of(siste14AVedtakForBruker.aktorId, siste14AVedtakForBruker));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK);
    }

    @Test
    public void skalFinneAvvikDersomInnsatsgruppeManglerISiste14aVedtak() {
        GjeldendeIdenter ident1 = genererGjeldendeIdent();
        Set<GjeldendeIdenter> identer = Set.of(ident1);
        OppfolgingsbrukerEntity oppfolgingsbruker = OppfolgingsbrukerEntity.builder()
                .fodselsnr(ident1.getFnr().get())
                .kvalifiseringsgruppekode(ArenaInnsatsgruppe.IKVAL.name())
                .hovedmaalkode(ArenaHovedmal.BEHOLDEA.name())
                .build();
        Siste14aVedtakForBruker siste14AVedtakForBruker = Siste14aVedtakForBruker.builder()
                .aktorId(ident1.getAktorId())
                .hovedmal(Hovedmal.BEHOLDE_ARBEID)
                .build();

        when(oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(anySet())).thenReturn(Map.of(Fnr.of(oppfolgingsbruker.fodselsnr()), oppfolgingsbruker));
        when(siste14aVedtakRepository.hentSiste14aVedtakForBrukere(anySet())).thenReturn(Map.of(siste14AVedtakForBruker.aktorId, siste14AVedtakForBruker));

        Map<GjeldendeIdenter, Avvik14aVedtak> avvik = avvik14aVedtakService.hentAvvik(identer);
        assertThat(avvik.values()).containsOnly(Avvik14aVedtak.INNSATSGRUPPE_MANGLER_I_NY_KILDE);
    }

    private GjeldendeIdenter genererGjeldendeIdent() {
        return GjeldendeIdenter.builder().fnr(randomFnr()).aktorId(randomAktorId()).build();
    }
}
