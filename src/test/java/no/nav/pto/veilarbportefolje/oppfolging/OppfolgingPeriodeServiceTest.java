package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.UUID;

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
        SisteOppfolgingsperiodeV1 sisteOppfolgingsperiode = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktorId, startOppfolgingDate, null);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(sisteOppfolgingsperiode);

        Mockito.verify(oppfolgingStartetService, Mockito.times(1)).startOppfolging(AktorId.of(aktorId), startOppfolgingDate);
        Mockito.verify(oppfolgingAvsluttetService, Mockito.times(0)).avsluttOppfolging(AktorId.of(aktorId));
    }

    @Test
    public void testOppfolgingAvslutt() {
        String aktorId = "111111";
        ZonedDateTime startOppfolgingDate = ZonedDateTime.now().minusDays(2);
        ZonedDateTime sluttOppfolgingDate = ZonedDateTime.now();
        SisteOppfolgingsperiodeV1 sisteOppfolgingsperiode = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktorId, startOppfolgingDate, sluttOppfolgingDate);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(sisteOppfolgingsperiode);

        Mockito.verify(oppfolgingStartetService, Mockito.times(0)).startOppfolging(AktorId.of(aktorId), startOppfolgingDate);
        Mockito.verify(oppfolgingAvsluttetService, Mockito.times(1)).avsluttOppfolging(AktorId.of(aktorId));
    }
}