package no.nav.pto.veilarbportefolje.ensligforsorger.mapping;

import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Aktivitetstype;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.ensligforsorger.domain.Aktivitetstype.*;
import static no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype.*;

public class AktivitetsTypeTilAktivitetspliktTest {

    @Test
    public void periodeFørFødsel_skalReturnereFalse() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(PERIODE_FØR_FØDSEL, BARN_UNDER_ETT_ÅR);
        Assert.assertEquals(Optional.of(false), result);
    }

    @Test
    public void nyPeriodeForNyttBarn_medBarnUnderEttÅr_skalReturnereFalse() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(NY_PERIODE_FOR_NYTT_BARN, BARN_UNDER_ETT_ÅR);
        Assert.assertEquals(Optional.of(false), result);
    }

    @Test
    public void hovedperiode_medBarnUnderEttÅr_skalReturnereFalse() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(HOVEDPERIODE, BARN_UNDER_ETT_ÅR);
        Assert.assertEquals(Optional.of(false), result);
    }

    @Test
    public void nyPeriodeForNyttBarn_medAktiveAktivitetstyper_skalReturnereTrue() {
        List.of(FORSØRGER_I_ARBEID, FORSØRGER_REELL_ARBEIDSSØKER, FORSØRGER_I_UTDANNING, FORSØRGER_ETABLERER_VIRKSOMHET)
                .forEach(aktivitetstype -> {
                    Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(NY_PERIODE_FOR_NYTT_BARN, aktivitetstype);
                    Assert.assertEquals("Forventet true for " + aktivitetstype, Optional.of(true), result);
                });
    }

    @Test
    public void hovedperiode_medAktiveAktivitetstyper_skalReturnereTrue() {
        List.of(FORSØRGER_I_ARBEID, FORSØRGER_REELL_ARBEIDSSØKER, FORSØRGER_I_UTDANNING, FORSØRGER_ETABLERER_VIRKSOMHET)
                .forEach(aktivitetstype -> {
                    Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(HOVEDPERIODE, aktivitetstype);
                    Assert.assertEquals("Forventet true for " + aktivitetstype, Optional.of(true), result);
                });
    }

    @Test
    public void nyPeriodeForNyttBarn_medUnntaksAktivitetstyper_skalReturnereFalse() {
        List.of(BARNET_SÆRLIG_TILSYNSKREVENDE, FORSØRGER_MANGLER_TILSYNSORDNING, FORSØRGER_ER_SYK, BARNET_ER_SYKT)
                .forEach(aktivitetstype -> {
                    Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(NY_PERIODE_FOR_NYTT_BARN, aktivitetstype);
                    Assert.assertEquals("Forventet false for " + aktivitetstype, Optional.of(false), result);
                });
    }

    @Test
    public void utvidelse_medUtvidelseForsørgerIUtdanning_skalReturnereTrue() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(UTVIDELSE, UTVIDELSE_FORSØRGER_I_UTDANNING);
        Assert.assertEquals(Optional.of(true), result);
    }

    @Test
    public void utvidelse_medUtvidelseBarnetSærligTilsynskrevende_skalReturnereFalse() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(UTVIDELSE, UTVIDELSE_BARNET_SÆRLIG_TILSYNSKREVENDE);
        Assert.assertEquals(Optional.of(false), result);
    }

    @Test
    public void forlengelse_medForlengelseStønadUtSkoleåret_skalReturnereTrue() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(FORLENGELSE, FORLENGELSE_STØNAD_UT_SKOLEÅRET);
        Assert.assertEquals(Optional.of(true), result);
    }

    @Test
    public void forlengelse_medAndreForlengelsestyper_skalReturnereFalse() {
        List.of(
                FORLENGELSE_MIDLERTIDIG_SYKDOM,
                FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
                FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER,
                FORLENGELSE_STØNAD_PÅVENTE_OPPSTART_KVALIFISERINGSPROGRAM,
                FORLENGELSE_STØNAD_PÅVENTE_TILSYNSORDNING,
                FORLENGELSE_STØNAD_PÅVENTE_UTDANNING
        ).forEach(aktivitetstype -> {
            Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(FORLENGELSE, aktivitetstype);
            Assert.assertEquals("Forventet false for " + aktivitetstype, Optional.of(false), result);
        });
    }

    @Test
    public void sanksjon_skalReturnereFalse() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(SANKSJON, BARN_UNDER_ETT_ÅR);
        Assert.assertEquals(Optional.of(false), result);
    }

    @Test
    public void ikkeAktivitetsplikt_skalReturnereFalseUavhengigAvPeriodetype() {
        List.of(HOVEDPERIODE, NY_PERIODE_FOR_NYTT_BARN, FORLENGELSE, UTVIDELSE, SANKSJON, SÆRLIG_TILSYNSKREVENDE_BARN)
                .forEach(periodetype -> {
                    Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(periodetype, IKKE_AKTIVITETSPLIKT);
                    Assert.assertEquals("Forventet false for periodetype " + periodetype, Optional.of(false), result);
                });
    }

    @Test
    public void migreringPeriodetype_skalReturnereEmpty() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(Periodetype.MIGRERING, BARN_UNDER_ETT_ÅR);
        Assert.assertEquals(Optional.empty(), result);
    }

    @Test
    public void migreringAktivitetstype_skalReturnereEmpty() {
        Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(HOVEDPERIODE, Aktivitetstype.MIGRERING);
        Assert.assertEquals(Optional.empty(), result);
    }

    @Test
    public void ukjentKombinasjon_skalReturnereEmpty() {
        List.of(SÆRLIG_TILSYNSKREVENDE_BARN, BARN_UNDER_14_MÅNEDER, FORBIGÅENDE_SYKDOM_HOS_BARNET)
                .forEach(periodetype -> {
                    Optional<Boolean> result = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(periodetype, BARN_UNDER_ETT_ÅR);
                    Assert.assertEquals("Forventet empty for ukjent periodetype " + periodetype, Optional.empty(), result);
                });
    }
}
