package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
import no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto.AvsluttetOppfolgingsperiodeV2;
import no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto.GjeldendeOppfolgingsperiodeV2Dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.genererAvsluttetOppfolgingsperiode;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.genererStartetOppfolgingsperiode;

public class OppfolgingPeriodeServiceTest {
    private OppfolgingStartetService oppfolgingStartetService;
    private OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private OppfolgingPeriodeService oppfolgingPeriodeService;

    @BeforeEach
    public void setUp() {
        oppfolgingStartetService = Mockito.mock(OppfolgingStartetService.class);
        oppfolgingAvsluttetService = Mockito.mock(OppfolgingAvsluttetService.class);
        oppfolgingPeriodeService = new OppfolgingPeriodeService(oppfolgingStartetService, oppfolgingAvsluttetService);
    }

    @Test
    public void testOppfolgingStart() {
        String aktorId = "111111";
        ZonedDateTime startOppfolgingDate = ZonedDateTime.now();
        GjeldendeOppfolgingsperiodeV2Dto sisteOppfolgingsperiode = genererStartetOppfolgingsperiode(AktorId.of(aktorId), startOppfolgingDate);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(sisteOppfolgingsperiode);

        Mockito.verify(oppfolgingStartetService, Mockito.times(1)).behandleOppfølgingStartetEllerKontorEndret(Fnr.of(sisteOppfolgingsperiode.getIdent()), AktorId.of(aktorId), startOppfolgingDate, new NavKontor(sisteOppfolgingsperiode.getKontorId()));
        Mockito.verify(oppfolgingStartetService, Mockito.times(0)).startOppfolging(AktorId.of(aktorId), startOppfolgingDate, new NavKontor(sisteOppfolgingsperiode.getKontorId()));
        Mockito.verify(oppfolgingAvsluttetService, Mockito.times(0)).avsluttOppfolging(AktorId.of(aktorId));
    }

    @Test
    public void testOppfolgingAvslutt() {
        String aktorId = "111111";

        AvsluttetOppfolgingsperiodeV2 sisteOppfolgingsperiode = genererAvsluttetOppfolgingsperiode(AktorId.of(aktorId));
        ZonedDateTime startOppfolgingDate = sisteOppfolgingsperiode.getStartTidspunkt();
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(sisteOppfolgingsperiode);

        Mockito.verify(oppfolgingStartetService, Mockito.times(0)).startOppfolging(AktorId.of(aktorId), startOppfolgingDate, null);
        Mockito.verify(oppfolgingAvsluttetService, Mockito.times(1)).avsluttOppfolging(AktorId.of(aktorId));
    }
}